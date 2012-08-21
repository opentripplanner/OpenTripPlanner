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

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.RouteSpec;
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
import lombok.Getter;
import com.vividsolutions.jts.geom.Geometry;


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
public class TransitBoardAlight extends PatternEdge implements OnBoardForwardEdge {

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
    
    /** look for pattern in tov (instead of fromv as is done for all other pattern edges) */
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

    public Geometry getGeometry() {
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
        TraverseMode mode = state0.getNonTransitMode(options); 

        // figure out the direction
        // it's leaving transit iff it's a boarding and is arrive by, or it's not a boarding and
        // is not arrive by
        boolean offTransit = (boarding && options.isArriveBy()) || 
                (!boarding && !options.isArriveBy()); 
        
        if (offTransit) {
            int type;
            
            /* leaving transit, not so much to do */
            // do not alight immediately when arrive-depart dwell has been eliminated
            // this affects multi-itinerary searches
            if (state0.getBackEdge() instanceof TransitBoardAlight) {
                return null;
            }
            EdgeNarrative en = new TransitNarrative(state0.getTripTimes().getTrip(), this);
            StateEditor s1 = state0.edit(this, en);
            
            if (boarding)
                type = getPattern().getBoardType(stopIndex);
            else
                type = getPattern().getAlightType(stopIndex + 1);
                
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setTripId(null);
            s1.setLastAlightedTime(state0.getTime());
            s1.setPreviousStop(fromv);
            s1.setLastPattern(this.getPattern());

            // determine the wait
            if (arrivalTimeAtStop > 0) {
                int wait = (int) Math.abs(state0.getTime() - arrivalTimeAtStop);
                
                s1.incrementTimeInSeconds(wait);
                // this should only occur at the beginning
                s1.incrementWeight(wait * options.waitAtBeginningFactor);

                s1.setInitialWaitTime(wait);

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

            return s1.makeState();
        } else {
            /* onto transit: look for a transit trip on this pattern */
            int wait, type;
            TripTimes tripTimes;
            
            if (state0.getLastPattern() == this.getPattern()) {
                return null;
            }
            if (!options.getModes().get(modeMask)) {
                return null;
            }
            /* find next boarding/alighting time
             * check lists of transit serviceIds running yesterday, today, and tomorrow (relative to
             * initial state) if this pattern's serviceId is running look for the next boarding time
             * choose the soonest boarding time among trips starting yesterday, today, or tomorrow
             */
            long current_time = state0.getTime();
            int bestWait = -1;
            TripTimes bestTripTimes = null;
            int serviceId = getPattern().getServiceId();
            for (ServiceDay sd : rctx.serviceDays) {
                int secondsSinceMidnight = sd.secondsSinceMidnight(current_time);
                // only check for service on days that are not in the future
                // this avoids unnecessarily examining tomorrow's services

                // Removed by mattwigway 2012-08-06 following discussion with novalis_dt.
                // Imagine a state at 23:59 Sunday, that should take a bus departing at 00:01
                // Monday (and coded on Monday in the GTFS); disallowing Monday's departures would
                // produce a strange plan. This proved to be a problem when reverse-optimizing
                // arrive-by trips; trips would get moved earlier for transfer purposes and then
                // the future days would not be considered.

                // We also can't break off the search after we find trips today, because imagine
                // a trip on a pattern at 25:00 today and another trip on the same pattern at
                // 00:30 tommorrow. The 00:30 trip should be taken, but if we stopped the search
                // after finding today's 25:00 trip we would never find tomorrow's 00:30 trip.
                //if (secondsSinceMidnight < 0)
                //    continue;
                if (sd.serviceIdRunning(serviceId)) {
                    
                    // make sure we search for boards on board and alights on alight
                    if (boarding)
                        tripTimes = getPattern().getNextTrip(stopIndex, 
                                secondsSinceMidnight, mode == TraverseMode.BICYCLE, options);
                    else
                        tripTimes = getPattern().getPreviousTrip(stopIndex, 
                                secondsSinceMidnight, mode == TraverseMode.BICYCLE, options);
                    
                    if (tripTimes != null) {
                        // a trip was found, index is valid, wait will be non-negative
                        // we care about departures on board and arrivals on alight
                        if (boarding)
                        	wait = (int) 
                        		(sd.time(tripTimes.getDepartureTime(stopIndex)) - current_time);
                        else
                        	wait = (int) 
                    			(current_time - sd.time(tripTimes.getArrivalTime(stopIndex)));
                        
                        if (wait < 0)
                            _log.error("negative wait time on board");
                        if (bestWait < 0 || wait < bestWait) {
                            // track the soonest departure over all relevant schedules
                            bestWait = wait;
                            bestTripTimes = tripTimes;
                        }
                    }
                }
            }
            if (bestWait < 0) {
                return null;
            }
            Trip trip = bestTripTimes.getTrip();

            /* check if route banned for this plan */
            if (options.bannedRoutes != null) {
                Route route = trip.getRoute();
                RouteSpec spec = new RouteSpec(route.getId().getAgencyId(),
                        GtfsLibrary.getRouteName(route));
                if (options.bannedRoutes.contains(spec)) {
                    return null;
                }
            }

            /* check if route is preferred for this plan */
            long preferences_penalty = 0;
            if (options.preferredRoutes != null && options.preferredRoutes.size() > 0) {
                Route route = trip.getRoute();
                RouteSpec spec = new RouteSpec(route.getId().getAgencyId(),
                        GtfsLibrary.getRouteName(route));
                if (!options.preferredRoutes.contains(spec)) {
                    preferences_penalty += options.useAnotherThanPreferredRoutesPenalty;
                }
            }

            /* check if route is unpreferred for this plan */
            if (options.unpreferredRoutes != null && options.unpreferredRoutes.size() > 0) {
                Route route = trip.getRoute();
                RouteSpec spec = new RouteSpec(route.getId().getAgencyId(),
                        GtfsLibrary.getRouteName(route));
                if (options.unpreferredRoutes.contains(spec)) {
                    preferences_penalty += options.useUnpreferredRoutesPenalty;
                }
            }

            EdgeNarrative en = new TransitNarrative(trip, this);
            StateEditor s1 = state0.edit(this, en);
            
            if (boarding)
                type = getPattern().getBoardType(stopIndex);
            else
                type = getPattern().getAlightType(stopIndex + 1);
            
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            // save the trip times to ensure that router has a consistent view of them 
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
                s1.setInitialWaitTime(bestWait);
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

    /*
     * If the main search is proceeding backward, the lower bound search is proceeding forward.
     * Check the mode or serviceIds of this pattern at board time to see whether this pattern is
     * worth exploring. If the main search is proceeding forward, board cost is added at board
     * edges. The lower bound search is proceeding backward, and if it has reached a board edge the
     * pattern was already deemed useful.
     */
    public double weightLowerBound(RoutingRequest options) {
        if ((options.isArriveBy() && boarding) || (!options.isArriveBy() && !boarding))
            return timeLowerBound(options);
        else
            return options.getBoardCostLowerBound();
    }

    
    public int getStopIndex() {
        return stopIndex;
    }

    public String toString() {
        return "TransitBoardAlight(" +
                (boarding ? "boarding " : "alighting ") +
                getFromVertex() + " to " + getToVertex() + ")";
    }
}
