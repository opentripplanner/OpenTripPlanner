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
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineString;

/**
 * A PatternInterlineDwell refers to "interlining", where a single physical vehicle carries out several logical trips
 * and a passenger is optionally allowed to remain on the vehicle as it passes from one trip to the other. Interlining
 * is inferred from block IDs in GTFS input. We do not represent cases where the user is not allowed to remain on the
 * vehicle, as these are only interesting from an operations perspective and not for passenger information.
 *
 * Interline dwell info could be stored in various places, with various advantages and disadvantages:
 *
 * A. In the dwell edge itself (where it is now located). This will get in the way of realtime updates and the
 *    eventual elimination of transit edges.
 * B. In TripTimes. This has the disadvantage of leaving null fields in every TripTimes where interlining does not
 *    happen, but the null field could be a pointer to a compound type so it doesn't waste too much space.
 * C. In Timetables. Like TripTimes, this option also allows for real-time updates to interlining information.
 * D. TripPatterns. This does not allow full real-time updates to block and interlining behavior.
 *
 * Another option is to store the Trip->Trip mapping at the Graph level, and use the source and target vertices of the
 * interline dwell edges to know which pattern should be used for resolving TripTimes. However, this will get in the
 * way of eventually eliminating transit edges. This could be a Guava BiMap<Trip, Trip>.
 *
 * Links to previous and next trips could be stored directly as fields in TripTimes, or in a map.
 * Previous/next TripTimes in fields will not work because real-time updates to any one TripTimes will
 * "infect" all other TripTimes in its block.
 * Previous/next (Pattern, Trip) tuples in fields will work because the realtime lookup can be performed on the fly.
 * In maps, the trips do not need to be grouped by service ID because each trip exists on only a single service ID.
 *
 * The "previous" and "next" targets in interline dwell information could be:
 * A. TripTimes. This requires refreshing the TripTimes pointers when committing realtime updates to ensure that they
 *    do not point to scheduled or outdated TripTimes. However, TripTimes do not currently store which TripPattern
 *    they belong to, which could interfere with realtime lookup.
 * B. Tuples of (TripPattern, Trip). This requires resolving the actual TripTimes object at runtime in case there are
 *    real-time updates.
 *
 * Storing pointers directly to other TripTimes in prev/next fields in TripTimes prevents those TripTimes from being
 * independent of one another. An update to one TripTimes will "infect" the entire block it belongs to because the
 * prev/next links are bidirectional.
 *
 */
public class PatternInterlineDwell extends Edge implements OnboardEdge {

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
        int arrivalTime;
        int departureTime;
        TripPattern pattern;
        TripTimes newTripTimes;
        TripTimes oldTripTimes = state0.getTripTimes();
        RoutingRequest options = state0.getOptions();

        AgencyAndId tripId = state0.getTripId();
        InterlineDwellData dwellData;

        if (options.isArriveBy()) {
            // traversing backward
            dwellData = reverseTripIdToInterlineDwellData.get(tripId);
            if (dwellData == null) return null;

            pattern = ((OnboardVertex) fromv).getTripPattern();
            newTripTimes = pattern.getResolvedTripTimes(dwellData.patternIndex, state0);
            arrivalTime = newTripTimes.getArrivalTime(newTripTimes.getNumStops());
            departureTime = oldTripTimes.getDepartureTime(0);
        } else {
            // traversing forward
            dwellData = tripIdToInterlineDwellData.get(tripId);
            if (dwellData == null) return null;

            pattern = ((OnboardVertex) tov).getTripPattern();
            newTripTimes = pattern.getResolvedTripTimes(dwellData.patternIndex, state0);
            arrivalTime = oldTripTimes.getArrivalTime(oldTripTimes.getNumStops());
            departureTime = newTripTimes.getDepartureTime(0);
        }

        BannedStopSet banned = options.bannedTrips.get(dwellData.trip.getId());
        if (banned != null) {
            if (banned.contains(0)) 
                return null;
        }

        int dwellTime = departureTime - arrivalTime;
        if (dwellTime < 0) return null;

        StateEditor s1 = state0.edit(this);

        s1.incrementTimeInSeconds(dwellTime);
        s1.setTripId(dwellData.trip.getId());
        s1.setPreviousTrip(dwellData.trip);

        s1.setTripTimes(newTripTimes);
        s1.incrementWeight(dwellTime);
        
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
