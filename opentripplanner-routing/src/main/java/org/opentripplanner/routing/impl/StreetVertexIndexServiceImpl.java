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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.GraphRefreshListener;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
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
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * Indexes all edges and transit vertices of the graph spatially. Has a variety of query methods
 * used during network linking and trip planning.
 * 
 * Creates a StreetLocation representing a location on a street that's not at an intersection, based
 * on input latitude and longitude. Instantiating this class is expensive, because it creates a
 * spatial index of all of the intersections in the graph.
 */
@Component
public class StreetVertexIndexServiceImpl implements StreetVertexIndexService, GraphRefreshListener {

    private GraphService graphService;

    /**
     * Contains only instances of {@link StreetEdge}
     */
    private SpatialIndex edgeTree;

    private STRtree transitStopTree;

    private STRtree intersectionTree;

    public static final double MAX_DISTANCE_FROM_STREET = 0.05;

    public static final double DISTANCE_ERROR = 0.00005;

    //if a point is within MAX_CORNER_DISTANCE, it is treated as at the corner
    private static final double MAX_CORNER_DISTANCE = 0.00010;

    private static final double DIRECTION_ERROR = 0.05;

    private static final Logger _log = LoggerFactory.getLogger(StreetVertexIndexServiceImpl.class);

    public StreetVertexIndexServiceImpl() {
    }

    public StreetVertexIndexServiceImpl(Graph graph) {
        this.graphService = new GraphServiceBeanImpl(graph);
    }

    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
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

    @Override
    public void handleGraphRefresh(GraphService graphService) {
        this.graphService = graphService;
        setup();
    }

