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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

/** 
 * A vehicle's wait between the end of one run and the beginning of another run on the same block 
 * */
class InterlineDwellData implements Serializable {

    private static final long serialVersionUID = 1L;

    public int dwellTime;

    public int patternIndex;

    public InterlineDwellData(int dwellTime, int patternIndex) {
        this.dwellTime = dwellTime;
        this.patternIndex = patternIndex;
    }
}

public class PatternInterlineDwell extends AbstractEdge implements OnBoardForwardEdge, OnBoardReverseEdge {
    private static final Logger _log = LoggerFactory.getLogger(PatternInterlineDwell.class);

    private static final long serialVersionUID = 1L;

    private Map<AgencyAndId, InterlineDwellData> tripIdToInterlineDwellData;

    private Map<AgencyAndId, InterlineDwellData> reverseTripIdToInterlineDwellData;

    private int bestDwellTime = Integer.MAX_VALUE;
    
    private Trip targetTrip;

    public PatternInterlineDwell(Vertex startJourney, Vertex endJourney, Trip targetTrip) {
        super(startJourney, endJourney);
        this.tripIdToInterlineDwellData = new HashMap<AgencyAndId, InterlineDwellData>();
        this.reverseTripIdToInterlineDwellData = new HashMap<AgencyAndId, InterlineDwellData>();
        this.targetTrip = targetTrip;
    }

    public void addTrip(AgencyAndId trip, AgencyAndId reverseTrip, int dwellTime,
            int oldPatternIndex, int newPatternIndex) {
        if (dwellTime < 0) {
	    dwellTime = 0;
            _log.warn ("Negative dwell time for trip " + trip.getAgencyId() + " " + trip.getId() + "(forcing to zero)");
        }
        tripIdToInterlineDwellData.put(trip, new InterlineDwellData(dwellTime, newPatternIndex));
        reverseTripIdToInterlineDwellData.put(reverseTrip, new InterlineDwellData(dwellTime,
                oldPatternIndex));
        if (dwellTime < bestDwellTime) {
            bestDwellTime = dwellTime;
        }
    }

    public String getDirection() {
        return targetTrip.getTripHeadsign();
    }

    public double getDistance() {
        return 0;
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(targetTrip.getRoute());
    }

    public String getName() {
        return GtfsLibrary.getRouteName(targetTrip.getRoute());
    }

    public double optimisticTraverse(TraverseOptions options) {
        return bestDwellTime;
    }
    
    public State traverse(State state0) {

        AgencyAndId tripId = state0.getTripId();
        InterlineDwellData dwellData = tripIdToInterlineDwellData.get(tripId);
        if (dwellData == null) {
            return null;
        }
        
        StateEditor s1 = state0.edit(this);
        s1.incrementTimeInSeconds(dwellData.dwellTime);
        s1.setTripId(targetTrip.getId());
        s1.setTrip(dwellData.patternIndex);
        s1.incrementWeight(dwellData.dwellTime);
        return s1.makeState();
    }

    public State traverseBack(State state0) {

        AgencyAndId tripId = state0.getTripId();
        InterlineDwellData dwellData = reverseTripIdToInterlineDwellData.get(tripId);
        if (dwellData == null) {
            return null;
        }
        
        StateEditor s1 = state0.edit(this);
        s1.incrementTimeInSeconds(-dwellData.dwellTime);
        s1.setTripId(targetTrip.getId());
        s1.setTrip(dwellData.patternIndex);
        s1.incrementWeight(dwellData.dwellTime);
        return s1.makeState(); 
    }

    public Geometry getGeometry() {
        return null;
    }

    public String toString() {
        return "PatterninterlineDwell(" + super.toString() + ")";
    }
}
