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

package org.opentripplanner.graph_builder.module.map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.math3.util.FastMath;
import org.geotools.math.Statistics;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.edgetype.loader.LinkRequest;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.GeometryCSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by mabu on 8.4.2015.
 */
public class TransitToStreetNetworkLinkerModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory
        .getLogger(TransitToStreetNetworkLinkerModule.class);

    private final double DISTANCE_THRESHOLD = SphericalDistanceLibrary.metersToDegrees(22);

    private SpatialIndex index;

    private Graph graph;

    private GeometryCSVWriter writerStopShapesGeo;

    private GeometryCSVWriter writerTransitStop;

    private GeometryCSVWriter writerMatchedStreets;

    private GeometryCSVWriter writerClosestPoints;

    private GeometryCSVWriter writerAzimuth;

    private GeometryCSVWriter writerPoint;

    private Multiset<Integer> counts;

    private Statistics statsMinDistance;

    private StreetMatcher streetMatcher;

    private int numOfStopsEdgesNotMatchedToShapes;

    private NetworkLinkerLibrary networkLinkerLibrary;

    int numStopsMinDistanceBigger1;

    int numStopsMinDIstanceBigger20;

    private static final double ANGLE_DIFF = Math.PI / 18; // 10 degrees

    STRtree createIndex() {
        STRtree edgeIndex = new STRtree();
        for (Vertex v : graph.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                if (e instanceof StreetEdge || e instanceof PublicTransitEdge) {
                    Envelope envelope;
                    Geometry geometry = e.getGeometry();
                    envelope = geometry.getEnvelopeInternal();
                    edgeIndex.insert(envelope, e);
                }
            }
        }
        edgeIndex.build();
        LOG.debug("Created index");
        return edgeIndex;
    }

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // why not "transit" ?
    }

    /**
     * Gets which vertex is closer to the start shape vertex
     * <p/>
     * This vertex needs to be extended to see if better matching can be found
     *
     * @param shape         Part of GTFS shape from shape_dist_traveled +4 vertices
     * @param closestStreet found closest street with {@link StreetMatcher}
     * @return True if it is from vertex in closestStreet false otherwise
     */
    //private boolean getClosestPoint(LineString shape, Edge closestStreet) {
    private boolean getClosestPoint(LineString shape, Geometry closestStreetGeometry) {
        Coordinate firstC, lastC;
        if (closestStreetGeometry instanceof LineString) {
            firstC = ((LineString) closestStreetGeometry).getCoordinateN(0);
            lastC = ((LineString) closestStreetGeometry)
                .getCoordinateN(closestStreetGeometry.getNumPoints() - 1);
        } else if (closestStreetGeometry instanceof MultiLineString) {
            firstC = ((LineString) closestStreetGeometry.getGeometryN(0)).getCoordinateN(0);
            LineString lastGeometry = (LineString) closestStreetGeometry
                .getGeometryN(closestStreetGeometry.getNumGeometries() - 1);
            lastC = lastGeometry.getCoordinateN(lastGeometry.getNumPoints() - 1);
        } else {

            //TODO: exception
            return false;
        }
        //First vertex from shape is always checked since this should be vertex
        // which should be closest since from this point on transit geometry will be drawn.
        //LineString closestStreetGeometry = closestStreet.getGeometry();
        Coordinate a0 = null, cur_a;
        Coordinate b0 = null, cur_b;
        boolean fromVertex;
        double distance = Double.MAX_VALUE;
        //A0 vs B0
        cur_a = shape.getCoordinateN(0);
        cur_b = firstC;
        fromVertex = true;
        double cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            distance = cur_distance;
        }

        //A0 vs BLast
        cur_b = lastC;
        cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            fromVertex = false;
            distance = cur_distance;
        }

        //We only need to check first shape vertices. because this is where shape starts
        /*
        //ALast vs BLast
        cur_a = shape.getCoordinateN(shape.getNumPoints() - 1);
        cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            fromVertex = false;
            distance = cur_distance;
        }

        //Alast vs B0
        cur_b = closestStreetGeometry.getCoordinateN(0);
        cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            fromVertex = true;
            distance = cur_distance;
        }
        */

        return fromVertex;
    }

    /**
     * Returns list of edges and points which are closest to GTFS shapes. This can be {@link StreetEdge} where bus drives
     * or {@link PublicTransitEdge} where TRAM/RAIL/subway drives. And a point which is a point where would edge
     * is closest to transitStop according to GTFS.
     * <p/>
     * List is returned because some stops are connected to multiple bus routes or to different transit types. (Bus/tram most often)
     *
     * @param transitStop
     * @return
     */
    private List<EdgePoint> getTransitEdges(TransitStop transitStop) {
        int geometries_count = transitStop.geometries.size();
        List<EdgePoint> closestEdges = new ArrayList<>(geometries_count);

        if (geometries_count != 1) {
            // System.out.println("Stop ID: " + transitStop.getLabel() + "(" + transitStop.getName() + "|" + geometries_count + ")");
        }
        writerTransitStop.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
            Integer.toString(geometries_count), transitStop.getModes().toString()),
            transitStop.getCoordinate());

        counts.add(geometries_count);

        //All geometries near stop
        for (T2<LineString, TraverseMode> GTFSShape : transitStop.geometries) {
            if (geometries_count != 1) {
                // System.out.println("  shape:" + GTFSShape.first.toString() + "[" + GTFSShape.first.hashCode() + "]||" + GTFSShape.second.toString());
            }
            writerStopShapesGeo.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                    transitStop.getLabel() + " " + GTFSShape.second.toString(),
                    Integer.toString(geometries_count), Double.toString(FastMath.toDegrees(Angle
                            .normalizePositive(DirectionUtils.getAzimuthRad(GTFSShape.first))))),
                GTFSShape.first);
            List<Edge> edges = streetMatcher.match(GTFSShape.first, GTFSShape.second);
            if (edges == null) {
                numOfStopsEdgesNotMatchedToShapes++;
                continue;
            }
            transitStop.addLevel(((EdgeInfo) edges.get(0)).getLevel());
            LineString[] lss = new LineString[edges.size()];
            int edge_idx = 0;
            TLongSet edge_ids = new TLongHashSet(5);
            double minDistance = Double.MAX_VALUE;
            for (Edge edge : edges) {
                //TODO: minDistance should be calculated between projection of STOP to found? This means that broken GTFS shapes isn't such a big problem
                minDistance = Math.min(minDistance, SphericalDistanceLibrary
                    .fastDistance(GTFSShape.first.getCoordinateN(0), edge.getGeometry()));
                lss[edge_idx] = edge.getGeometry();
                edge_idx++;
                if (edge instanceof EdgeInfo) {
                    edge_ids.add(((EdgeInfo) edge).getOsmID());
                }
            }
            /*if (minDistance <= 20) {
                statsMinDistance.add(minDistance);
            }*/
            String edge_ids_show;
            if (!edge_ids.isEmpty()) {
                edge_ids_show = "";
                for (long edge_id : edge_ids.toArray()) {
                    edge_ids_show += Long.toString(edge_id) + "|";
                }
            } else {
                edge_ids_show = "??";
            }
            //LineMerger lineMerger = new LineMerger();
            //lineMerger.add(edges);
            LineString foundEdgeGeometry = edges.get(0)
                .getGeometry(); //(LineString) lineMerger.getMergedLineStrings().iterator().next();
            MultiLineString multiLineString = GeometryUtils.getGeometryFactory()
                .createMultiLineString(lss);

            boolean isFromVertexClosest = getClosestPoint(GTFSShape.first, multiLineString);

            Edge cur_edge = null;
            double prevMinDistance = minDistance;
            if (minDistance <= 20) {

                //This tries to lengthen start or end of found edge to try to find even better matching
                //because sometimes point where we should link is right after ending of edge or right before it starts.
                if (isFromVertexClosest) {
                    for (Edge edge : edges.get(0).getFromVertex().getIncoming()) {
                        if (edge instanceof EdgeInfo) {
                            double cur_distance = SphericalDistanceLibrary
                                .fastDistance(GTFSShape.first.getCoordinateN(0),
                                    edge.getGeometry());
                            if (cur_distance < minDistance) {
                                minDistance = cur_distance;
                                cur_edge = edge;
                            }
                        }
                    }
                    if (cur_edge != null) {
                        LOG.info("Found edge closer in fromVertex for stop: {} prev:{} now:{}",
                            transitStop.getLabel(), prevMinDistance, minDistance);
                        foundEdgeGeometry = cur_edge.getGeometry();
                        LineString[] lss1 = new LineString[lss.length + 1];
                        lss1[0] = cur_edge.getGeometry();
                        for (int i = 0; i < lss.length; i++) {
                            lss1[i + 1] = lss[i];
                        }
                        edge_ids_show =
                            Long.toString(((EdgeInfo) cur_edge).getOsmID()) + "|" + edge_ids_show;
                        multiLineString = GeometryUtils.getGeometryFactory()
                            .createMultiLineString(lss1);
                    }
                } else {
                    for (Edge edge : edges.get(edges.size() - 1).getToVertex().getOutgoing()) {
                        if (edge instanceof EdgeInfo) {
                            double cur_distance = SphericalDistanceLibrary
                                .fastDistance(GTFSShape.first.getCoordinateN(0),
                                    edge.getGeometry());
                            if (cur_distance < minDistance) {
                                minDistance = cur_distance;
                                cur_edge = edge;
                            }
                        }
                    }
                    if (cur_edge != null) {
                        LOG.info("Found edge closer  in toVertex for stop: {} prev:{} now:{}",
                            transitStop.getLabel(), prevMinDistance, minDistance);
                        foundEdgeGeometry = cur_edge.getGeometry();
                        LineString[] lss1 = new LineString[lss.length + 1];
                        for (int i = 0; i < lss.length; i++) {
                            lss1[i] = lss[i];
                        }
                        lss1[lss1.length - 1] = cur_edge.getGeometry();
                        edge_ids_show += Long.toString(((EdgeInfo) cur_edge).getOsmID());
                        multiLineString = GeometryUtils.getGeometryFactory()
                            .createMultiLineString(lss1);
                    }
                }
                //Some edge which is even closer then previous was found
                if (cur_edge != null) {

                    closestEdges.add(new EdgePoint(cur_edge, GTFSShape.first, GTFSShape.second));
                } else {
                    closestEdges
                        .add(new EdgePoint(edges.get(0), GTFSShape.first, GTFSShape.second));
                }
                statsMinDistance.add(minDistance);
            } else {
                LOG.warn("STOP: {} found edge is too far: {}", transitStop.getLabel(), minDistance);
            }

            //This is for debugging
            if (minDistance > 1) {
                numStopsMinDistanceBigger1++;
                if (minDistance > 20) {
                    numStopsMinDIstanceBigger20++;
                }
            }

            String distance = Double.toString(
                minDistance), max_distance = "0.0", trimm_distance = "0.0", nearness_distance = "0.0";
            String closest = "", closest_max = "", closest_trim = "", closest_nearnes = "";
            String distant = "";
            String parallel = "0";
            if (parallel(GTFSShape.first, foundEdgeGeometry)) {
                parallel = "1";
            }

            writerMatchedStreets.add(Arrays
                    .asList(transitStop.getLabel(), transitStop.getName(), distance, max_distance,
                        trimm_distance, nearness_distance, closest, closest_max, closest_trim,
                        closest_nearnes, Integer.toString(geometries_count), edge_ids_show, distant,
                        parallel, Double.toString(FastMath.toDegrees(Angle.normalizePositive(
                                DirectionUtils.getAzimuthRad(foundEdgeGeometry))))),
                multiLineString);

            Point[] pts = new Point[2];
            pts[0] = GeometryUtils.getGeometryFactory()
                .createPoint(GTFSShape.first.getCoordinateN(0));
            if (isFromVertexClosest) {
                pts[1] = GeometryUtils.getGeometryFactory()
                    .createPoint(foundEdgeGeometry.getCoordinateN(0));
            } else {
                pts[1] = GeometryUtils.getGeometryFactory().createPoint(
                    foundEdgeGeometry.getCoordinateN(foundEdgeGeometry.getNumPoints() - 1));
            }
            Geometry closestPoints = GeometryUtils.getGeometryFactory().createMultiPoint(pts);
            writerClosestPoints.add(Arrays
                .asList(transitStop.getLabel(), transitStop.getName(), closest, closest_max,
                    closest_trim, closest_nearnes, edge_ids_show, distant, parallel),
                closestPoints);
            if (geometries_count != 1) {
               /* System.out.println("    (" + closest + "|" + closest_max + "|" + closest_trim +
                        "|" + closest_nearnes + "||" + parallel + ")" + " edgeID:" + edge_ids_show);
                        */
            }
        }
        return closestEdges;
    }

    private boolean parallel(LineString GTFSShape, Edge foundEdge) {
        double shapeAzimuthRad = DirectionUtils.getAzimuthRad(GTFSShape);
        double edgeAzimuthRad = DirectionUtils
            .getAzimuthRad(foundEdge.getFromVertex().getCoordinate(),
                foundEdge.getToVertex().getCoordinate());
        return parallel(shapeAzimuthRad, edgeAzimuthRad);
    }

    private boolean parallel(LineString GTFSShape, LineString edge) {
        double shapeAzimuthRad = DirectionUtils.getAzimuthRad(GTFSShape);
        double edgeAzimuthRad = DirectionUtils.getAzimuthRad(edge);
        return parallel(shapeAzimuthRad, edgeAzimuthRad);
    }

    private boolean parallel(double shapeAzimuthRad, double edgeAzimuthRad) {
        double angDiff = Angle.diff(shapeAzimuthRad, edgeAzimuthRad);
        double angInvDiff = Angle.diff(shapeAzimuthRad, Angle.normalize(edgeAzimuthRad + Math.PI));

        double minAngDiff = Math.min(angDiff, angInvDiff);
        return minAngDiff < ANGLE_DIFF;
        //TODO: some problems with round streets -116 shape -145 matched -172 shape all basically parallel the only problem is that there is very little overlap
    }

    @Override public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Linking transit stops to streets...with help of GTFS shapes");
        this.graph = graph;
        this.index = createIndex();
        ArrayList<Vertex> vertices = new ArrayList<>();
        vertices.addAll(graph.getVertices());

        this.networkLinkerLibrary = new NetworkLinkerLibrary(graph, extra);
        writerStopShapesGeo = new GeometryCSVWriter(
            Arrays.asList("stop_id", "stop_name", "trip_pattern", "num_geo", "azimuth", "geo"),
            "geo", "outMatcher/MBpatterns.csv", false);
        writerTransitStop = new GeometryCSVWriter(
            Arrays.asList("stop_id", "stop_name", "num_geo", "geo", "modes"), "geo",
            "outMatcher/MBpattern_stop.csv", false);
        writerMatchedStreets = new GeometryCSVWriter(Arrays
            .asList("stop_id", "stop_name", "distance", "max_distance", "trimmed_distance",
                "nearness_distance", "is_closest", "is_closest_max_d", "is_closest_trim_d",
                "is_nearness_d", "num_geo", "edge_id", "distant", "parallel", "azimuth", "geo"),
            "geo", "outMatcher/MBpattern_match.csv", false);
        writerClosestPoints = new GeometryCSVWriter(Arrays
            .asList("stop_id", "stop_name", "is_closest", "is_closest_max_d", "is_closest_trim_d",
                "is_nearness_d", "edge_id", "distant", "parallel", "geo"), "geo",
            "outMatcher/pattern_point.csv", false);
        //writerAzimuth = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", ))
        writerPoint = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", "mode", "geo"),
            "geo", "outMatcher/MBpattern_wpoint.csv", false);
        counts = HashMultiset.create(10);
        statsMinDistance = new Statistics();
        streetMatcher = new StreetMatcher(graph, (STRtree) index);
        numOfStopsEdgesNotMatchedToShapes = 0;
        int nUnlinked = 0;
        numStopsMinDistanceBigger1 = 0;
        numStopsMinDIstanceBigger20 = 0;
        //first we need to figure out on which streets PT is driving
        //for each TransitStop we found street on which PT is driving
        for (TransitStop transitStop : Iterables.filter(vertices, TransitStop.class)) {
            // if the street is already linked there is no need to linked it again,
            // could happened if using the prune isolated island
            boolean alreadyLinked = false;
            for (Edge e : transitStop.getOutgoing()) {
                if (e instanceof StreetTransitLink) {
                    alreadyLinked = true;
                    break;
                }
            }

            //for each street we found if it is correct to link to or if it has neighbour streets which are parallel to it to which we can link
            //we add streets to linked streets
            List<EdgePoint> edges = getTransitEdges(transitStop);

            //statsMinDistance.add(transitStop.getLevels().size());
            if (alreadyLinked)
                continue;
            if (edges.isEmpty()) {
                LOG.info(graph.addBuilderAnnotation(new StopUnlinked(transitStop)));
                nUnlinked += 1;
                continue;
            }

            int edge_index = 0;
            for (EdgePoint edgePoint : edges) {
                //writerPoint.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(), transitStop.getModes().toString()), edgePoint.getClosestPoint());
                StreetEdge se;
                if (edgePoint.getEdge() instanceof StreetEdge) {
                    se = (StreetEdge) edgePoint.getEdge();
                } else {
                    LOG.info("Skipping transitMode {} in stop: {}", edgePoint.getTraverseMode(),
                        transitStop);
                    continue;
                }

                LinkRequest request = new LinkRequest(networkLinkerLibrary);
                //request.connectVertexToStreets(edgePoint.getClosestPoint(), transitStop.hasWheelchairEntrance());
                String vertexLabel;
                vertexLabel = "link for " + transitStop.getStopId();

                Coordinate coordinate = edgePoint.getClosestPoint();
                /*LocationIndexedLine indexedEdge = new LocationIndexedLine(edgePoint.getEdge().getGeometry());
                LinearLocation stopLocation = indexedEdge.project(transitStop.getCoordinate());

                coordinate = stopLocation.getCoordinate(edgePoint.getEdge().getGeometry());*/
                writerPoint.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                    transitStop.getModes().toString()), coordinate);
                // if the bundle was caught endwise (T intersections and dead ends),
                // get the intersection instead.
                Collection<StreetVertex> nearbyStreetVertices = null; // new ArrayList<>(5);
                //if (edges.endwise())
                //TODO: numOfStopsEdgesNotMatchedToShapes
                //else
                /* is the stop right at an intersection? */
                /*StreetVertex atIntersection = networkLinkerLibrary.index.getIntersectionAt(coordinate);
                // if so, the stop can be linked directly to all vertices at the intersection
                if (atIntersection != null) {
                    //if intersection isn't publicTransit intersection
                    if (!atIntersection.getOutgoingStreetEdges().isEmpty()) {
                        nearbyStreetVertices = Arrays.asList(atIntersection);
                        LOG.info("Connecting stop {} to intersection", transitStop);
                    }
                }
                */

                if (nearbyStreetVertices == null) {
                    nearbyStreetVertices = request
                        .getSplitterVertices(vertexLabel, Arrays.asList(se), coordinate);
                    //LOG.info("Splitting edge for stop {}", transitStop);
                }

                if (nearbyStreetVertices != null) {
                    boolean wheelchairAccessible = transitStop.hasWheelchairEntrance();
                    //Actual linking happens
                    for (StreetVertex sv : nearbyStreetVertices) {
                        new StreetTransitLink(sv, transitStop, wheelchairAccessible);
                        new StreetTransitLink(transitStop, sv, wheelchairAccessible);
                    }
                } else {
                    LOG.info(graph.addBuilderAnnotation(new StopUnlinked(transitStop)));
                    nUnlinked += 1;
                }
                edge_index++;
            }

        }
        if (nUnlinked > 0) {
            LOG.warn(
                "{} transit stops were not close enough to the street network to be connected to it.",
                nUnlinked);
        }
        LOG.info("Search finished");
        writerStopShapesGeo.close();
        writerClosestPoints.close();
        writerTransitStop.close();
        writerMatchedStreets.close();
        writerPoint.close();
        for (Multiset.Entry<Integer> stop : counts.entrySet()) {
            System.out.println(stop.getElement() + ": " + stop.getCount());
        }
        System.out.println("Distance between shapes & found edges:");
        System.out.println(statsMinDistance.toString());
        System.out
            .println("numOfStopsEdgesNotMatchedToShapes: " + numOfStopsEdgesNotMatchedToShapes);
        LOG.info("minDistance > 1 : {}, > 20: {}", numStopsMinDistanceBigger1,
            numStopsMinDIstanceBigger20);


        //remove replaced edges
        for (HashSet<StreetEdge> toRemove : networkLinkerLibrary.getReplacements().keySet()) {
            for (StreetEdge edge : toRemove) {
                edge.getFromVertex().removeOutgoing(edge);
                edge.getToVertex().removeIncoming(edge);
            }
        }
        //and add back in replacements
        for (LinkedList<P2<StreetEdge>> toAdd : networkLinkerLibrary.getReplacements().values()) {
            for (P2<StreetEdge> edges : toAdd) {
                StreetEdge edge1 = edges.first;
                if (edge1.getToVertex().getLabel().startsWith("split ") || edge1.getFromVertex()
                    .getLabel().startsWith("split ")) {
                    continue;
                }
                edge1.getFromVertex().addOutgoing(edge1);
                edge1.getToVertex().addIncoming(edge1);
                StreetEdge edge2 = edges.second;
                if (edge2 != null) {
                    edge2.getFromVertex().addOutgoing(edge2);
                    edge2.getToVertex().addIncoming(edge2);
                }
            }
        }
        //TODO: problems, when GTFS doesn't have shapes or train/bus/tram/subway big stations
    }

    @Override public void checkInputs() {
        //no inputs
    }
}