    private void postSetup() {

        Graph graph = graphService.getGraph();

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
                if (ts.isEntrance() || !ts.hasEntrances()) {
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
    public Vertex getClosestVertex(final Coordinate coordinate, String name, TraverseOptions options) {
        return getClosestVertex(coordinate, name, options, null);
    }

    public Vertex getClosestVertex(final Coordinate coordinate, String name, TraverseOptions options, List<Edge> extraEdges) {
        _log.debug("Looking for/making a vertex near {}", coordinate);

        // first, check for intersections very close by
        List<StreetVertex> vertices = getIntersectionAt(coordinate);
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
        if (options != null && options.getModes().getTransit()) {
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
        Collection<StreetEdge> edges = getClosestEdges(coordinate, options, extraEdges);
        double closest_street_distance = Double.POSITIVE_INFINITY;
        if (edges != null) {
            StreetEdge bestStreet = edges.iterator().next();
            Geometry g = bestStreet.getGeometry();
            LocationIndexedLine l = new LocationIndexedLine(g);
            LinearLocation location = l.project(coordinate);
            Coordinate nearestPoint = location.getCoordinate(g);
            closest_street_distance = DistanceLibrary.distance(coordinate, nearestPoint);
            _log.debug("best street: {} dist: {}", bestStreet.toString(), closest_street_distance);
            if (name == null) {
                name = bestStreet.getName();
            }
            closest_street = StreetLocation.createStreetLocation(bestStreet.getName() + "_"
                    + coordinate.toString(), name, edges, nearestPoint);
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

    public Collection<StreetEdge> getClosestEdges(Coordinate coordinate, TraverseOptions options) {
        return getClosestEdges(coordinate, options, null);
    }

    @SuppressWarnings("unchecked")
    public Collection<StreetEdge> getClosestEdges(Coordinate coordinate, TraverseOptions options, List<Edge> extraEdges) {
        ArrayList<StreetEdge> extraStreets = new ArrayList<StreetEdge> ();
        if (extraEdges != null) {
            for (Edge e : extraEdges) {
                if (e instanceof StreetEdge) {
                    extraStreets.add((StreetEdge) e);
                }
            }
        }
        Envelope envelope = new Envelope(coordinate);
        int i = 0;
        double envelopeGrowthRate = 0.0002;
        GeometryFactory factory = new GeometryFactory();
        Point p = factory.createPoint(coordinate);
        double bestDistance = Double.MAX_VALUE;

        TraverseOptions walkingOptions = null;
        if (options != null) {
            walkingOptions = options.getWalkingOptions();
        }
        while (bestDistance > MAX_DISTANCE_FROM_STREET && i < 10) {
            ++i;
            envelope.expandBy(envelopeGrowthRate);
            envelopeGrowthRate *= 2;

            /*
             * It is presumed, that edges which are roughly the same distance from the examined
             * coordinate in the same direction are parallel (coincident) edges.
             * 
             * Parallel edges are needed to account for (oneway) streets with varying permissions,
             * as well as the edge-based nature of the graph. i.e. using a C point on a oneway
             * street a cyclist may go in one direction only, while a pedestrian should be able to
             * go in any direction.
             */

            bestDistance = Double.MAX_VALUE;
            StreetEdge bestEdge = null;
            List<StreetEdge> nearby = edgeTree.query(envelope);
            if (extraEdges != null && nearby != null) {
                nearby = new JoinedList<StreetEdge>(nearby, extraStreets);
            }
            if (nearby != null) {
                for (StreetEdge e : nearby) {
                    if (e == null || e instanceof OutEdge)
                        continue;
                    Geometry g = e.getGeometry();
                    if (g != null) {
                        if (options != null) {
                            if (!(e.canTraverse(options) || e.canTraverse(walkingOptions))) {
                                continue;
                            }
                        }
                        double distance = g.distance(p);
                        if (distance > envelope.getWidth() / 2) {
                            // Even if an edge is outside the query envelope, bounding boxes can
                            // still intersect. In this case, distance to the edge is greater
                            // than the query envelope size.
                            continue;
                        }
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestEdge = e;
                        }
                    }
                }

                // find coincidence edges
                if (bestDistance <= MAX_DISTANCE_FROM_STREET) {
                    LocationIndexedLine lil = new LocationIndexedLine(bestEdge.getGeometry());
                    LinearLocation location = lil.project(coordinate);
                    Coordinate nearestPointOnEdge = lil.extractPoint(location);
                    double xd = nearestPointOnEdge.x - coordinate.x;
                    double yd = nearestPointOnEdge.y - coordinate.y;
                    double edgeDirection = Math.atan2(yd, xd);

                    /**
                     * If the edgeDirection is NaN, it means the edge has no length and therefore no
                     * direction, so we just return it directly instead of looking for parallel
                     * edges
                     */
                    if (Double.isNaN(edgeDirection))
                        return Arrays.asList(bestEdge);

                    List<StreetEdge> parallel = new ArrayList<StreetEdge>();
                    for (StreetEdge e : nearby) {
                        /* only include edges that this user can actually use */
                        if (e == null || e instanceof OutEdge) {
                            continue;
                        }
                        if (options != null) {
                            if (!(e.canTraverse(options) || e.canTraverse(walkingOptions))) {
                                continue;
                            }
                        }
                        Geometry eg = e.getGeometry();
                        if (eg != null) {
                            double distance = eg.distance(p);

                            if (distance <= bestDistance + DISTANCE_ERROR) {

                                lil = new LocationIndexedLine(eg);
                                location = lil.project(coordinate);
                                nearestPointOnEdge = lil.extractPoint(location);

                                if (distance > bestDistance) {
                                    /*
                                     * ignore edges caught end-on unless they're the only choice
                                     */
                                    Coordinate[] coordinates = eg.getCoordinates();
                                    if (nearestPointOnEdge.equals(coordinates[0])
                                            || nearestPointOnEdge
                                                    .equals(coordinates[coordinates.length - 1])) {
                                        continue;
                                    }
                                }

                                /* compute direction from coordinate to edge */
                                xd = nearestPointOnEdge.x - coordinate.x;
                                yd = nearestPointOnEdge.y - coordinate.y;
                                double direction = Math.atan2(yd, xd);

                                if (Math.abs(direction - edgeDirection) < DIRECTION_ERROR) {
                                    parallel.add(e);
                                }
                            }
                        }
                    }
                    return parallel;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<StreetVertex> getIntersectionAt(Coordinate coordinate) {
        Envelope envelope = new Envelope(coordinate);
        envelope.expandBy(DISTANCE_ERROR * 2);
        List<StreetVertex> nearby = intersectionTree.query(envelope);
        List<StreetVertex> atIntersection = new ArrayList<StreetVertex>(nearby.size());
        for (StreetVertex v : nearby) {
            if (coordinate.distance(v.getCoordinate()) < DISTANCE_ERROR) {
                atIntersection.add(v);
            }
        }
        if (atIntersection.isEmpty()) {
            return null;
        }
        return atIntersection;
    }

}
