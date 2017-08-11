/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.flex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    public TemporaryPartialPatternHop makeHopNewTo(RoutingRequest opt, State state, PatternHop hop, PatternArriveVertex to, Stop toStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new TemporaryPartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, line.getStartIndex(), line.project(to.getCoordinate()), opt.flagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewFrom(RoutingRequest opt, State state, PatternHop hop, PatternDepartVertex from, Stop fromStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new TemporaryPartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(), line.project(from.getCoordinate()), line.getEndIndex(), opt.flagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop shortenEnd(RoutingRequest opt, State state, TemporaryPartialPatternHop hop, PatternStopVertex to, Stop toStop) {
        PatternHop originalHop = hop.getOriginalHop();
        LengthIndexedLine line = new LengthIndexedLine(originalHop.getGeometry());
        double endIndex = line.project(to.getCoordinate());
        if (endIndex < hop.getStartIndex())
            return null;
        return new TemporaryPartialPatternHop(originalHop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, hop.getStartIndex(), endIndex,
                hop.getStartGeometry(), hop.getStartVehicleTime(), null, 0, opt.flagStopBufferSize);
    }

    @Override
    public void findExtraHops(RoutingRequest rr, Map<PatternHop, State> patternHopStateMap) {
        Vertex initVertex = rr.arriveBy ? rr.rctx.toVertex : rr.rctx.fromVertex;
        findFlagStopEdgesNearby(rr, initVertex, patternHopStateMap);
    }

    @Override
    public StreetVertex getLocationForTemporaryStop(State s, PatternHop hop) {
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

        return (StreetVertex) v;
    }

    @Override
    public boolean checkHopAllowsBoardAlight(State state, PatternHop hop, boolean boarding) {
        return hop.canRequestService(boarding);
    }

    /**
     * Find the street edges that were split at beginning of search by StreetSplitter, check whether they are served by flag stop routes.
     * This is duplicating the work done earlier by StreetSplitter, but want to minimize the number of changes introduced by flag stops.
     */
    private void findFlagStopEdgesNearby(RoutingRequest rr, Vertex initVertex, Map<PatternHop, State> patternHopStateMap) {
        List<StreetEdge> flagStopEdges = getClosestStreetEdges(initVertex.getCoordinate());

        for(StreetEdge streetEdge : flagStopEdges){
            State nearbyState = new State(initVertex, rr);
            nearbyState.backEdge = streetEdge;
            Collection<PatternHop> hops = graph.index.getHopsForEdge(streetEdge);
            for (PatternHop hop : hops) {
                if (checkHopAllowsBoardAlight(nearbyState, hop, !rr.arriveBy)) {
                    patternHopStateMap.put(hop, nearbyState);
                }
            }
        }
    }

    /**
     * Find the nearest street edges to the given point, check if they are served by flag stop routes.
     */
    private List<StreetEdge> getClosestStreetEdges(Coordinate pointLocation) {

        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(500);

        Envelope env = new Envelope(pointLocation);

        // local equirectangular projection
        double lat = pointLocation.getOrdinate(1);
        final double xscale = Math.cos(lat * Math.PI / 180);

        env.expandBy(radiusDeg / xscale, radiusDeg);

        Collection<Edge> edges = graph.streetIndex.getEdgesForEnvelope(env);
        if (edges.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Double, List<StreetEdge>> edgeDistanceMap = new TreeMap<>();
        for(Edge edge : edges){
            if(edge instanceof StreetEdge){
                LineString line = edge.getGeometry();
                double dist = SphericalDistanceLibrary.fastDistance(pointLocation, line);
                double roundOff = (double) Math.round(dist * 100) / 100;
                if(!edgeDistanceMap.containsKey(roundOff))
                    edgeDistanceMap.put(roundOff, new ArrayList<>());
                edgeDistanceMap.get(roundOff).add((StreetEdge) edge);
            }
        }

        List<StreetEdge> closestEdges = edgeDistanceMap.values().iterator().next();
        List<StreetEdge> ret = new ArrayList<>();
        for(StreetEdge closestEdge : closestEdges){
            Collection<PatternHop> patternHops = graph.index.getHopsForEdge(closestEdge);
            if(patternHops.size() > 0) {
                ret.add(closestEdge);
            }
        }
        return ret;
    }
}
