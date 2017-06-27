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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

public class PartialPatternHop extends PatternHop {

    private static final long serialVersionUID = 1L;

    private double startIndex;
    private double endIndex;
    private double originalHopLength;
    private double percentageOfHop;
    private PatternHop originalHop;

    public PartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), hop.getContinuousStops(), false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.percentageOfHop = (this.endIndex - this.startIndex) / line.getEndIndex();
        this.originalHop = hop;
        this.originalHopLength = line.getEndIndex();
        Geometry geom = line.extractLine(startIndex, endIndex);
        if (geom instanceof LineString) { // according to the javadocs, it is.
            setGeometry((LineString) geom);
        }
    }

    // given hop s0->s1 and a temporary position t, create a partial hop s0->t
    public static PartialPatternHop startHop(PatternHop hop, PatternArriveVertex to, Stop toStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new PartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, line.getStartIndex(), line.project(to.getCoordinate()));
    }

    public static PartialPatternHop endHop(PatternHop hop, PatternDepartVertex from, Stop fromStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new PartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(), line.project(from.getCoordinate()), line.getEndIndex());
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return percentageOfHop * super.timeLowerBound(options);
    }

    @Override
    public int getRunningTime(State s0) {
        return (int) (percentageOfHop * super.getRunningTime(s0));
    }

    public boolean isOriginalHop(PatternHop hop) {
        return originalHop.getId() == hop.getId();
    }

    public PatternHop getOriginalHop() {
        return originalHop;
    }

    public double getPercentageOfHop() {
        return percentageOfHop;
    }

    public double getStartIndex() {
        return startIndex;
    }

    public double getEndIndex() {
        return endIndex;
    }

    public double getOriginalHopLength() {
        return originalHopLength;
    }


}

