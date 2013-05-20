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

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineString;

public class FrequencyBoard extends Edge implements OnBoardForwardEdge, PatternEdge {
    private static final long serialVersionUID = 7919511656529752927L;

    private static final Logger _log = LoggerFactory.getLogger(FrequencyBoard.class);
            
    private int stopIndex;
    private FrequencyBasedTripPattern pattern;
    private int modeMask;

    private int serviceId;


    public FrequencyBoard(TransitVertex from, TransitVertex to,
            FrequencyBasedTripPattern pattern, int stopIndex, TraverseMode mode, int serviceId) {
        super(from, to);
        this.pattern = pattern;
        this.stopIndex = stopIndex;
        this.modeMask = new TraverseModeSet(mode).getMask();
        this.serviceId = serviceId;
    }

    @Override
    public Trip getTrip() {
        return pattern.getTrip();
    }

    public String getDirection() {
        return pattern.getHeadsign(stopIndex);
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        return null;
    }

    public TraverseMode getMode() {
        return TraverseMode.BOARDING;
    }

    public String getName() {
        return "leave street network for transit network";
    }

    public State traverse(State state0) {
        RoutingContext rctx = state0.getContext();
        RoutingRequest options = state0.getOptions();
        Trip trip = pattern.getTrip();

        if (options.isArriveBy()) {
            /* reverse traversal, not so much to do */
            // do not alight immediately when arrive-depart dwell has been eliminated
            // this affects multi-itinerary searches
            if (state0.getBackEdge() instanceof TransitBoardAlight && 
                    !((TransitBoardAlight) state0.getBackEdge()).isBoarding()) {
                return null;
            }
            StateEditor s1 = state0.edit(this);
            int type = pattern.getBoardType(stopIndex);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setTripId(null);
            s1.setLastAlightedTime(state0.getTimeSeconds());
            s1.setBackMode(TraverseMode.BOARDING);
            s1.setPreviousStop(fromv);
            return s1.makeState();
        } else {
            /* forward traversal: look for a transit trip on this pattern */
            if (!options.getModes().get(modeMask)) {
                return null;
            }
            /* find next boarding time */
            /*
             * check lists of transit serviceIds running yesterday, today, and tomorrow (relative to
             * initial state) if this pattern's serviceId is running look for the next boarding time
             * choose the soonest boarding time among trips starting yesterday, today, or tomorrow
             */
            long currentTime = state0.getTimeSeconds();
            int bestWait = -1;
            TraverseMode mode = state0.getNonTransitMode();
            if (options.bannedTrips.containsKey(trip.getId())) {
                //see comment in FrequencyAlight for details 
                return null;
            }
            for (ServiceDay sd : rctx.serviceDays) {
                int secondsSinceMidnight = sd.secondsSinceMidnight(currentTime);
                // only check for service on days that are not in the future
                // this avoids unnecessarily examining tomorrow's services
                if (secondsSinceMidnight < 0)
                    continue;
                if (sd.serviceIdRunning(serviceId)) {
                    int startTime = pattern.getNextDepartureTime(stopIndex, secondsSinceMidnight,
                            options.wheelchairAccessible, mode == TraverseMode.BICYCLE, true);
                    if (startTime >= 0) {
                        // a trip was found, wait will be non-negative
                        
                        int wait = (int) (sd.time(startTime) - currentTime);
                        if (wait < 0)
                            _log.error("negative wait time on board");
                        if (bestWait < 0 || wait < bestWait) {
                            // track the soonest departure over all relevant schedules
                            bestWait = wait;
                        }
                    }

                }
            }
            if (bestWait < 0) {
                return null;
            }
            
            /* check if trip is banned for this plan */
            if (options.tripIsBanned(trip))
            	return null;

            /* check if route is preferred for this plan */
            long preferences_penalty = options.preferencesPenaltyForTrip(trip);

            StateEditor s1 = state0.edit(this);
            int type = pattern.getBoardType(stopIndex);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.incrementTimeInSeconds(bestWait);
            s1.incrementNumBoardings();
            s1.setTripId(trip.getId());
            s1.setZone(pattern.getZone(stopIndex));
            s1.setRoute(trip.getRoute().getId());
            s1.setBackMode(TraverseMode.BOARDING);
            
            long wait_cost = bestWait;
            if (state0.getNumBoardings() == 0) {
                wait_cost *= options.waitAtBeginningFactor;
            } else {
                wait_cost *= options.waitReluctance;
            }
            s1.incrementWeight(preferences_penalty);
            s1.incrementWeight(wait_cost + options.getBoardCost(mode));
            return s1.makeState();
        }
    }

    public State optimisticTraverse(State state0) {
        StateEditor s1 = state0.edit(this);
        // no cost (see patternalight)
        s1.setBackMode(TraverseMode.BOARDING);
        return s1.makeState();
    }

    /* See weightLowerBound comment. */
    public double timeLowerBound(RoutingContext rctx) {
        if (rctx.opt.isArriveBy()) {
            if (! rctx.opt.getModes().get(modeMask)) {
                return Double.POSITIVE_INFINITY;
            }
            for (ServiceDay sd : rctx.serviceDays)
                if (sd.serviceIdRunning(serviceId))
                    return 0;
            return Double.POSITIVE_INFINITY;
        } else {
            return 0;
        }
    }

    /*
     * If the main search is proceeding backward, the lower bound search is proceeding forward.
     * Check the mode or serviceIds of this pattern at board time to see whether this pattern is
     * worth exploring. If the main search is proceeding forward, board cost is added at board
     * edges. The lower bound search is proceeding backward, and if it has reached a board edge the
     * pattern was already deemed useful.
     */
    public double weightLowerBound(RoutingRequest options) {
        if (options.isArriveBy())
            return timeLowerBound(options);
        else
            return options.getBoardCostLowerBound();
    }

    
    public int getStopIndex() {
        return stopIndex;
    }

    public String toString() {
        return "FrequencyBoard(" + getFromVertex() + ", " + getToVertex() + ")";
    }

    public FrequencyBasedTripPattern getPattern() {
        return pattern;
    }
}
