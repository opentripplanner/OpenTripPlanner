/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import static org.opentripplanner.common.IterableLibrary.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.GraphRefreshListener;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.opentripplanner.util.JoinedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods
 * used during network linking and trip planning.
 * 
 * Creates a StreetLocation representing a location on a street that's not at an intersection, based
 * on input latitude and longitude. Instantiating this class is expensive, because it creates a
 * spatial index of all of the intersections in the graph.
 */
//@Component
public class StreetVertexIndexServiceImpl implements StreetVertexIndexService {

    private Graph graph;

    /**
     * Contains only instances of {@link StreetEdge}
     */
    private SpatialIndex edgeTree;

    private STRtree transitStopTree;

    private STRtree intersectionTree;

//    private static final double SEARCH_RADIUS_M = 100; // meters
//    private static final double SEARCH_RADIUS_DEG = DistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    /* all distance constants here are plate-carée Euclidean, 0.001 ~= 100m at equator */
    
    // edges will only be found if they are closer than this distance 
    public static final double MAX_DISTANCE_FROM_STREET = 0.01000;

    // maximum difference in distance for two geometries to be considered coincident
    public static final double DISTANCE_ERROR = 0.000001;

    //if a point is within MAX_CORNER_DISTANCE, it is treated as at the corner
    private static final double MAX_CORNER_DISTANCE = 0.0001;

    private static final double DIRECTION_ERROR = 0.05;

    private static final Logger _log = LoggerFactory.getLogger(StreetVertexIndexServiceImpl.class);

    public StreetVertexIndexServiceImpl(Graph graph) {
        this.graph = graph;
        setup();
    }

    public void setup_modifiable() {
        edgeTree = new Quadtree();
        postSetup();
    }

    public void setup() {
        edgeTree = new STRtree();
        postSetup();
        ((STRtree) edgeTree).build();
    }

    private void postSetup() {

        transitStopTree = new STRtree();
        intersectionTree = new STRtree();

        for (Vertex gv : graph.getVertices()) {
            Vertex v = gv;
            // We only care about StreetEdges
            for (StreetEdge e : filter(gv.getOutgoing(), StreetEdge.class)) {
                if (e.getGeometry() == null) {
                    continue;
                }
                Envelope env = e.getGeometry().getEnvelopeInternal();
                edgeTree.insert(env, e);
            }
            if (v instanceof TransitStop) {
                // only index transit stops that (a) are entrances, or (b) have no associated
                // entrances
                TransitStop ts = (TransitStop) v;
                if (!ts.isEntrance() && ts.hasEntrances()) {
                    continue;
                }
                Envelope env = new Envelope(v.getCoordinate());
                transitStopTree.insert(env, v);
            }
            if (v instanceof TurnVertex || v instanceof IntersectionVertex) {
                Envelope env = new Envelope(v.getCoordinate());
                intersectionTree.insert(env, v);
            }
        }
        transitStopTree.build();
    }

    /**
     * Get all transit stops within a given distance of a coordinate
     * 
     * @param distance in meters
     */
    @SuppressWarnings("unchecked")
    public List<Vertex> getLocalTransitStops(Coordinate c, double distance) {
        Envelope env = new Envelope(c);
        env.expandBy(DistanceLibrary.metersToDegrees(distance));
        List<Vertex> nearby = transitStopTree.query(env);
        List<Vertex> results = new ArrayList<Vertex>();
        for (Vertex v : nearby) {
            if (v.distance(c) <= distance) {
                results.add(v);
            }
        }
        return results;
    }

    /**
     * Gets the closest vertex to a coordinate. If necessary, this vertex will be created by
     * splitting nearby edges (non-permanently).
     */
    public Vertex getClosestVertex(final Coordinate coordinate, String name, RoutingRequest options) {
        return getClosestVertex(coordinate, name, options, null);
    }

