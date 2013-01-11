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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.LocationObservation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods used during network linking and trip planning.
 * 
 * Creates a StreetLocation representing a location on a street that's not at an intersection, based on input latitude and longitude. Instantiating
 * this class is expensive, because it creates a spatial index of all of the intersections in the graph.
 */
// @Component
public class StreetVertexIndexServiceImpl implements StreetVertexIndexService {

    private Graph graph;

    /**
     * Contains only instances of {@link StreetEdge}
     */
    private SpatialIndex edgeTree;

    private STRtree transitStopTree;

    private STRtree intersectionTree;

    @Getter
    @Setter
    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    // private static final double SEARCH_RADIUS_M = 100; // meters
    // private static final double SEARCH_RADIUS_DEG = DistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    /* all distance constants here are plate-carée Euclidean, 0.001 ~= 100m at equator */

    // edges will only be found if they are closer than this distance
    public static final double MAX_DISTANCE_FROM_STREET = 0.01000;

    // maximum difference in distance for two geometries to be considered coincident
    public static final double DISTANCE_ERROR = 0.000001;

    // if a point is within MAX_CORNER_DISTANCE, it is treated as at the corner
    private static final double MAX_CORNER_DISTANCE = 0.0001;

    static final Logger _log = LoggerFactory.getLogger(StreetVertexIndexServiceImpl.class);

    public StreetVertexIndexServiceImpl(Graph graph) {
        this.graph = graph;
        setup();
    }

