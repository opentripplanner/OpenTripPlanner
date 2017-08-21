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

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

/**
 * Add temporary vertices/edges for deviated-route service.
 */
public class DeviatedRouteGraphModifier extends GtfsFlexGraphModifier {

    private static final int MAX_DRS_SEARCH_DIST = 1600 * 10; // approx 5 mile limit. Could be set by data.

    public DeviatedRouteGraphModifier(Graph graph) {
        super(graph);
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.CAR;
    }

    @Override
    public SearchTerminationStrategy getSearchTerminationStrategy() {
        return (origin, target, state, s, opt) -> {
            double distance = SphericalDistanceLibrary.distance(origin.getCoordinate(), state.getVertex().getCoordinate());
            return distance > MAX_DRS_SEARCH_DIST;
        };
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewTo(RoutingRequest opt, State state, PatternHop hop, PatternArriveVertex to, Stop toStop) {
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        double startIndex = line.getStartIndex();
        double endIndex = line.project(state.getBackEdge().getToVertex().getCoordinate());
        return new TemporaryPartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop,
                startIndex, endIndex, null, 0, path.getGeometry(), path.getDuration(), opt.flagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewFrom(RoutingRequest opt, State state, PatternHop hop, PatternDepartVertex from, Stop fromStop) {
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        // state is place where we meet line
        double startIndex = line.project(state.getBackEdge().getFromVertex().getCoordinate());
        double endIndex = line.getEndIndex();
        return new TemporaryPartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(),
                startIndex, endIndex, path.getGeometry(), path.getDuration(), null, 0, opt.flagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop shortenEnd(RoutingRequest opt, State state, TemporaryPartialPatternHop hop, PatternStopVertex to, Stop toStop) {
        PatternHop originalHop = hop.getOriginalHop();
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(originalHop.getGeometry());
        double startIndex = hop.getStartIndex();
        double endIndex = line.project(state.getBackEdge().getToVertex().getCoordinate());
        if (endIndex < startIndex)
            return null;
        // we may want to create a ~direct~ hop.
        // let's say, create a direct hop if the distance we would travel on the route is < 100m todo
        if (tooLittleOnRoute(originalHop, line, startIndex, endIndex)) {
            StreetVertex fromVertex = findFirstStreetVertex(opt.rctx, false);
            StreetVertex toVertex = findFirstStreetVertex(opt.rctx, true);
            createDirectHop(opt, originalHop, fromVertex, toVertex);
            return null;
        } else {
            return new TemporaryPartialPatternHop(originalHop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop,
                    startIndex, endIndex, hop.getStartGeometry(), hop.getStartVehicleTime(), path.getGeometry(), path.getDuration(), opt.flagStopBufferSize);
        }
    }

    private boolean tooLittleOnRoute(PatternHop originalHop, LengthIndexedLine line, double startIndex, double endIndex) {
        double onRouteDistance = SphericalDistanceLibrary.fastLength((LineString) line.extractLine(startIndex, endIndex));
        return onRouteDistance <= Math.min(100, originalHop.getDistance());
    }

    @Override
    public boolean checkHopAllowsBoardAlight(State s, PatternHop hop, boolean boarding) {
        StreetVertex sv = findFirstStreetVertex(s);
        if (hop.hasServiceArea()) {
            Point orig = GeometryUtils.getGeometryFactory().createPoint(sv.getCoordinate());
            Point dest = GeometryUtils.getGeometryFactory().createPoint(s.getVertex().getCoordinate());
            if (hop.getServiceArea().contains(orig) && hop.getServiceArea().contains(dest)) {
                return true;
            }
        }
        double distance = SphericalDistanceLibrary.distance(s.getVertex().getCoordinate(), sv.getCoordinate());
        return distance < hop.getServiceAreaRadius();
    }

    @Override
    public StreetVertex getLocationForTemporaryStop(State s, PatternHop hop) {
        return findFirstStreetVertex(s);
    }

    private StreetVertex findFirstStreetVertex(State s) {
        return findFirstStreetVertex(s.getOptions().rctx, s.getOptions().arriveBy);
    }

    private StreetVertex findFirstStreetVertex(RoutingContext rctx, boolean reverse) {
        Vertex v = reverse ? rctx.toVertex : rctx.fromVertex;
        if (!(v instanceof StreetVertex)) {
            throw new RuntimeException("Not implemented: GTFS-flex from or to a stop.");
        }
        return (StreetVertex) v;
    }
}
