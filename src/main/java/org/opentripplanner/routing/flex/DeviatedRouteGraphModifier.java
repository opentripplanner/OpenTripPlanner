package org.opentripplanner.routing.flex;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.flex.FlexPatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStop;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Add temporary vertices/edges for deviated-route service.
 */
public class DeviatedRouteGraphModifier extends GtfsFlexGraphModifier {

    // want to ensure we only keep one pattern hop per trip pattern
    private Map<TripPattern, FlexPatternHop> directServices = Maps.newHashMap();
    private Set<State> transitStopStates = Sets.newHashSet();

    public DeviatedRouteGraphModifier(Graph graph) {
        super(graph);
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.CAR;
    }

    @Override
    public SearchTerminationStrategy getSearchTerminationStrategy() {
        return new SearchTerminationStrategy() {
            @Override
            public boolean shouldSearchTerminate(Vertex origin, Vertex target, State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
                return current.getElapsedTimeSeconds() > traverseOptions.flexMaxCallAndRideSeconds;
            }
        };
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewTo(RoutingRequest opt, State state, FlexPatternHop hop, PatternArriveVertex to, Stop toStop) {
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        double startIndex = line.getStartIndex();
        double endIndex = line.project(state.getBackEdge().getToVertex().getCoordinate());
        if (hop.getStopIndex() == 0 && tooLittleOnRoute(hop, line, startIndex, endIndex)) {
            StreetVertex toVertex = findFirstStreetVertex(opt.rctx, true);
            TemporaryTransitStop toTempStop = getTemporaryStop(toVertex, null, opt.rctx, opt);
            TransitStop fromStop = graph.index.stopVertexForStop.get(hop.getBeginStop());
            createDirectHop(opt, hop, fromStop, toTempStop, path);
            return null;
        }
        return new TemporaryPartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop,
                startIndex, endIndex, null, 0, path.getGeometry(), path.getDuration(), opt.flexFlagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewFrom(RoutingRequest opt, State state, FlexPatternHop hop, PatternDepartVertex from, Stop fromStop) {
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        // state is place where we meet line
        double startIndex = line.project(state.getBackEdge().getFromVertex().getCoordinate());
        double endIndex = line.getEndIndex();
        if (hop.getStopIndex() + 1 == hop.getPattern().getPatternHops().size() && tooLittleOnRoute(hop, line, startIndex, endIndex)) {
            StreetVertex fromVertex = findFirstStreetVertex(opt.rctx, false);
            TemporaryTransitStop fromTempStop = getTemporaryStop(fromVertex, null, opt.rctx, opt);
            TransitStop toStop = graph.index.stopVertexForStop.get(hop.getEndStop());
            createDirectHop(opt, hop, fromTempStop, toStop, path);
            return null;
        }
        return new TemporaryPartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(),
                startIndex, endIndex, path.getGeometry(), path.getDuration(), null, 0, opt.flexFlagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop shortenEnd(RoutingRequest opt, State state, TemporaryPartialPatternHop hop, PatternStopVertex to, Stop toStop) {
        FlexPatternHop originalHop = hop.getOriginalHop();
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(originalHop.getGeometry());
        double startIndex = hop.getStartIndex();
        double endIndex = line.project(state.getBackEdge().getToVertex().getCoordinate());
        if (endIndex < startIndex)
            return null;
        // we may want to create a ~direct~ hop.
        // let's say, create a direct hop if the distance we would travel on the route is < 100m. We'll do this in vertexVisitor later.
        if (tooLittleOnRoute(originalHop, line, startIndex, endIndex)) {
            return null;
        } else {
            return new TemporaryPartialPatternHop(originalHop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop,
                    startIndex, endIndex, hop.getStartGeometry(), hop.getStartVehicleTime(), path.getGeometry(), path.getDuration(), opt.flexFlagStopBufferSize);
        }
    }

    private boolean tooLittleOnRoute(FlexPatternHop originalHop, LengthIndexedLine line, double startIndex, double endIndex) {
        double onRouteDistance = SphericalDistanceLibrary.fastLength((LineString) line.extractLine(startIndex, endIndex));
        return onRouteDistance <= Math.min(100, originalHop.getDistance());
    }

    @Override
    public boolean checkHopAllowsBoardAlight(State s, FlexPatternHop hop, boolean boarding) {
        StreetVertex sv = findFirstStreetVertex(s);
        // If first vertex is not a StreetVertex, it's a transit vertex, which we'll catch later.
        if (sv == null) {
            // Do check for service
            if (!s.getOptions().arriveBy) {
                Point pt = GeometryUtils.getGeometryFactory().createPoint(s.getOptions().rctx.fromVertex.getCoordinate());
                if (addHopAsDirectService(hop, pt)) {
                    directServices.put(hop.getPattern(), hop);
                }
            }
            return false;
        }

        Point orig = GeometryUtils.getGeometryFactory().createPoint(sv.getCoordinate());
        Point dest = GeometryUtils.getGeometryFactory().createPoint(s.getVertex().getCoordinate());
        boolean ret = false;
        if (hop.hasServiceArea()) {
            if (hop.getServiceArea().contains(orig) && hop.getServiceArea().contains(dest)) {
                ret = true;
            }
        }
        if (!ret && hop.getServiceAreaRadius() > 0) {
            double distance = SphericalDistanceLibrary.distance(s.getVertex().getCoordinate(), sv.getCoordinate());
            ret = distance < hop.getServiceAreaRadius();
        }
        if (addHopAsDirectService(hop, orig)) {
            directServices.put(hop.getPattern(), hop);
        }
        return ret;
    }

    private boolean addHopAsDirectService(FlexPatternHop hop, Point orig) {
        return hop.hasServiceArea() && hop.getServiceArea().contains(orig) && directServices.get(hop.getPattern()) == null;
    }

    @Override
    public void vertexVisitor(State state) {
        if (state.getVertex() instanceof TransitStop && !(state.getVertex() instanceof TemporaryVertex)) {
            if (((TransitStop) state.getVertex()).getModes().contains(TraverseMode.BUS)) {
                transitStopStates.add(state);
            }
        }
        // Direct hop to destination if found
        boolean foundTarget = state.getVertex() == state.getOptions().rctx.toVertex;
        if (!state.getOptions().arriveBy && foundTarget) {
            transitStopStates.add(state);
        }
    }

    protected void streetSearch(RoutingRequest rr) {
        transitStopStates.clear();
        super.streetSearch(rr);
        createDirectHopsToStops(rr);
    }

    private void createDirectHopsToStops(RoutingRequest opt) {
        Collection<FlexPatternHop> services = directServices.values();
        for (State state : transitStopStates) {
            Vertex v = state.getVertex();
            Point dest = GeometryUtils.getGeometryFactory().createPoint(v.getCoordinate());
            for (FlexPatternHop hop : services) {
                if (hop.getServiceArea().contains(dest)) {
                    TransitStop fromStop, toStop;
                    if (opt.arriveBy) {
                        if (opt.rctx.toVertex instanceof TransitStop) {
                            toStop = (TransitStop) opt.rctx.toVertex;
                        } else {
                            StreetVertex toVertex = findFirstStreetVertex(opt.rctx, true);
                            toStop = getTemporaryStop(toVertex, null, opt.rctx, opt);
                        }
                        Point toPt = GeometryUtils.getGeometryFactory().createPoint(toStop.getCoordinate());
                        if (!hop.getServiceArea().contains(toPt)) {
                            continue;
                        }
                        if (!(v instanceof TransitStop)) {
                            throw new RuntimeException("Unexpected error! Only non-transit stop should be destination");
                        } else {
                            fromStop = (TransitStop) v;
                        }
                    } else {
                        if (opt.rctx.fromVertex instanceof TransitStop) {
                            fromStop = (TransitStop) opt.rctx.fromVertex;
                        } else {
                            StreetVertex fromVertex = findFirstStreetVertex(opt.rctx, false);
                            fromStop = getTemporaryStop(fromVertex, null, opt.rctx, opt);
                        }
                        Point fromPt = GeometryUtils.getGeometryFactory().createPoint(fromStop.getCoordinate());
                        if (!hop.getServiceArea().contains(fromPt)) {
                            continue;
                        }
                        if (!(v instanceof TransitStop)) {
                            if (v == state.getOptions().rctx.toVertex) {
                                StreetVertex toVertex = findFirstStreetVertex(opt.rctx, true);
                                toStop = getTemporaryStop(toVertex, null, opt.rctx, opt, false);
                            } else {
                                throw new RuntimeException("Unexpected error! Only non-transit stop should be destination");
                            }
                        } else {
                            toStop = (TransitStop) v;
                        }
                    }
                    createDirectHop(opt, hop, fromStop, toStop, new GraphPath(state, true));
                }
            }
        }
    }

    @Override
    public StreetVertex getLocationForTemporaryStop(State s, FlexPatternHop hop) {
        return findFirstStreetVertex(s);
    }

    // Return null if first vertex is not a street vertex
    private StreetVertex findFirstStreetVertex(State s) {
        return findFirstStreetVertex(s.getOptions().rctx, s.getOptions().arriveBy);
    }

    private StreetVertex findFirstStreetVertex(RoutingContext rctx, boolean reverse) {
        Vertex v = reverse ? rctx.toVertex : rctx.fromVertex;
        if (v instanceof StreetVertex) {
            return (StreetVertex) v;
        }
        return null;
    }
}
