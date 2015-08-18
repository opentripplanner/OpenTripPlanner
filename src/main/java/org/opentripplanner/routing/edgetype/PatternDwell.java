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

import java.util.Locale;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;

import com.vividsolutions.jts.geom.LineString;


/**
 *  Models waiting in a station on a vehicle.  The vehicle is not permitted to change 
 *  names during this time -- PatternInterlineDwell represents that case.
 */
public class PatternDwell extends TablePatternEdge implements OnboardEdge, DwellEdge {
    
    private static final long serialVersionUID = 1L;

    private int stopIndex;
    
    public PatternDwell(PatternArriveVertex from, PatternDepartVertex to, int stopIndex, TripPattern tripPattern) {
        super(from, to);
        this.stopIndex = stopIndex;
    }

    public String getDirection() {
        return getPattern().getDirection();
    }

    public double getDistance() {
        return 0;
    }
        
    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(getPattern().route);
    }

    public String getName() {
        return GtfsLibrary.getRouteName(getPattern().route);
    }

    @Override
    public String getName(Locale locale) {
        return this.getName();
    }

    public State traverse(State state0) {
        //int trip = state0.getTrip();
        TripTimes tripTimes = state0.getTripTimes();
        int dwellTime = tripTimes.getDwellTime(stopIndex);
        StateEditor s1 = state0.edit(this);
        s1.setBackMode(getMode());
        s1.incrementTimeInSeconds(dwellTime);
        s1.incrementWeight(dwellTime);
        return s1.makeState();
    }

    @Override
    public State optimisticTraverse(State s0) {
        int dwellTime = getPattern().scheduledTimetable.getBestDwellTime(stopIndex);
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(dwellTime);
        s1.setBackMode(getMode());
        s1.incrementWeight(dwellTime);
        return s1.makeState();
    }
    
    @Override
    public double timeLowerBound(RoutingRequest options) {
        return getPattern().scheduledTimetable.getBestDwellTime(stopIndex);
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }

    public LineString getGeometry() {
        return null;
    }

    public String toString() {
        return "PatternDwell(" + super.toString() + ")";
    }

    public void setStopIndex(int stopIndex) {
        this.stopIndex = stopIndex;
    }

    @Override
    public int getStopIndex() {
        return stopIndex;
    }
}
