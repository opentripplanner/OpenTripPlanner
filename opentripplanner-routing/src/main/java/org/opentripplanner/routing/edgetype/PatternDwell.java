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

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class PatternDwell extends AbstractEdge {

    /**
     *  Models waiting in a station on a vehicle.  The vehicle may change 
     *  names during this time (interlining).
     */
    
    private static final long serialVersionUID = 1L;

    private TripPattern pattern;
    private int stopIndex;
    
    public PatternDwell(Vertex startJourney, Vertex endJourney, int stopIndex, TripPattern tripPattern) {
        super(startJourney, endJourney);
        this.stopIndex = stopIndex;
        this.pattern = tripPattern;
    }

    public String getDirection() {
        return pattern.exemplar.getTripHeadsign();
    }

    public double getDistance() {
        return 0;
    }

    public String getEnd() {
        return null;
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(pattern.exemplar.getRoute());
    }

    public String getStart() {
        return null;
    }

    public String getName() {
        return GtfsLibrary.getRouteName(pattern.exemplar.getRoute());
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        int dwellTime = pattern.getDwellTime(stopIndex, state0.getPattern());
        state1.incrementTimeInSeconds(dwellTime);
        return new TraverseResult(dwellTime, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        int dwellTime = pattern.getDwellTime(stopIndex, state0.getPattern());
        state1.incrementTimeInSeconds(-dwellTime);
        return new TraverseResult(dwellTime, state1);
    }

    public Geometry getGeometry() {
        return null;
    }

}
