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
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineString;

import lombok.Getter;


/**
 * Models boarding or alighting a vehicle - that is to say, traveling from a state off 
 * vehicle to a state on vehicle. When traversed forward on a boarding or backwards on an 
 * alighting, the the resultant state has the time of the next departure, in addition the pattern
 * that was boarded. When traversed backward on a boarding or forward on an alighting, the result
 * state is unchanged. A boarding penalty can also be applied to discourage transfers. In an on
 * the fly reverse-optimization search, the overloaded traverse method can be used to give an
 * initial wait time. Also, in reverse-opimization, board costs are correctly applied.
 * 
 * This is the result of combining the classes formerly known as PatternBoard and PatternAlight.
 * 
 * @author mattwigway
 */
public class TransitBoardAlight extends TablePatternEdge implements OnBoardForwardEdge {

    private static final long serialVersionUID = 1042740795612978747L;

    private static final Logger _log = LoggerFactory.getLogger(TransitBoardAlight.class);

    private int stopIndex;

    private int modeMask;
   
    @Getter
    private boolean boarding;

    public TransitBoardAlight (TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex, 
            int stopIndex, TraverseMode mode) {
        super(fromStopVertex, toPatternVertex);
        this.stopIndex = stopIndex;
        this.modeMask = new TraverseModeSet(mode).getMask();
        this.boarding = true;
    }
    
    public TransitBoardAlight (PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
            int stopIndex, TraverseMode mode) {
        super(fromPatternStop, toStationVertex);
        this.stopIndex = stopIndex;
        this.modeMask = new TraverseModeSet(mode).getMask();
        this.boarding = false;
    }
    
