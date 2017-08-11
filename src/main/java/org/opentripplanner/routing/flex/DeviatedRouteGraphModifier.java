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
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import static org.opentripplanner.api.resource.GraphPathToTripPlanConverter.makeCoordinates;

/**
 * Add temporary vertices/edges for deviated-route service.
 */
public class DeviatedRouteGraphModifier extends GtfsFlexGraphModifier {

    private static final int MAX_DRS_SEARCH_DIST = 1600; // could be set by data

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
                startIndex, endIndex, null, 0, geometry(path), path.getDuration(), opt.flagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop makeHopNewFrom(RoutingRequest opt, State state, PatternHop hop, PatternDepartVertex from, Stop fromStop) {
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        // state is place where we meet line
        double startIndex = line.project(state.getBackEdge().getFromVertex().getCoordinate());
        double endIndex = line.getEndIndex();
        return new TemporaryPartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(),
                startIndex, endIndex, geometry(path), path.getDuration(), null, 0, opt.flagStopBufferSize);
    }

    @Override
    public TemporaryPartialPatternHop shortenEnd(RoutingRequest opt, State state, TemporaryPartialPatternHop hop, PatternStopVertex to, Stop toStop) {
        PatternHop originalHop = hop.getOriginalHop();
        GraphPath path = new GraphPath(state, false);
        LengthIndexedLine line = new LengthIndexedLine(originalHop.getGeometry());
        double endIndex = line.project(state.getBackEdge().getToVertex().getCoordinate());
        if (endIndex < hop.getStartIndex())
            return null;
        return new TemporaryPartialPatternHop(originalHop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop,
                hop.getStartIndex(), endIndex, hop.getStartGeometry(), hop.getStartVehicleTime(), geometry(path), path.getDuration(), opt.flagStopBufferSize);
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

    private StreetVertex findFirstStreetVertex(State state) {
        StreetVertex v = null;
        for (State s = state; s != null; s = s.getBackState()) {
            if (s.getVertex() instanceof StreetVertex && ! (s.getVertex() instanceof TemporaryStreetLocation)) {
                v = (StreetVertex) s.getVertex();
            }
        }
        return v;
    }

    private static LineString geometry(GraphPath path) {
        CoordinateArrayListSequence coordinates = makeCoordinates(path.edges.toArray(new Edge[0]));
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }


}
