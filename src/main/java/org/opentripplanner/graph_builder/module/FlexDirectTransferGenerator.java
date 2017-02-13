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

package org.opentripplanner.graph_builder.module;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.apache.commons.math3.util.Pair;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.module.map.StreetMatcher;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PartialPatternHop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link GraphBuilderModule} module that links flex-enabled PatternHops to other ones or TransitStops.
 *
 * It will use the street network if OSM data has already been loaded into the graph.
 * Otherwise it will use straight-line distance between stops.
 *
 */
public class FlexDirectTransferGenerator implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(FlexDirectTransferGenerator.class);

    private static final double MAX_DISTANCE = 1000;
    private static final double TRANSIT_STOP_CUTOFF = 100;

    public List<String> provides() {
        return Arrays.asList("linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("street to transit");
    }

    StreetMatcher matcher;
    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        /* Initialize graph index which is needed by the nearby stop finder. */
        if (graph.index == null) {
            graph.index = new GraphIndex(graph);
        }

        /* Initialize street matcher. */
        matcher = new StreetMatcher(graph);

        int nTransfersTotal = 0;
        int nLinkableHops = 0;

        for (TripPattern tripPattern : graph.index.patternsForFeedId.values()) {
            for (PatternHop hop : tripPattern.getPatternHops()) {

                // for each hop, find nearby stops and hops. create transfers.
                if (hop.getContinuousDropoff() > 0 || hop.getContinuousPickup() > 0) {
                    if (++nLinkableHops % 1000 == 0) {
                        LOG.info("Linked {} hops", nLinkableHops);
                    }

                    // Exclude a transfer point if the transfer from DirectTransferGenerator would be better:
                    // - it is a TransitStop and it is within 100m of either endpoint of the hop
                    // - it is a hop where either endpoint is within 100m of either endpoint of this hop.

                    Collection<TransferPointAtDistance> pts =  findNearbyTransferPoints(graph, hop);

                    for (TransferPointAtDistance pt : pts) {
                        if (!shouldExclude(hop, pt)) {
                            link(graph, hop, pt);
                        }
                    }
                }

            }
        }


        LOG.info("Done creating transfers for flex hops. Created a total of {} transfers from {} hops.", nTransfersTotal, nLinkableHops);
    }

    @Override
    public void checkInputs() {
        // No inputs
    }


    // This is based on NearbyStopFinder but more flexible
    // In addition, we *require* that we have streets (can't use Euclidean distance). Flag stops make no sense without streets.
    private Collection<TransferPointAtDistance> findNearbyTransferPoints(Graph graph, PatternHop hop) {
        Multimap<TripPattern, TransferPointAtDistance> closestTransferPointForTripPattern = ArrayListMultimap.create();

        RoutingRequest request = new RoutingRequest(TraverseMode.WALK);
        request.softWalkLimiting = false;
        request.maxWalkDistance = MAX_DISTANCE;
        request.batch = true;
        request.setRoutingContext(graph, hop.getFromVertex(), hop.getToVertex());

        GenericDijkstra gd = new GenericDijkstra(request);

        gd.setHeuristic(new TrivialRemainingWeightHeuristic());
        gd.traverseVisitor = new TraverseVisitor() {
            @Override
            public void visitEdge(Edge edge, State state) {
            }

            @Override
            public void visitVertex(State state) {
                Vertex v = state.getVertex();

                // TODO: Check these assumptions:
                // If we encounter something from a trip pattern, it's definitely the first time we've seen it.
                // It's fine to search from vertices rather than the line, because we would need to turn from an intersection anyway.


                if (v instanceof TransitStop) {
                    TransitStop tstop = (TransitStop) v;
                    Collection<TripPattern> patterns = graph.index.patternsForStop.get(tstop.getStop());
                    for (TripPattern pattern : patterns) {
                        TransferPointAtDistance pt = new TransferPointAtDistance(hop, state, tstop);
                        closestTransferPointForTripPattern.put(pattern, pt);
                    }
                }

                // get hops
                if (state.backEdge instanceof StreetEdge) {
                    for (PatternHop h : graph.index.getHopsForEdge(state.backEdge, true)) {
                        if (h.getContinuousPickup() > 0 || h.getContinuousDropoff() > 0) {
                            TripPattern pattern = h.getPattern();
                            TransferPointAtDistance pt = new TransferPointAtDistance(hop, state, h, state.getVertex().getCoordinate());
                            closestTransferPointForTripPattern.put(pattern, pt);
                        }
                    }
                }
            }

            @Override
            public void visitEnqueue(State state) {
            }

        };

        // Remap edge to the graph to find vertices for search
        List<Edge> edges = matcher.match(hop.getGeometry());
        Set<Vertex> verts = new HashSet<>();
        for (Edge e : edges) {
            verts.add(e.getToVertex());
            verts.add(e.getFromVertex());
        }

        State[] states = verts.stream().map(v -> new State(v, request)).toArray(State[]::new);

        gd.getShortestPathTree(states);

        Set<TransferPointAtDistance> pts = new HashSet<>();

        for (Collection<TransferPointAtDistance> ptsForPattern : closestTransferPointForTripPattern.asMap().values()) {
            TransferPointAtDistance best = Collections.max(ptsForPattern, (x, y) -> y.betterThan(x, true) ? -1 : 1);
            TransferPointAtDistance rev  = Collections.max(ptsForPattern, (x, y) -> y.betterThan(x, false) ? -1 : 1);
            pts.add(best);
            pts.add(rev);
        }

        LOG.debug("for hop={} found {} transfer points", hop, pts.size());
        return pts;
    }

    private void link(Graph graph, PatternHop hop, TransferPointAtDistance point) {
        if (point.isTransitStop()) {
            // linking from a hop to a transit stop

            Vertex v = point.getFrom();
            Stop stop = new Stop();
            stop.setId(new AgencyAndId(hop.getPattern().getFeedId(), String.valueOf(Math.random())));
            stop.setLat(v.getLat());
            stop.setLon(v.getLon());

            String msg = String.format("Transfer from pattern=%s, stopIndex=%d, pos=%s to stop=%s, dist=%g, dafh=%g", hop.getPattern().code, hop.getStopIndex(), v.getCoordinate().toString(), point.tstop.toString(), point.dist, point.distanceAlongFromHop);
            stop.setName(msg);
            TransitStop transferStop = new TransitStop(graph, stop);

            // hop -> stop transfer
            PatternArriveVertex patternArriveVertex = new PatternArriveVertex(graph, hop.getPattern(), hop.getStopIndex(), stop);
            TransitStopArrive transitStopArrive = new TransitStopArrive(graph, stop, transferStop);
            PartialPatternHop.startHop(hop, patternArriveVertex, stop, matcher, geometryFactory);
            new TransitBoardAlight(patternArriveVertex, transitStopArrive, hop.getStopIndex(), hop.getPattern().mode);
            new PreAlightEdge(transitStopArrive, transferStop);
            new SimpleTransfer(transferStop, point.tstop, point.dist, point.geom, point.edges);

            // stop -> hop
            TransitStopDepart transitStopDepart = new TransitStopDepart(graph, stop, transferStop);
            PatternDepartVertex patternDepartVertex = new PatternDepartVertex(graph, hop.getPattern(), hop.getStopIndex(), stop);
            new PreBoardEdge(transferStop, transitStopDepart);
            new TransitBoardAlight(transitStopDepart, patternDepartVertex, hop.getStopIndex(), hop.getPattern().mode);
            PartialPatternHop.endHop(hop, patternDepartVertex,stop, matcher, geometryFactory);
            TransferPointAtDistance rev = point.reverse();
            new SimpleTransfer(point.tstop, transferStop, rev.dist, rev.geom, rev.edges);

        } else {
            // TODO
        }
    }

    private static boolean tooClose(Vertex v, Vertex w) {
        return SphericalDistanceLibrary.fastDistance(v.getLat(), v.getLon(),  w.getLat(), w.getLon()) < TRANSIT_STOP_CUTOFF;
    }

    private static boolean shouldExclude(PatternHop hop, TransitStop stop) {
        return tooClose(hop.getFromVertex(), stop) || tooClose(hop.getToVertex(), stop);
    }

    private static boolean shouldExclude(PatternHop hop0, PatternHop hop1) {
        Vertex v = hop0.getFromVertex(), w = hop0.getToVertex(), x = hop1.getFromVertex(), y = hop1.getToVertex();
        return tooClose(v, x) || tooClose(v, y) || tooClose(w, x) || tooClose(w, y);
    }

    private static boolean shouldExclude(PatternHop hop, TransferPointAtDistance pt) {
        return pt.isTransitStop() ? shouldExclude(hop, pt.getTransitStop()) : shouldExclude(hop, pt.getHop());
    }
}

