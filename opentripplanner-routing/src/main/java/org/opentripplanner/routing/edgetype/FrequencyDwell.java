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
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.trippattern.FrequencyBasedTripPattern;
import org.opentripplanner.routing.vertextype.TransitVertex;

import com.vividsolutions.jts.geom.Geometry;


/**
 *  Models waiting in a station on a vehicle, for frequency-based trips
 */
public class FrequencyDwell extends AbstractEdge implements OnBoardForwardEdge, OnBoardReverseEdge, DwellEdge {
    
    private static final long serialVersionUID = 1L;

    private int stopIndex;

    private FrequencyBasedTripPattern pattern;
    
    public FrequencyDwell(TransitVertex from, TransitVertex to, int stopIndex, FrequencyBasedTripPattern pattern) {
        super(from, to);
        this.stopIndex = stopIndex;
        this.pattern = pattern;
    }

    public String getDirection() {
        return pattern.getTrip().getTripHeadsign();
    }

    public double getDistance() {
        return 0;
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(pattern.getTrip().getRoute());
    }

    public String getName() {
        return GtfsLibrary.getRouteName(pattern.getTrip().getRoute());
    }

    public State traverse(State state0) {
        int dwellTime = pattern.getDwellTime(stopIndex);
        StateEditor s1 = state0.edit(this);
        s1.incrementTimeInSeconds(dwellTime);
        s1.incrementWeight(dwellTime);
        return s1.makeState();
    }

    @Override
    public State optimisticTraverse(State s0) {
        int dwellTime = pattern.getDwellTime(stopIndex);
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(dwellTime);
        s1.incrementWeight(dwellTime);
        return s1.makeState();
    }
    
    @Override
    public double timeLowerBound(RoutingRequest options) {
        return pattern.getDwellTime(stopIndex);
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }

    public Geometry getGeometry() {
        return null;
    }

    public String toString() {
        return "PatternDwell(" + super.toString() + ")";
    }

    public void setPattern(FrequencyBasedTripPattern pattern) {
        this.pattern = pattern;
    }

    public FrequencyBasedTripPattern getPattern() {
        return pattern;
    }

    public void setStopIndex(int stopIndex) {
        this.stopIndex = stopIndex;
    }

    public int getStopIndex() {
        return stopIndex;
    }

}
