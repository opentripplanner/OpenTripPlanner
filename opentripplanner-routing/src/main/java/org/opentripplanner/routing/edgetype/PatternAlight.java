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
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.StateData.Editor;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;


/**
 * Models alighting from a vehicle - that is to say, traveling from a station on vehicle to a
 * station off vehicle. When traversed backwards, the the resultant state has the time of the
 * previous arrival, in addition the pattern that was boarded. When traversed forwards, the
 * result state is unchanged. An boarding penalty can also be applied to discourage transfers.
 */
public class PatternAlight extends PatternEdge implements OnBoardReverseEdge {

    private static final long serialVersionUID = 1042740795612978747L;

    private static final long MILLI_IN_DAY = 24 * 60 * 60 * 1000;

    private static final int SEC_IN_DAY = 24 * 60 * 60;

    private int stopIndex;

    private int modeMask;
    
    public PatternAlight(Vertex startStation, Vertex startJourney, TripPattern pattern,
            int stopIndex, TraverseMode mode) {
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
        return TraverseMode.ALIGHTING;
    }

    public String getName() {
        return "leave transit network for street network";
    }

    @Override
    public TraverseResult traverseBack(State state0, TraverseOptions options) {
        if (!options.getModes().get(modeMask)) {
            return null;
        }
        long current_time = state0.getTime();
        long transfer_penalty = 0;
        
        StateData data = state0.getData();
        
        /* apply transfer rules */
        /* look in the global transfer table for the rules from the previous stop to
         * this stop. 
         */
        if (data.getLastAlightedTime() != 0) { /* this is a transfer rather than an initial boarding */
            TransferTable transferTable = options.getTransferTable();
            
            if (transferTable.hasPreferredTransfers()) {
                transfer_penalty = options.baseTransferPenalty;
            }
            
            int transfer_time = transferTable.getTransferTime(getToVertex(), data.getPreviousStop());
            if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
                transfer_time = options.minTransferTime;
            }
            if (transfer_time > 0 && transfer_time > (current_time + data.getLastAlightedTime()) * 1000) {
                /* minimum time transfers */
                current_time += data.getLastAlightedTime() - transfer_time * 1000;
            } else if (transfer_time == TransferTable.FORBIDDEN_TRANSFER) {
                return null;
            } else if (transfer_time == TransferTable.PREFERRED_TRANSFER) {
                /* depenalize preferred transfers */
                transfer_penalty = 0; 
            }
        }
        
        ServiceDate service_date = getServiceDate(current_time, options.calendar);
        ServiceDate service_date_testerday = getServiceDate(current_time - MILLI_IN_DAY, options.calendar);
        int secondsSinceMidnight = (int) ((current_time - service_date.getAsDate().getTime()) / 1000);

        int wait = 1;
        int patternIndex = -1;
        AgencyAndId service = pattern.getExemplar().getServiceId();
        if (options.serviceOn(service, service_date)) {
            // try to get the departure time on today's schedule
            patternIndex = pattern.getPreviousTrip(stopIndex, secondsSinceMidnight, options.wheelchairAccessible, false);
            if (patternIndex >= 0) {
                wait = pattern.getArrivalTime(stopIndex, patternIndex) - secondsSinceMidnight;
            }
        }
        if (options.serviceOn(service, service_date_testerday)) {
            // now, try to get the departure time on yesterday's schedule -- assuming that
            // yesterday's is on the same schedule as today. If it's not, then we'll worry about it
            // when we get to the pattern(s) which do contain yesterday.
            int yesterday_pattern_index = pattern.getPreviousTrip(stopIndex, secondsSinceMidnight
                    + SEC_IN_DAY, options.wheelchairAccessible, false);
            if (yesterday_pattern_index >= 0) {
                int wait_yesterday = pattern.getArrivalTime(stopIndex, yesterday_pattern_index)
                        - secondsSinceMidnight - SEC_IN_DAY;
                if (wait > 0 || wait_yesterday > wait) {
                    // choose the better time
                    wait = wait_yesterday;
                    patternIndex = yesterday_pattern_index;
                }
            }
        }

        if (wait > 0) {
            return null;
        }
        
        Trip trip = getPattern().getTrip(patternIndex);
        
        /* check if route banned for this plan */
        if (options.bannedRoutes != null) {
            Route route = trip.getRoute();
            RouteSpec spec = new RouteSpec(route.getId().getAgencyId(), GtfsLibrary.getRouteName(route));
            if (options.bannedRoutes.contains(spec)) {
                return null;
            }
        }
        
        Editor editor = state0.edit();
        editor.setTrip(patternIndex);
        editor.incrementTimeInSeconds(wait);
        editor.incrementNumBoardings();
        editor.setTripId(trip.getId());
        editor.setZone(pattern.getZone(stopIndex));
        editor.setRoute(pattern.getExemplar().getRoute().getId());
        editor.setFareContext(pattern.getFareContext());
        

        if (options.optimizeFor == OptimizeType.TRANSFERS && state0.getData().getTrip() != -1) {
            //this is not the first boarding, therefore we must have "transferred" -- whether
            //via a formal transfer or by walking.
            transfer_penalty += options.optimizeTransferPenalty;
        }
        long wait_cost = -wait;
        if (state0.getData().getNumBoardings() == 0) {
            wait_cost *= options.waitAtBeginningFactor;
        }
        else {
            wait_cost *= options.waitReluctance;
        }
        
        return new TraverseResult(wait_cost + options.boardCost + transfer_penalty, editor.createState(), this);
    }

    @Override
    public TraverseResult traverse(State state0, TraverseOptions options) {
	if (!pattern.canAlight(stopIndex + 1)) {
	    return null;
	}
        Editor s1 = state0.edit();
        s1.setTripId(null);
        s1.setLastAlightedTime(state0.getTime());
        s1.setPreviousStop(tov);
        return new TraverseResult(1, s1.createState(), this);
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
        return "PatternAlight(" + super.toString() + ")";
    }
}