    public Vertex getClosestVertex(final Coordinate coordinate, String name, RoutingRequest options, List<Edge> extraEdges) {
        _log.debug("Looking for/making a vertex near {}", coordinate);

        // first, check for intersections very close by
        List<StreetVertex> vertices = getIntersectionAt(coordinate, MAX_CORNER_DISTANCE);
        if (vertices != null && !vertices.isEmpty()) {
            // coordinate is at a street corner or endpoint
            if (name == null) {
                // generate names for corners when no name was given
                // TODO: internationalize
                Set<String> uniqueNameSet = new HashSet<String>();
                // filter to avoid using OSM node ids for dead ends 
                for (StreetVertex v : IterableLibrary.filter(vertices, StreetVertex.class))
                    uniqueNameSet.add(v.getName());
                List<String> uniqueNames = new ArrayList<String>(uniqueNameSet);
                if (uniqueNames.size() > 1)
                    name = String.format("corner of %s and %s", uniqueNames.get(0), uniqueNames.get(1));
                else if (uniqueNames.size() == 1)
                    name = uniqueNames.get(0);
                else 
                    name = "unnamed street";
            }
            StreetLocation closest = new StreetLocation("corner " + Math.random(), coordinate, name);
            for (Vertex v : vertices) {
                FreeEdge e = new FreeEdge(closest, v);
                closest.getExtra().add(e);
                e = new FreeEdge(v, closest);
                closest.getExtra().add(e);
                if (v instanceof TurnVertex && ((TurnVertex) v).isWheelchairAccessible()) {
                    closest.setWheelchairAccessible(true);
                }
            }
            return closest;
        }

        // if no intersection vertices were found, then find the closest transit stop
        // (we can return stops here because this method is not used when street-transit linking)
        double closest_stop_distance = Double.POSITIVE_INFINITY;
        Vertex closest_stop = null;
        // elsewhere options=null means no restrictions, find anything.
        // here we skip examining stops, as they are really only relevant when transit is being used
        if (options != null && options.getModes().isTransit()) {
            for (Vertex v : getLocalTransitStops(coordinate, 1000)) {
                double d = v.distance(coordinate);
                if (d < closest_stop_distance) {
                    closest_stop_distance = d;
                    closest_stop = v;
                }
            }
        }
        _log.debug(" best stop: {} distance: {}", closest_stop, closest_stop_distance);

        // then find closest walkable street
        StreetLocation closest_street = null;
        CandidateEdgeBundle bundle = getClosestEdges(coordinate, options, extraEdges, null);
        CandidateEdge candidate = bundle.best;
        double closest_street_distance = Double.POSITIVE_INFINITY;
        if (candidate != null) {
            StreetEdge bestStreet = candidate.edge;
            Coordinate nearestPoint = candidate.nearestPointOnEdge;
            closest_street_distance = DistanceLibrary.distance(coordinate, nearestPoint);
            _log.debug("best street: {} dist: {}", bestStreet.toString(), closest_street_distance);
            if (name == null) {
                name = bestStreet.getName();
            }
            closest_street = StreetLocation.createStreetLocation(bestStreet.getName() + "_"
                    + coordinate.toString(), name, bundle.toEdgeList(), nearestPoint);
        }

        // decide whether to return stop, street, or street + stop
        if (closest_street == null) {
            // no street found, return closest stop or null
            _log.debug("returning only transit stop (no street found)");
            return closest_stop; // which will be null if none was found
        } else {
            // street found
            if (closest_stop != null) {
                // both street and stop found
                double relativeStopDistance = closest_stop_distance / closest_street_distance;
                if (relativeStopDistance < 0.1) {
                    _log.debug("returning only transit stop (stop much closer than street)");
                    return closest_stop;
                }
                if (relativeStopDistance < 1.5) {
                    _log.debug("linking transit stop to street (distances are comparable)");
                    closest_street.addExtraEdgeTo(closest_stop);
                }
            }
            _log.debug("returning split street");
            return closest_street;
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<Vertex> getVerticesForEnvelope(Envelope envelope) {
        return intersectionTree.query(envelope);
    }

    public static class CandidateEdge {
        private static final double PLATFORM_PREFERENCE = 2.0;
        private static final double SIDEWALK_PREFERENCE = 1.5;
        public final DistanceOp op;
        public final StreetEdge edge;
        public final Vertex endwiseVertex;
        private double score;
        public final Coordinate nearestPointOnEdge;
        public final double directionToEdge;
        public final double directionOfEdge;
        public final double directionDifference;
        public final double distance;
        public CandidateEdge(StreetEdge e, Point p, double preference) {
            edge = e;
            Geometry edgeGeom = edge.getGeometry();
            op = new DistanceOp(p, edgeGeom);
            distance = op.distance();
            // location on second geometry (edge)
            GeometryLocation edgeLocation = op.nearestLocations()[1];
            nearestPointOnEdge = edgeLocation.getCoordinate(); 
            Coordinate[] edgeCoords = edgeGeom.getCoordinates();
            if (nearestPointOnEdge.equals(edgeCoords[0]))
                endwiseVertex = edge.getFromVertex();
            else if (nearestPointOnEdge.equals(edgeCoords[edgeCoords.length - 1])) 
                endwiseVertex = edge.getToVertex();
            else
                endwiseVertex = null;
            score = distance;
            if (endwise()) {
                score *= 1.5;
            }
            score /= preference;
            if (e.getName().contains("platform")) {
                //this is kind of a hack, but there's not really a better way to do it
                score /= PLATFORM_PREFERENCE;
            }
            if (e.getName().contains("sidewalk") || !e.getPermission().allows(StreetTraversalPermission.CAR)) {
                //this is kind of a hack, but there's not really a better way to do it
                score /= SIDEWALK_PREFERENCE;
            }
            double xd = nearestPointOnEdge.x - p.getX();
            double yd = nearestPointOnEdge.y - p.getY();
            directionToEdge = Math.atan2(yd, xd);
            int edgeSegmentIndex = edgeLocation.getSegmentIndex();
            Coordinate c0 = edgeCoords[edgeSegmentIndex];
            Coordinate c1 = edgeCoords[edgeSegmentIndex + 1];
            xd = c1.x - c1.y;
            yd = c1.y - c0.y;
            directionOfEdge = Math.atan2(yd, xd);
            double absDiff = Math.abs(directionToEdge - directionOfEdge);
            directionDifference = Math.min(2*Math.PI - absDiff, absDiff);
            if (Double.isNaN(directionToEdge) ||
                Double.isNaN(directionOfEdge) ||
                Double.isNaN(directionDifference)) {
                _log.warn("direction to/of edge is NaN (0 length?): {}", edge);
            }
        }
        public boolean endwise() { return endwiseVertex != null; }
        public boolean parallel()  { return directionDifference < Math.PI / 2; } 
        public boolean perpendicular()  { return !parallel(); } 
        public double getScore() {
            return score;
        }
    }
    
    public static class CandidateEdgeBundle extends ArrayList<CandidateEdge> {
        private static final long serialVersionUID = 20120222L;
        public Vertex endwiseVertex = null;
        public CandidateEdge best = null;
        public boolean add(CandidateEdge ce) {
            if (ce.endwiseVertex != null)
                this.endwiseVertex = ce.endwiseVertex;
            if (best == null || ce.score < best.score)
                best = ce;
            return super.add(ce);
        }
        public List<StreetEdge> toEdgeList() {
            List<StreetEdge> ret = new ArrayList<StreetEdge>();
            for (CandidateEdge ce : this)
                ret.add(ce.edge);
            return ret;
        }
        public Collection<CandidateEdgeBundle> binByDistanceAndAngle() {
            Map<P2<Double>, CandidateEdgeBundle> bins = 
                new HashMap<P2<Double>, CandidateEdgeBundle>(); // (r, theta)
            CANDIDATE : for (CandidateEdge ce : this) {
                for (Entry<P2<Double>, CandidateEdgeBundle> bin : bins.entrySet()) {
                    double distance  = bin.getKey().getFirst();
                    double direction = bin.getKey().getSecond();
                    if (Math.abs(direction - ce.directionToEdge) < DIRECTION_ERROR &&
                        Math.abs(distance  - ce.distance) < DISTANCE_ERROR ) {
                        bin.getValue().add(ce);
                        continue CANDIDATE;
                    }
                }
                P2<Double> rTheta = new P2<Double>(ce.distance, ce.directionToEdge);
                CandidateEdgeBundle bundle = new CandidateEdgeBundle();
                bundle.add(ce);
                bins.put(rTheta, bundle);
            }
            return bins.values();
        }
        public boolean endwise() { return endwiseVertex != null; }
        public double getScore() {
            return best.score;
        }
    }

    @SuppressWarnings("unchecked")
    public CandidateEdgeBundle getClosestEdges(Coordinate coordinate, RoutingRequest options, List<Edge> extraEdges, Collection<Edge> routeEdges) {
        ArrayList<StreetEdge> extraStreets = new ArrayList<StreetEdge> ();
        if (extraEdges != null)
            for (StreetEdge se : IterableLibrary.filter(extraEdges, StreetEdge.class))
                extraStreets.add(se);

        Envelope envelope = new Envelope(coordinate);
        GeometryFactory factory = new GeometryFactory();
        Point p = factory.createPoint(coordinate);
        RoutingRequest walkingOptions = null;
        if (options != null) {
            walkingOptions = options.getWalkingOptions();
        }
        double envelopeGrowthAmount = 0.001; // ~= 100 meters
        double radius = 0;
        CandidateEdgeBundle candidateEdges = new CandidateEdgeBundle();
        while (candidateEdges.size() == 0) {
            // expand envelope -- assumes many close searches and occasional far ones
            envelope.expandBy(envelopeGrowthAmount);
            radius += envelopeGrowthAmount;
            if (radius > MAX_DISTANCE_FROM_STREET)
                return candidateEdges; // empty list
            // envelopeGrowthAmount *= 2;
            List<StreetEdge> nearbyEdges = edgeTree.query(envelope);
            // TODO remove use of ExtraEdges
            if (extraEdges != null && nearbyEdges != null) {
                nearbyEdges = new JoinedList<StreetEdge>(nearbyEdges, extraStreets);
            }
            for (StreetEdge e : nearbyEdges) {
                if (e == null || e instanceof OutEdge)
                    continue;
                if (options != null && 
                   (!(e.canTraverse(options) || e.canTraverse(walkingOptions))))
                    continue;
                double preferrence = 1;
                if (routeEdges != null && routeEdges.contains(e)) {
                    preferrence = 3.0;
                }
                CandidateEdge ce = new CandidateEdge(e, p, preferrence);
                // Even if an edge is outside the query envelope, bounding boxes can
                // still intersect. In this case, distance to the edge is greater
                // than the query envelope size.
                if (ce.distance < radius)
                    candidateEdges.add(ce);
            }
        }

        Collection<CandidateEdgeBundle> bundles = candidateEdges.binByDistanceAndAngle();
        // initially set best bundle to the closest bundle
        CandidateEdgeBundle best = null; 
        for (CandidateEdgeBundle bundle : bundles) {
            if (best == null || bundle.best.score < best.best.score)
                best = bundle;
        }

        return best;
    }

    public List<StreetVertex> getIntersectionAt(Coordinate coordinate) {
        return getIntersectionAt(coordinate, MAX_CORNER_DISTANCE);
    }

    @SuppressWarnings("unchecked")
    public List<StreetVertex> getIntersectionAt(Coordinate coordinate, double distanceError) {
        Envelope envelope = new Envelope(coordinate);
        envelope.expandBy(distanceError * 2);
        List<StreetVertex> nearby = intersectionTree.query(envelope);
        List<StreetVertex> atIntersection = new ArrayList<StreetVertex>(nearby.size());
        for (StreetVertex v : nearby) {
            if (coordinate.distance(v.getCoordinate()) < distanceError) {
                atIntersection.add(v);
            }
        }
        if (atIntersection.isEmpty()) {
            return null;
        }
        return atIntersection;
    }

    @Override
    /** radius is meters */
    public List<TransitStop> getNearbyTransitStops(Coordinate coordinate, double radius) {
        Envelope envelope = new Envelope(coordinate);
        envelope.expandBy(DistanceLibrary.metersToDegrees(radius));
        List<?> stops = transitStopTree.query(envelope);
        ArrayList<TransitStop> out = new ArrayList<TransitStop>();
        for (Object o : stops) {
            TransitStop stop = (TransitStop) o;
            if (stop.distance(coordinate) < radius) {
                out.add(stop);
            }
        }
        return out;
    }
    
    /* EX-GENERICPATHSERVICE */
    
    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    private static final Pattern _latLonPattern = Pattern.compile("^\\s*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\s*$");

    @Override
    public Vertex getVertexForPlace(NamedPlace place, RoutingRequest options) {
        return getVertexForPlace(place, options, null);
    }

    @Override
    public Vertex getVertexForPlace(NamedPlace place, RoutingRequest options, Vertex other) {
        Matcher matcher = _latLonPattern.matcher(place.place);
        if (matcher.matches()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(4));
            Coordinate location = new Coordinate(lon, lat);
            if (other instanceof StreetLocation) {
                return getClosestVertex(location, place.name, options, ((StreetLocation) other).getExtra());
            } else {
                return getClosestVertex(location, place.name, options);
            }
        }
        // did not match lat/lon, interpret place as a vertex label.
        // this should probably only be used in tests.
        return graph.getVertex(place.place);
    }

    @Override
    public boolean isAccessible(NamedPlace place, RoutingRequest options) {
        /* fixme: take into account slope for wheelchair accessibility */
        Vertex vertex = getVertexForPlace(place, options);
        if (vertex instanceof TransitStop) {
            TransitStop ts = (TransitStop) vertex;
            return ts.hasWheelchairEntrance();
        } else if (vertex instanceof StreetLocation) {
            StreetLocation sl = (StreetLocation) vertex;
            return sl.isWheelchairAccessible();
        }
        return true;
    }

}