    /** 
     * Find the TableTripPattern this edge is boarding or alighting from. Overrides the general
     * method which always looks at the from-vertex.
     * @return the pattern of the to-vertex when boarding, and that of the from-vertex 
     * when alighting. 
     */
    @Override 
    public TableTripPattern getPattern() {
        if (boarding)
            return ((PatternStopVertex) tov).getTripPattern();
        else
            return ((PatternStopVertex) fromv).getTripPattern();
    }
                           
    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        return null;
    }

    public TraverseMode getMode() {
        return boarding ? TraverseMode.BOARDING : TraverseMode.ALIGHTING;
    }

    public String getName() {
        return boarding ? "leave street network for transit network" : 
            "leave transit network for street network";
    }

    @Override
    public State traverse(State state0) {
        return traverse(state0, 0);
    }

    public State traverse(State state0, long arrivalTimeAtStop) {
        RoutingContext rctx = state0.getContext();
        RoutingRequest options = state0.getOptions();
        // this method is on State not RoutingRequest because we care whether the user is in
        // possession of a rented bike.
        TraverseMode mode = state0.getNonTransitMode();

        // Determine whether we are going onto or off of transit.
        // We are leaving transit iff the edge is a boarding and the search is arrive-by, 
        // or the edge is not a boarding and the search is not arrive-by.
        boolean offTransit = (boarding && options.isArriveBy()) || 
                (!boarding && !options.isArriveBy()); 
        
        if (offTransit) { 
            /* We are leaving transit, not as much to do. */
            // do not alight immediately when arrive-depart dwell has been eliminated
            // this affects multi-itinerary searches (should be handled by PathParser)
            if (state0.getBackEdge() instanceof TransitBoardAlight) {
                return null;
            }
            StateEditor s1 = state0.edit(this);
            
            int type;            
            if (boarding)
                type = getPattern().getBoardType(stopIndex);
            else
                type = getPattern().getAlightType(stopIndex + 1);
                
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setTripId(null);
            s1.setLastAlightedTimeSeconds(state0.getTimeSeconds());
            s1.setPreviousStop(fromv);
            s1.setLastPattern(this.getPattern());

            // determine the wait
            if (arrivalTimeAtStop > 0) {
                int wait = (int) Math.abs(state0.getTimeSeconds() - arrivalTimeAtStop);
                
                s1.incrementTimeInSeconds(wait);
                // this should only occur at the beginning
                s1.incrementWeight(wait * options.waitAtBeginningFactor);

                s1.setInitialWaitTimeSeconds(wait);

                //_log.debug("Initial wait time set to {} in PatternBoard", wait);
            }
            
            // during reverse optimization, board costs should be applied to PatternBoards
            // so that comparable trip plans result (comparable to non-optimized plans)
            if (options.isReverseOptimizing())
                s1.incrementWeight(options.getBoardCost(mode));

            if (options.isReverseOptimizeOnTheFly()) {
                int thisDeparture = state0.getTripTimes().getDepartureTime(stopIndex);
                int numTrips = getPattern().getNumScheduledTrips(); 
                int nextDeparture;

                s1.setLastNextArrivalDelta(Integer.MAX_VALUE);

                for (int tripIndex = 0; tripIndex < numTrips; tripIndex++) {
                    nextDeparture = getPattern().getDepartureTime(stopIndex, tripIndex);
        
                    if (nextDeparture > thisDeparture) {
                        s1.setLastNextArrivalDelta(nextDeparture - thisDeparture);
                        break;
                    }
                }
            }            

            s1.setBackMode(getMode());
            return s1.makeState();
        } else { 
            /* We are going onto transit and must look for a suitable transit trip on this pattern. */            
            if (state0.getLastPattern() == this.getPattern()) {
                return null; // to disallow ever re-boarding the same trip pattern
            }
            if (!options.getModes().get(modeMask)) {
                return null;
            }
            // TODO: assuming all trips within a pattern have the same route and agency,
            // we could check route and agency up front ("is pattern suitable") up front, rather than
            // below after the trip search.
            
            /* Find the next boarding/alighting time relative to the current State.
             * Check lists of transit serviceIds running yesterday, today, and tomorrow 
             * (relative to the initial state). If this pattern's serviceId is running, look for 
             * the closest boarding/alighting time. Choose the closest board/alight time 
             * among trips starting yesterday, today, or tomorrow.
             * Note that we cannot skip searching on service days that have not started yet:
             * Imagine a state at 23:59 Sunday, that should take a bus departing at 00:01
             * Monday (and coded on Monday in the GTFS); disallowing Monday's departures would
             * produce a strange plan. This proved to be a problem when reverse-optimizing
             * arrive-by trips; trips would get moved earlier for transfer purposes and then
             * the future days would not be considered.
             * We also can't break off the search after we find trips today. Imagine 
             * a trip on a pattern at 25:00 today and another trip on the same pattern at
             * 00:30 tommorrow. The 00:30 trip should be taken, but if we stopped the search
             * after finding today's 25:00 trip we would never find tomorrow's 00:30 trip. */
            long current_time = state0.getTimeSeconds();
            int bestWait = -1;
            TripTimes bestTripTimes = null;
            int serviceId = getPattern().getServiceId();
            TripTimes tripTimes;
            // this method is on State not RoutingRequest because we care whether the user is in
            // possession of a rented bike.
            ServiceDay serviceDay = null;
            for (ServiceDay sd : rctx.serviceDays) {
                int wait;
                int secondsSinceMidnight = sd.secondsSinceMidnight(current_time);
                if (sd.serviceIdRunning(serviceId)) {
                    // getNextTrip will find next or prev departure depending on final boolean parameter
                    tripTimes = getPattern().getNextTrip(stopIndex, secondsSinceMidnight, 
                            mode == TraverseMode.BICYCLE, options, boarding);
                    if (tripTimes != null) {
                        wait = boarding ? // we care about departures on board and arrivals on alight
                            (int)(sd.time(tripTimes.getDepartureTime(stopIndex)) - current_time):
                            (int)(current_time - sd.time(tripTimes.getArrivalTime(stopIndex)));
                        // a trip was found and the index is valid, so the wait should be non-negative
                        if (wait < 0)
                            _log.error("negative wait time on board");
                        if (bestWait < 0 || wait < bestWait) {
                            // track the soonest departure over all relevant schedules
                            bestWait = wait;
                            serviceDay = sd;
                            bestTripTimes = tripTimes;
                        }
                    }
                }
            }
            if (bestWait < 0) { 
                return null; // no appropriate trip was found
            }
            Trip trip = bestTripTimes.getTrip();
            
            /* check if route and/or Agency are banned for this plan */
            if (options.tripIsBanned(trip))
            	return null;

            /* check if route is preferred for this plan */
            long preferences_penalty = options.preferencesPenaltyForTrip(trip);

            StateEditor s1 = state0.edit(this);
            s1.setBackMode(getMode());
            int type;
            if (boarding)
                type = getPattern().getBoardType(stopIndex);
            else
                type = getPattern().getAlightType(stopIndex + 1);
            // check: isn't this now handled inside the trip search? (AMB)
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setServiceDay(serviceDay);
            // save the trip times to ensure that router has a consistent view 
            // and constant-time access to them 
            s1.setTripTimes(bestTripTimes);
            s1.incrementTimeInSeconds(bestWait);
            s1.incrementNumBoardings();
            s1.setTripId(trip.getId());
            s1.setZone(getPattern().getZone(stopIndex));
            s1.setRoute(trip.getRoute().getId());

            double wait_cost = bestWait;

            if (state0.getNumBoardings() == 0 && !options.isReverseOptimizing()) {
                wait_cost *= options.waitAtBeginningFactor;
                // this is subtracted out in Analyst searches in lieu of reverse optimization
                s1.setInitialWaitTimeSeconds(bestWait);
            } else {
                wait_cost *= options.waitReluctance;
            }
            
            s1.incrementWeight(preferences_penalty);

            // when reverse optimizing, the board cost needs to be applied on
            // alight to prevent state domination due to free alights
            if (options.isReverseOptimizing())
                s1.incrementWeight(wait_cost);
            else
                s1.incrementWeight(wait_cost + options.getBoardCost(mode));

            // On-the-fly reverse optimization
            // determine if this needs to be reverse-optimized.
            // The last alight can be moved forward by bestWait (but no further) without
            // impacting the possibility of this trip
            if (options.isReverseOptimizeOnTheFly() && !options.isReverseOptimizing() && 
                    state0.getNumBoardings() > 0 && state0.getLastNextArrivalDelta() <= bestWait &&
                    state0.getLastNextArrivalDelta() > -1) {

                // it is re-reversed by optimize, so this still yields a forward tree
                State optimized = s1.makeState().optimizeOrReverse(true, true);
                if (optimized == null)
                    _log.error("Null optimized state. This shouldn't happen");
                return optimized;
            }
            
            // if we didn't return an optimized path, return an unoptimized one
            return s1.makeState();
        }
    }

    public State optimisticTraverse(State state0) {
        StateEditor s1 = state0.edit(this);
        // no cost (see patternalight)
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    /* See weightLowerBound comment. */
    public double timeLowerBound(RoutingContext rctx) {
        if ((rctx.opt.isArriveBy() && boarding) || (!rctx.opt.isArriveBy() && !boarding)) {
            if (!rctx.opt.getModes().get(modeMask)) {
                return Double.POSITIVE_INFINITY;
            }
            int serviceId = getPattern().getServiceId();
            for (ServiceDay sd : rctx.serviceDays)
                if (sd.serviceIdRunning(serviceId))
                    return 0;
            return Double.POSITIVE_INFINITY;
        } else {
            return 0;
        }
    }

    /* If the main search is proceeding backward, the lower bound search is proceeding forward.
     * Check the mode or serviceIds of this pattern at board time to see whether this pattern is
     * worth exploring. If the main search is proceeding forward, board cost is added at board
     * edges. The lower bound search is proceeding backward, and if it has reached a board edge the
     * pattern was already deemed useful. */
    public double weightLowerBound(RoutingRequest options) {
        // return 0; // for testing/comparison, since 0 is always a valid heuristic value
        if ((options.isArriveBy() && boarding) || (!options.isArriveBy() && !boarding))
            return timeLowerBound(options);
        else
            return options.getBoardCostLowerBound();
    }

    @Override
    public int getStopIndex() {
        return stopIndex;
    }

    public String toString() {
        return "TransitBoardAlight(" +
                (boarding ? "boarding " : "alighting ") +
                getFromVertex() + " to " + getToVertex() + ")";
    }

}
