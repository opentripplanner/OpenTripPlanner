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

package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.module.map.StreetMatcher;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

import java.util.ArrayList;
import java.util.List;


public class PartialPatternHop extends PatternHop {

    private static final long serialVersionUID = 1L;

    private double startIndex;
    private double endIndex;
    private double percentageOfHop;

    private PartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, StreetMatcher matcher, GeometryFactory factory, boolean start) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), hop.getContinuousPickup(), hop.getContinuousDropoff(), false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        if (start) {
            this.startIndex = 0;
            this.endIndex = line.project(to.getCoordinate());
        } else {
            this.startIndex = line.project(from.getCoordinate());
            this.endIndex = line.getEndIndex();
        }
        this.setGeometryFromHop(matcher, factory, hop);
        this.percentageOfHop = (this.endIndex - this.startIndex) / line.getEndIndex();
    }

    // given hop s0->s1 and a temporary position t, create a partial hop s0->t
    public static PartialPatternHop startHop(PatternHop hop, PatternArriveVertex to, Stop toStop, StreetMatcher matcher, GeometryFactory factory) {
        return new PartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, matcher, factory, true);
    }

    public static PartialPatternHop endHop(PatternHop hop, PatternDepartVertex from, Stop fromStop, StreetMatcher matcher, GeometryFactory factory) {
        return new PartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(), matcher, factory, false);
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return percentageOfHop * super.timeLowerBound(options);
    }

    @Override
    public int getRunningTime(State s0) {
        return (int) (percentageOfHop * super.getRunningTime(s0));
    }

    private void setGeometryFromHop(StreetMatcher matcher, GeometryFactory factory, PatternHop hop) {
        List<Edge> edges = matcher.match(hop.getGeometry());
        List<Coordinate> coords = new ArrayList<>();
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        for (Edge e : edges) {
            double idx = line.project(e.getToVertex().getCoordinate());
            if (idx >= endIndex)
                break;
            if (idx < startIndex)
                continue;
            for (Coordinate c : e.getGeometry().getCoordinates())
                coords.add(c);
        }
        Coordinate[] arr = coords.toArray(new Coordinate[0]);
        LineString geometry = factory.createLineString(arr);
        setGeometry(geometry);
    }


}