class TransferPointAtDistance {

    State state;

    TransitStop tstop;
    double dist; // distance TO original hop
    LineString geom;
    List<Edge>  edges;
    PatternHop hop;
    Coordinate locationOnHop;
    Vertex from;

    PatternHop fromHop;
    double distanceAlongFromHop; // distance along original hop

    private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    public TransferPointAtDistance(PatternHop fromHop, State state, TransitStop tstop) {
        this(fromHop, state);
        this.tstop = tstop;
    }

    public TransferPointAtDistance(PatternHop fromHop, State state, PatternHop hop, Coordinate coord) {
        this(fromHop, state);
        this.hop = hop;
        this.locationOnHop = coord;
    }

    public boolean isTransitStop() {
        return tstop != null;
    }

    public TransitStop getTransitStop() {
        return tstop;
    }

    public PatternHop getHop() {
        return hop;
    }

    public Vertex getFrom() {
        return from;
    }

    private TransferPointAtDistance(PatternHop fromHop, State state) {
        this(state);
        this.state = state;
        this.fromHop = fromHop;
        LengthIndexedLine line = new LengthIndexedLine(fromHop.getGeometry());
        this.distanceAlongFromHop = (line.project(from.getCoordinate())/line.getEndIndex()) * fromHop.getDistance();
    }