    public StreetVertexIndexServiceImpl(Graph graph, DistanceLibrary distanceLibrary) {
        this.graph = graph;
        this.distanceLibrary = distanceLibrary;
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
            if (v instanceof IntersectionVertex) {
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
        env.expandBy(SphericalDistanceLibrary.metersToDegrees(distance));
        List<Vertex> nearby = transitStopTree.query(env);
        List<Vertex> results = new ArrayList<Vertex>();
        for (Vertex v : nearby) {
            if (distanceLibrary.distance(v.getCoordinate(), c) <= distance) {
                results.add(v);
            }
        }
        return results;
    }

    /**
     * Gets the closest vertex to a coordinate. If necessary, this vertex will be created by splitting nearby edges (non-permanently).
     */
    public Vertex getClosestVertex(final Coordinate coordinate, String name, RoutingRequest options) {
        return getClosestVertex(coordinate, name, options, null);
    }

    public Vertex getClosestVertex(final Coordinate coordinate, String name,
            RoutingRequest options, List<Edge> extraEdges) {
        _log.debug("Looking for/making a vertex near {}", coordinate);

        // first, check for intersections very close by
        StreetVertex intersection = getIntersectionAt(coordinate, MAX_CORNER_DISTANCE);
        if (intersection != null) {
            // coordinate is at a street corner or endpoint
            if (name == null) {
                _log.debug("found intersection {}. not splitting.", intersection);
                // generate names for corners when no name was given
                Set<String> uniqueNameSet = new HashSet<String>();
                for (Edge e : intersection.getOutgoing()) {
                    if (e instanceof StreetEdge) {
                        uniqueNameSet.add(e.getName());
                    }
                }
                List<String> uniqueNames = new ArrayList<String>(uniqueNameSet);
                Locale locale;
                if (options == null) {
                    locale = new Locale("en");
                } else {
                    locale = options.getLocale();
                }
                ResourceBundle resources = ResourceBundle.getBundle("internals", locale);
                String fmt = resources.getString("corner");
                if (uniqueNames.size() > 1) {
                    name = String.format(fmt, uniqueNames.get(0), uniqueNames.get(1));
                } else if (uniqueNames.size() == 1) {
                    name = uniqueNames.get(0);
                } else {
                    name = resources.getString("unnamedStreet");
                }
            }
            StreetLocation closest = new StreetLocation(graph, "corner " + Math.random(),
                    coordinate, name);
            FreeEdge e = new FreeEdge(closest, intersection);
            closest.getExtra().add(e);
            e = new FreeEdge(intersection, closest);
            closest.getExtra().add(e);
            return closest;
        }

        // if no intersection vertices were found, then find the closest transit stop
        // (we can return stops here because this method is not used when street-transit linking)
        double closestStopDistance = Double.POSITIVE_INFINITY;
        Vertex closestStop = null;
        // elsewhere options=null means no restrictions, find anything.
        // here we skip examining stops, as they are really only relevant when transit is being used
        if (options != null && options.getModes().isTransit()) {
            for (Vertex v : getLocalTransitStops(coordinate, 1000)) {
                double d = distanceLibrary.distance(v.getCoordinate(), coordinate);
                if (d < closestStopDistance) {
                    closestStopDistance = d;
                    closestStop = v;
                }
            }
        }
        _log.debug(" best stop: {} distance: {}", closestStop, closestStopDistance);

        // then find closest walkable street
        StreetLocation closestStreet = null;
        CandidateEdgeBundle bundle = getClosestEdges(coordinate, options, extraEdges, null, false);
        CandidateEdge candidate = bundle.best;
        double closestStreetDistance = Double.POSITIVE_INFINITY;
        if (candidate != null) {
            StreetEdge bestStreet = candidate.edge;
            Coordinate nearestPoint = candidate.nearestPointOnEdge;
            closestStreetDistance = distanceLibrary.distance(coordinate, nearestPoint);
            _log.debug("best street: {} dist: {}", bestStreet.toString(), closestStreetDistance);
            if (name == null) {
                name = bestStreet.getName();
            }
            String closestName = String
                    .format("%s_%s", bestStreet.getName(), coordinate.toString());
            closestStreet = StreetLocation.createStreetLocation(graph, closestName, name,
                    bundle.toEdgeList(), nearestPoint, coordinate);
        }

        // decide whether to return street, or street + stop
        if (closestStreet == null) {
            // no street found, return closest stop or null
            _log.debug("returning only transit stop (no street found)");
            return closestStop; // which will be null if none was found
        } else {
            // street found
            if (closestStop != null) {
                // both street and stop found
                double relativeStopDistance = closestStopDistance / closestStreetDistance;
                if (relativeStopDistance < 1.5) {
                    _log.debug("linking transit stop to street (distances are comparable)");
                    closestStreet.addExtraEdgeTo(closestStop);
                }
            }
            _log.debug("returning split street");
            return closestStreet;
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<Vertex> getVerticesForEnvelope(Envelope envelope) {
        return intersectionTree.query(envelope);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CandidateEdgeBundle getClosestEdges(LocationObservation location,
            TraversalRequirements reqs, List<Edge> extraEdges, Collection<Edge> preferredEdges,
            boolean possibleTransitLinksOnly) {
        Coordinate coordinate = location.getCoordinate();
        Envelope envelope = new Envelope(coordinate);

        // Collect the extra StreetEdges to consider.
        Iterable<StreetEdge> extraStreets = IterableLibrary.filter(graph.getTemporaryEdges(),
                StreetEdge.class);
        if (extraEdges != null) {
            extraStreets = Iterables.concat(IterableLibrary.filter(extraEdges, StreetEdge.class),
                    extraStreets);
        }

        double envelopeGrowthAmount = 0.001; // ~= 100 meters
        double radius = 0;
        CandidateEdgeBundle candidateEdges = new CandidateEdgeBundle();
        while (candidateEdges.size() == 0) {
            // expand envelope -- assumes many close searches and occasional far ones
            envelope.expandBy(envelopeGrowthAmount);
            radius += envelopeGrowthAmount;
            if (radius > MAX_DISTANCE_FROM_STREET) {
                return candidateEdges; // empty list
            }

            Iterable<StreetEdge> nearbyEdges = edgeTree.query(envelope);
            if (nearbyEdges != null) {
                nearbyEdges = Iterables.concat(nearbyEdges, extraStreets);
            }

            // oh. This is part of the problem: we're not linking to one-way
            // streets, even though that is a perfectly reasonable thing to do.
            // we need to handle that using bundles.
            for (StreetEdge e : nearbyEdges) {
                // Ignore invalid edges.
                if (e == null || e.getFromVertex() == null) {
                    continue;
                }

                // Ignore those edges we can't traverse
                if (!reqs.canBeTraversed(e)) {
                    // NOTE(flamholz): canBeTraversed checks internally if we
                    // can walk our bike on this StreetEdge.
                    continue;
                }

                // Compute preference value
                double preferrence = 1;
                if (preferredEdges != null && preferredEdges.contains(e)) {
                    preferrence = 3.0;
                }

                TraverseModeSet modes = reqs.getModes();
                CandidateEdge ce = new CandidateEdge(e, location, preferrence, modes);

                // Even if an edge is outside the query envelope, bounding boxes can
                // still intersect. In this case, distance to the edge is greater
                // than the query envelope size.
                if (ce.getDistance() < radius) {
                    candidateEdges.add(ce);
                }
            }
        }

        Collection<CandidateEdgeBundle> bundles = candidateEdges.binByDistanceAndAngle();
        // initially set best bundle to the closest bundle
        CandidateEdgeBundle best = null;
        for (CandidateEdgeBundle bundle : bundles) {
            if (best == null || bundle.best.score < best.best.score) {
                if (possibleTransitLinksOnly) {
                    if (!(bundle.allowsCars() || bundle.isPlatform()))
                        continue;
                }
                best = bundle;
            }
        }

        return best;
    }

    @Override
    public CandidateEdgeBundle getClosestEdges(LocationObservation location,
            TraversalRequirements reqs) {
        return getClosestEdges(location, reqs, null, null, false);
    }

    /**
     * Find edges closest to the given location.
     * 
     * @param coordinate Point to get edges near
     * @param request RoutingRequest that must be able to traverse the edge (all edges if null)
     * @param extraEdges Any edges not in the graph that might be included (allows trips within one block)
     * @param preferredEdges Any edges to prefer in the search
     * @param possibleTransitLinksOnly only return edges traversable by cars or are platforms
     * @return
     */
    public CandidateEdgeBundle getClosestEdges(Coordinate coordinate, RoutingRequest request,
            List<Edge> extraEdges, Collection<Edge> preferredEdges, boolean possibleTransitLinksOnly) {
        // Make a LocationObservation from a coordinate.
        LocationObservation loc = new LocationObservation(coordinate);

        // NOTE(flamholz): if request is null, will initialize TraversalRequirements
        // that accept all modes of travel.
        TraversalRequirements reqs = new TraversalRequirements(request);

        return getClosestEdges(loc, reqs, extraEdges, preferredEdges, possibleTransitLinksOnly);
    }

    public StreetVertex getIntersectionAt(Coordinate coordinate) {
        return getIntersectionAt(coordinate, MAX_CORNER_DISTANCE);
    }

    @SuppressWarnings("unchecked")
    public StreetVertex getIntersectionAt(Coordinate coordinate, double distanceError) {
        Envelope envelope = new Envelope(coordinate);
        envelope.expandBy(distanceError * 2);
        List<StreetVertex> nearby = intersectionTree.query(envelope);
        StreetVertex nearest = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (StreetVertex v : nearby) {
            double distance = coordinate.distance(v.getCoordinate());
            if (distance < distanceError) {
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = v;
                }
            }
        }
        return nearest;
    }

    @Override
    /** radius is meters */
    public List<TransitStop> getNearbyTransitStops(Coordinate coordinate, double radius) {
        Envelope envelope = new Envelope(coordinate);

        envelope.expandBy(SphericalDistanceLibrary.metersToDegrees(radius));
        List<?> stops = transitStopTree.query(envelope);
        ArrayList<TransitStop> out = new ArrayList<TransitStop>();
        for (Object o : stops) {
            TransitStop stop = (TransitStop) o;
            if (distanceLibrary.distance(stop.getCoordinate(), coordinate) < radius) {
                out.add(stop);
            }
        }
        return out;
    }

    @Override
    /** radius is meters */
    public List<TransitStop> getNearbyTransitStops(Coordinate coordinateOne,
            Coordinate coordinateTwo) {
        Envelope envelope = new Envelope(coordinateOne, coordinateTwo);

        List<?> stops = transitStopTree.query(envelope);
        ArrayList<TransitStop> out = new ArrayList<TransitStop>();
        for (Object o : stops) {
            TransitStop stop = (TransitStop) o;
            out.add(stop);
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

    /**
     * @param other: non-null when another vertex has already been found. When the from vertex has
     * already been made/found, that vertex is passed in when finding/creating the to vertex. 
     * TODO: This appears to be for reusing the extra edges list -- is this still needed?
     */
    @Override
    public Vertex getVertexForPlace(NamedPlace place, RoutingRequest options, Vertex other) {
        if (place == null || place.place == null) {
            return null;
        }

        Matcher matcher = _latLonPattern.matcher(place.place);
        if (matcher.matches()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(4));
            Coordinate location = new Coordinate(lon, lat);
            if (other instanceof StreetLocation) {
                return getClosestVertex(location, place.name, options,
                        ((StreetLocation) other).getExtra());
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
