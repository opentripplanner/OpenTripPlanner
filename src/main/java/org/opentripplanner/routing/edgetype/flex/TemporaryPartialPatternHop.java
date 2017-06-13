package org.opentripplanner.routing.edgetype.flex;

import com.vividsolutions.jts.geom.GeometryFactory;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.map.StreetMatcher;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.edgetype.PartialPatternHop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

/**
 * Created by simon on 6/13/17.
 */
// this may replace TemporaryPatternHop. the problem I want to solve is multiple possible hops.
public class TemporaryPartialPatternHop extends PartialPatternHop implements TemporaryEdge {
    public TemporaryPartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, RoutingContext rctx, Type type) {
        super(hop, from, to, fromStop, toStop, rctx.graph.index.matcher, GeometryUtils.getGeometryFactory(), type);
        rctx.temporaryEdges.add(this);
    }

    // todo can this be smarter
    // start hop is a hop from the existing origin TO a new flag destination
    public static TemporaryPartialPatternHop startHop(PatternHop hop, PatternArriveVertex to, Stop toStop, RoutingContext rctx) {
        return new TemporaryPartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, rctx, Type.START);
    }

    public static TemporaryPartialPatternHop endHop(PatternHop hop, PatternDepartVertex from, Stop fromStop, RoutingContext rctx) {
        return new TemporaryPartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(), rctx, Type.END);
    }

    public TemporaryPartialPatternHop shortenEnd(PatternStopVertex to, Stop toStop, RoutingContext rctx) {
        return new TemporaryPartialPatternHop(getOriginalHop(), (PatternStopVertex) getFromVertex(), to, getBeginStop(), toStop, rctx, Type.BOTH_SIDES);
    }

    @Override
    public void dispose() {
        fromv.removeOutgoing(this);
        tov.removeIncoming(this);
    }
}
