package org.opentripplanner.routing.flex;

import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.flex.FlexPatternHop;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add temporary vertices/edges for flag stops.
 */
public class FlagStopGraphModifier extends GtfsFlexGraphModifier {

    private static final Logger LOG = LoggerFactory.getLogger(FlagStopGraphModifier.class);

    public FlagStopGraphModifier(Graph graph) {
        super(graph);
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    @Override
    public SearchTerminationStrategy getSearchTerminationStrategy() {
        return (o, t, state, s, opt) -> state.getWalkDistance() > opt.maxWalkDistance;
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewTo(RoutingRequest opt, State state, FlexPatternHop hop, PatternArriveVertex to, Stop toStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new TemporaryPartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, line.getStartIndex(), line.project(to.getCoordinate()), opt.flexFlagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewFrom(RoutingRequest opt, State state, FlexPatternHop hop, PatternDepartVertex from, Stop fromStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new TemporaryPartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(), line.project(from.getCoordinate()), line.getEndIndex(), opt.flexFlagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop shortenEnd(RoutingRequest opt, State state, TemporaryPartialPatternHop hop, PatternStopVertex to, Stop toStop) {
        FlexPatternHop originalHop = hop.getOriginalHop();
        LengthIndexedLine line = new LengthIndexedLine(originalHop.getGeometry());
        double endIndex = line.project(to.getCoordinate());
        if (endIndex < hop.getStartIndex())
            return null;
        return new TemporaryPartialPatternHop(originalHop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, hop.getStartIndex(), endIndex,
                hop.getStartGeometry(), hop.getStartVehicleTime(), null, 0, opt.flexFlagStopBufferSize);
    }

    @Override
    public StreetVertex getLocationForTemporaryStop(State s, FlexPatternHop hop) {
        RoutingRequest rr = s.getOptions();
        Vertex initVertex = rr.arriveBy ? rr.rctx.toVertex : rr.rctx.fromVertex;

        Vertex v;
        if(s.getVertex() == initVertex){
            //the origin/destination lies along a flag stop route
            LOG.debug("the origin/destination lies along a flag stop route.");
            v = initVertex;
        } else {
            v = rr.arriveBy ? s.getBackEdge().getToVertex() : s.getBackEdge().getFromVertex();
        }

        // Ensure on line
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        double i = line.project(v.getCoordinate());
        if (i <= line.getStartIndex() || i >= line.getEndIndex()) {
            return null;
        }

        if (v instanceof StreetVertex) {
            return (StreetVertex) v;
        } else if (v instanceof TransitStop) {
            TransitStop tstop = (TransitStop) v;
            if (rr.rctx.graph.index.patternsForStop.get(tstop.getStop()).contains(hop.getPattern())) {
                LOG.debug("ignoring flag stop at existing stop");
                return null;
            }
            for (Edge e : tstop.getOutgoing()) {
                if (e instanceof StreetTransitLink) {
                    return (StreetVertex) e.getToVertex();
                }
            }
            return null;
        }
        throw new RuntimeException("Unexpected location.");
    }

    @Override
    public boolean checkHopAllowsBoardAlight(State state, FlexPatternHop hop, boolean boarding) {
        return hop.canRequestService(boarding);
    }
}