    // TODO: merge with NearbyStopFinder.stopAtDistanceForState() (where this code was taken from)
    private TransferPointAtDistance(State state) {
        double distance = 0.0;
        GraphPath graphPath = new GraphPath(state, false);
        from = graphPath.states.getFirst().getVertex();
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        List<Edge> edges = new ArrayList<>();
        for (Edge edge : graphPath.edges) {
            if (edge instanceof StreetEdge) {
                LineString geometry = edge.getGeometry();
                if (geometry != null) {
                    if (coordinates.size() == 0) {
                        coordinates.extend(geometry.getCoordinates());
                    } else {
                        coordinates.extend(geometry.getCoordinates(), 1);
                    }
                }
                distance += edge.getDistance();
            }
            edges.add(edge);
        }
        if (coordinates.size() < 2) {   // Otherwise the walk step generator breaks.
            ArrayList<Coordinate> coordinateList = new ArrayList<Coordinate>(2);
            coordinateList.add(graphPath.states.get(1).getVertex().getCoordinate());
            State lastState = graphPath.states.getLast().getBackState();
            coordinateList.add(lastState.getVertex().getCoordinate());
            coordinates = new CoordinateArrayListSequence(coordinateList);
        }
        this.geom = geometryFactory.createLineString(new PackedCoordinateSequence.Double(coordinates.toCoordinateArray()));
        this.edges = edges;
        this.dist = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransferPointAtDistance that = (TransferPointAtDistance) o;

        if (Double.compare(that.dist, dist) != 0) return false;
        if (tstop != null ? !tstop.equals(that.tstop) : that.tstop != null) return false;
        if (hop != null ? !hop.equals(that.hop) : that.hop != null) return false;
        return locationOnHop != null ? locationOnHop.equals(that.locationOnHop) : that.locationOnHop == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = tstop != null ? tstop.hashCode() : 0;
        temp = Double.doubleToLongBits(dist);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (hop != null ? hop.hashCode() : 0);
        result = 31 * result + (locationOnHop != null ? locationOnHop.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TransferPointAtDistance[dist=" + dist + "," +
                ((tstop != null) ? ("tstop=" + tstop.toString()) : ("hop=" + hop.toString() + ",coord=" + locationOnHop.toString()))
                + "]";
    }

    public boolean betterThan(TransferPointAtDistance other, boolean preferEarlyTransfer) {
        if (other==null)
            return true;
        // prefer a transfer point that is at a real stop [unless the real stop is outside the cutoff away?]
        if (this.isTransitStop() && !other.isTransitStop())
            return true;
        if (!this.isTransitStop() && other.isTransitStop())
            return false;

        // prefer traveling a shorter distance as long as the difference is not trivial. (should this be "softer"? if your walk preferences are larger you might prefer to walk a "cutoff")
        if (other.dist - this.dist > 100)
            return true;
        if (this.dist - other.dist > 100)
            return false;

        // prefer transferring earlier in the hop (or later, since we need to do reverse hops)
        if (preferEarlyTransfer)
            return (this.distanceAlongFromHop < other.distanceAlongFromHop);
        else
            return (this.distanceAlongFromHop > other.distanceAlongFromHop);
    }

    public TransferPointAtDistance reverse() {
        return new TransferPointAtDistance(state.reverse());
    }
}
