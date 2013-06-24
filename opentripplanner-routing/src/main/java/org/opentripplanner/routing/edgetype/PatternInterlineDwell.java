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

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineString;

public class PatternInterlineDwell extends Edge implements OnBoardForwardEdge, OnBoardReverseEdge {
    private static final Logger LOG = LoggerFactory.getLogger(PatternInterlineDwell.class);

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

    public void addTrip(Trip trip, Trip reverseTrip, int dwellTime,
            int oldPatternIndex, int newPatternIndex) {
        if (dwellTime < 0) {
	    dwellTime = 0;
            LOG.warn ("Negative dwell time for trip " + trip.getId().getAgencyId() + " " + trip.getId().getId() + "(forcing to zero)");
        }
        tripIdToInterlineDwellData.put(trip.getId(), new InterlineDwellData(dwellTime, newPatternIndex, reverseTrip));
        reverseTripIdToInterlineDwellData.put(reverseTrip.getId(), new InterlineDwellData(dwellTime,
                oldPatternIndex, trip));
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

    public State optimisticTraverse(State s0) {
    	StateEditor s1 = s0.edit(this);
    	s1.incrementTimeInSeconds(bestDwellTime);
        return s1.makeState();
    }
    
    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }
    
    @Override
    public double timeLowerBound(RoutingRequest options) {
        return bestDwellTime;
    }

    public State traverse(State state0) {
        RoutingRequest options = state0.getOptions();

        AgencyAndId tripId = state0.getTripId();
        InterlineDwellData dwellData;
        if (options.isArriveBy()) {
        	// traversing backward
        	dwellData = reverseTripIdToInterlineDwellData.get(tripId);
        } else {
        	// traversing forward
        	dwellData = tripIdToInterlineDwellData.get(tripId);
        }
        if (dwellData == null) {
            return null;
        }
        BannedStopSet banned = options.bannedTrips.get(dwellData.trip.getId());
        if (banned != null) {
            if (banned.contains(0)) 
                return null;
        }

        StateEditor s1 = state0.edit(this);
        // FIXME: ugly!
        TableTripPattern pattern = ((OnboardVertex)s1.getVertex()).getTripPattern();
        s1.incrementTimeInSeconds(dwellData.dwellTime);
        s1.setTripId(dwellData.trip.getId());
        s1.setPreviousTrip(dwellData.trip);
        // FIXME: this is interlining to the SCHEDULED timetable, not the updated timetable. use resolver.
        s1.setTripTimes(pattern.getTripTimes(dwellData.patternIndex));
        s1.incrementWeight(dwellData.dwellTime);
        
        // This shouldn't be changing - MWC
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    public LineString getGeometry() {
        return null;
    }

    public String toString() {
        return "PatternInterlineDwell(" + super.toString() + ")";
    }
    
    public Trip getTrip() {
    	return targetTrip;
    }

    public Map<AgencyAndId, InterlineDwellData> getReverseTripIdToInterlineDwellData() {
        return reverseTripIdToInterlineDwellData;
    }
    public Map<AgencyAndId, InterlineDwellData> getTripIdToInterlineDwellData() {
        return tripIdToInterlineDwellData;
    }

    @Override
    public int getStopIndex() {
        return -1; //special case.
    }

}
