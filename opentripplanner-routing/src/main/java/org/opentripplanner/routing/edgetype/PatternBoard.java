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

import java.util.Calendar;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;


/**
 * Models boarding a vehicle - that is to say, traveling from a station off vehicle to a station
 * on vehicle. When traversed forward, the the resultant state has the time of the next
 * departure, in addition the pattern that was boarded. When traversed backward, the result
 * state is unchanged. A boarding penalty can also be applied to discourage transfers.
 */
public class PatternBoard extends PatternEdge implements OnBoardForwardEdge {

    private static final long serialVersionUID = 1042740795612978747L;

    private static final long MILLI_IN_DAY = 24 * 60 * 60 * 1000;

    private static final int SEC_IN_DAY = 24 * 60 * 60;

    private int stopIndex;

    private int modeMask;

    public PatternBoard(Vertex startStation, Vertex startJourney, TripPattern pattern, int stopIndex, TraverseMode mode) {
        super(startStation, startJourney, pattern);
        this.stopIndex = stopIndex;
        this.modeMask = new TraverseModeSet(mode).getMask();
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public Geometry getGeometry() {
        return null;
    }

    public TraverseMode getMode() {
        return TraverseMode.BOARDING;
    }

    public String getName() {
        return "leave street network for transit network";
    }
    
    public TraverseResult optimisticTraverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(0);
        return new TraverseResult(0, state1, this);
    }
    
    public TraverseResult traverse(State state0, TraverseOptions options) {
        if (!options.getModes().get(modeMask)) {
            return null;
        }
        
        long current_time = state0.getTime();
        long transfer_penalty = 0;
        
        /* apply transfer rules */
        /* look in the global transfer table for the rules from the previous stop to
         * this stop. 
         */
        if (state0.lastAlightedTime != 0) { /* this is a transfer rather than an initial boarding */
            TransferTable transferTable = options.getTransferTable();
            
            if (transferTable.hasPreferredTransfers()) {
                transfer_penalty = options.baseTransferPenalty;
            }
            
            int transfer_time = transferTable.getTransferTime(state0.previousStop, getFromVertex());
            if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
                transfer_time = options.minTransferTime;
            }
            if (transfer_time > 0 && transfer_time > (current_time - state0.lastAlightedTime) * 1000) {
                /* minimum time transfers */
                current_time += state0.lastAlightedTime + transfer_time * 1000;
            } else if (transfer_time == TransferTable.FORBIDDEN_TRANSFER) {
                return null;
            } else if (transfer_time == TransferTable.PREFERRED_TRANSFER) {
                /* depenalize preferred transfers */
                transfer_penalty = 0; 
            }
        }
        
        ServiceDate service_date = getServiceDate(current_time, options.calendar);
        ServiceDate service_date_yesterday = getServiceDate(current_time - MILLI_IN_DAY, options.calendar);
        int seconds_since_midnight = (int) ((current_time - service_date.getAsDate().getTime()) / 1000);

        int wait = -1;
        int patternIndex = -1;
        AgencyAndId service = getPattern().getExemplar().getServiceId();
        if (options.serviceOn(service, service_date)) {
            // try to get the departure time on today's schedule
            patternIndex = getPattern().getNextTrip(stopIndex, seconds_since_midnight, options.wheelchairAccessible, true);
            if (patternIndex >= 0) {
                wait = getPattern().getDepartureTime(stopIndex, patternIndex) - seconds_since_midnight;
            }
        }
        if (options.serviceOn(service, service_date_yesterday)) {
            // now, try to get the departure time on yesterday's schedule -- assuming that
            // yesterday's is on the same schedule as today. If it's not, then we'll worry about it
            // when we get to the pattern(s) which do contain yesterday.
            int yesterday_pattern_index = getPattern().getNextTrip(stopIndex, seconds_since_midnight
                    + SEC_IN_DAY, options.wheelchairAccessible, true);
            if (yesterday_pattern_index >= 0) {
                int wait_yesterday = getPattern().getDepartureTime(stopIndex, yesterday_pattern_index)
                        - seconds_since_midnight - SEC_IN_DAY;
                if (wait < 0 || wait_yesterday < wait) {
                    // choose the better time
                    wait = wait_yesterday;
                    patternIndex = yesterday_pattern_index;
                }
            }
        }

        if (wait < 0) {
            return null;
        }
        State state1 = state0.clone();
        state1.setPattern(patternIndex);
        state1.incrementTimeInSeconds(wait);
        state1.numBoardings += 1;
        Trip trip = getPattern().getTrip(patternIndex);
              
        /* check if route banned for this plan */
        if (options.bannedRoutes != null) {
            Route route = trip.getRoute();
            RouteSpec spec = new RouteSpec(route.getId().getAgencyId(), GtfsLibrary.getRouteName(route));
            if (options.bannedRoutes.contains(spec)) {
                return null;
            }
        }
        
        state1.tripId = trip.getId();
        state1.setZoneAndRoute(getPattern().getZone(stopIndex), getPattern().getExemplar().getRoute().getId(), getPattern().getFareContext());
        if (options.optimizeFor == OptimizeType.TRANSFERS && state0.getTrip() != -1) {
            //this is not the first boarding, therefore we must have "transferred" -- whether
            //via a formal transfer or by walking.
            transfer_penalty += options.optimizeTransferPenalty;
        }
        long wait_cost = wait;
        if (state0.numBoardings == 0) {
            wait_cost *= options.waitAtBeginningFactor;
        }
        else {
            wait_cost *= options.waitReluctance;
        }
        
        return new TraverseResult(wait_cost + options.boardCost + transfer_penalty, state1, this);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
	if (!getPattern().canBoard(stopIndex)) {
            return null;
        }
        State s1 = state0.clone();
        s1.tripId = null;
        s1.lastAlightedTime = s1.time;
        s1.previousStop = tov;
        return new TraverseResult(1, s1, this);
    }

    private ServiceDate getServiceDate(long currentTime, Calendar c) {
        c.setTimeInMillis(currentTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return new ServiceDate(c.getTime());
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public String toString() {
        return "PatternBoard(" + super.toString() + ")";
    }
}
