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
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;


/**
 * Models boarding a vehicle - that is to say, traveling from a station off vehicle to a station
 * on vehicle. When traversed forward, the the resultant state has the time of the next
 * departure, in addition the pattern that was boarded. When traversed backward, the result
 * state is unchanged. A boarding penalty can also be applied to discourage transfers.
 */
public class PatternBoard extends PatternEdge implements OnBoardForwardEdge {

    private static final long serialVersionUID = 1042740795612978747L;

    private static final Logger _log = LoggerFactory.getLogger(PatternBoard.class);

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
        State state1 = state0.incrementTimeInSeconds(0);
        return new TraverseResult(0, state1, this);
    }
    
    public TraverseResult traverse(State state0, TraverseOptions options) {
        if (!options.getModes().get(modeMask)) {
            return null;
        }

        /* find next boarding time */
        /* 
         * check lists of transit serviceIds running yesterday, today, and tomorrow (relative to initial state)
         * if this pattern's serviceId is running look for the next boarding time
         * choose the soonest boarding time among trips starting yesterday, today, or tomorrow
         */
        long current_time = state0.getTime();
        int bestWait = -1;
        int bestPatternIndex = -1;
        AgencyAndId serviceId = getPattern().getExemplar().getServiceId();
        for (ServiceDay sd : options.serviceDays) {
            int secondsSinceMidnight = sd.secondsSinceMidnight(current_time);
            // only check for service on days that are not in the future
            // this avoids unnecessarily examining tomorrow's services
            if (secondsSinceMidnight < 0) continue; 
            if (sd.serviceIdRunning(serviceId)) {
                int patternIndex = getPattern().getNextTrip(stopIndex, secondsSinceMidnight, options.wheelchairAccessible,
                                                            options.getModes().getBicycle(), true);
                if (patternIndex >= 0) {
                    // a trip was found, index is valid, wait will be non-negative
                    int wait = (int) ((sd.time(getPattern().getDepartureTime(stopIndex, patternIndex)) - current_time) / 1000);
                    if (wait < 0) _log.error("negative wait time on board");
                    if (bestWait < 0 || wait < bestWait) {
                        // track the soonest departure over all relevant schedules
                        bestWait = wait;
                        bestPatternIndex = patternIndex;
                    }
                }
                
            }
        }
        if (bestWait < 0) {
            return null;
        }
        Trip trip = getPattern().getTrip(bestPatternIndex);

        /* check if route banned for this plan */
        if (options.bannedRoutes != null) {
            Route route = trip.getRoute();
            RouteSpec spec = new RouteSpec(route.getId().getAgencyId(), GtfsLibrary.getRouteName(route));
            if (options.bannedRoutes.contains(spec)) {
                return null;
            }
        }

        /* apply transfer rules */
        /* look in the global transfer table for the rules from the previous stop to
         * this stop. 
         */
        long transfer_penalty = 0;
        StateData data = state0.getData();
        if (data.getLastAlightedTime() != 0) { /* this is a transfer rather than an initial boarding */
            TransferTable transferTable = options.getTransferTable();
            
            if (transferTable.hasPreferredTransfers()) {
                transfer_penalty = options.baseTransferPenalty;
            }
            
            int transfer_time = transferTable.getTransferTime(data.getPreviousStop(), getFromVertex());
            if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
                transfer_time = options.minTransferTime;
            }
            if (transfer_time > 0 && transfer_time > (current_time - data.getLastAlightedTime()) * 1000) {
                /* minimum time transfers */
                current_time += data.getLastAlightedTime() + transfer_time * 1000;
            } else if (transfer_time == TransferTable.FORBIDDEN_TRANSFER) {
                return null;
            } else if (transfer_time == TransferTable.PREFERRED_TRANSFER) {
                /* depenalize preferred transfers */
                transfer_penalty = 0; 
            }
        }
        
        Editor editor = state0.edit();
        editor.setTrip(bestPatternIndex);
        editor.incrementTimeInSeconds(bestWait);
        editor.incrementNumBoardings();
        editor.setTripId(trip.getId());
        editor.setZone(getPattern().getZone(stopIndex));
        editor.setRoute(getPattern().getExemplar().getRoute().getId());
        editor.setFareContext(getPattern().getFareContext());
        
        if (options.optimizeFor == OptimizeType.TRANSFERS && state0.getData().getTrip() != -1) {
            //this is not the first boarding, therefore we must have "transferred" -- whether
            //via a formal transfer or by walking.
            transfer_penalty = options.optimizeTransferPenalty;
        }
        long wait_cost = bestWait;
        if (state0.getData().getNumBoardings() == 0) {
            wait_cost *= options.waitAtBeginningFactor;
        }
        else {
            wait_cost *= options.waitReluctance;
        }
        
        return new TraverseResult(wait_cost + options.boardCost + transfer_penalty, editor.createState(), this);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
	if (!getPattern().canBoard(stopIndex)) {
            return null;
        }
        Editor s1 = state0.edit();
        s1.setTripId(null);
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
        return "PatternBoard(" + super.toString() + ")";
    }
}
