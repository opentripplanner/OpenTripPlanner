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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Models alighting from a vehicle - that is to say, traveling from a station on vehicle to a
 * station off vehicle. When traversed backwards, the the resultant state has the time of the
 * previous arrival, in addition the pattern that was boarded. When traversed forwards, the result
 * state is unchanged. An boarding penalty can also be applied to discourage transfers.
 */
public class PatternAlight extends PatternEdge implements OnBoardReverseEdge {

    private static final long serialVersionUID = 1042740795612978747L;

    private static final Logger _log = LoggerFactory.getLogger(PatternBoard.class);

    private int stopIndex;

    private int modeMask;

    public PatternAlight(PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
            TableTripPattern pattern, int stopIndex, TraverseMode mode) {
        super(fromPatternStop, toStationVertex);
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
    public State traverse(State state0) {
        RoutingContext rctx = state0.getContext();
        RoutingRequest options = state0.getOptions();
        if (options.isArriveBy()) {
            /* backward traversal: find a transit trip on this pattern */
            if (state0.getLastPattern() == this.getPattern()) {
                return null;
            }
            if (!options.getModes().get(modeMask)) {
                return null;
            }
            /* find closest alighting time for backward searches */
            /*
             * check lists of transit serviceIds running yesterday, today, and tomorrow (relative to
             * initial state) if this pattern's serviceId is running look for the closest alighting
             * time at this stop. choose the soonest alighting time among trips starting yesterday,
             * today, or tomorrow
             */
            long current_time = state0.getTime();
            int bestWait = -1;
            TripTimes bestTripTimes = null;
            int serviceId = getPattern().getServiceId();
            // get the non-transit mode, mostly to determine whether the user is carrying a bike
            // this should maybe be done differently (using mode only for traversal permissions).
            TraverseMode nonTransitMode = state0.getNonTransitMode(options);
            for (ServiceDay sd : rctx.serviceDays) {
                int secondsSinceMidnight = sd.secondsSinceMidnight(current_time);
                // only check for service on days that are not in the future
                // this avoids unnecessarily examining trips starting tomorrow
                if (secondsSinceMidnight < 0)
                    continue;
                if (sd.serviceIdRunning(serviceId)) {
                    TripTimes tripTimes = getPattern().getPreviousTrip(stopIndex, 
                            secondsSinceMidnight, nonTransitMode == TraverseMode.BICYCLE, options);
                    if (tripTimes != null) {
                        // a trip was found, index is valid, wait will be defined.
                        // even though we are going backward I tend to think waiting
                        // should be expressed as non-negative.
                        int wait = (int) (current_time - sd.time(tripTimes.getArrivalTime(stopIndex)));
                        if (wait < 0)
                            _log.error("negative wait time on alight");
                        if (bestWait < 0 || wait < bestWait) {
                            // track the soonest arrival over all relevant schedules
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
            int type = getPattern().getAlightType(stopIndex + 1);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setTripTimes(bestTripTimes);
            s1.incrementTimeInSeconds(bestWait);
            s1.incrementNumBoardings();
            s1.setTripId(trip.getId());
            s1.setZone(getPattern().getZone(stopIndex + 1));
            s1.setRoute(trip.getRoute().getId());

            long wait_cost = bestWait;

            if (state0.getNumBoardings() == 0) {
                wait_cost *= options.waitAtBeginningFactor;
                s1.setInitialWaitTime(bestWait);
            } else {
                wait_cost *= options.waitReluctance;
            }
            s1.incrementWeight(preferences_penalty);
            s1.incrementWeight(wait_cost + options.getBoardCost(nonTransitMode));

            // On-the-fly reverse optimization
            // determine if this needs to be reverse-optimized.
            // The last alight can be moved forward by bestWait (but no further) without
            // impacting the possibility of this trip
            if (options.isReverseOptimizeOnTheFly() && !options.isReverseOptimizing() && 
                    state0.getNumBoardings() > 0 && state0.getLastNextArrivalDelta() <= bestWait) {

                // it is re-reversed by optimize, so this still yields a forward tree
                State optimized = s1.makeState().optimizeOrReverse(true, true);
                if (optimized == null)
                    _log.error("Null optimized state. This shouldn't happen");
                return optimized;
            }

            return s1.makeState();
        } else {
            /* forward traversal: not so much to do */
            // do not alight immediately when arrive-depart dwell has been eliminated
            // this affects multi-itinerary searches
            if (state0.getBackEdge() instanceof PatternBoard) {
                return null;
            }
            EdgeNarrative en = new TransitNarrative(state0.getTripTimes().trip, this);
            StateEditor s1 = state0.edit(this, en);
            int type = getPattern().getAlightType(stopIndex + 1);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }

            s1.setTripId(null);
            s1.setLastAlightedTime(state0.getTime());
            s1.setPreviousStop(tov);
            s1.setLastPattern(this.getPattern());

            // calculate the next possible arrival time, if necessary
            if (options.isReverseOptimizeOnTheFly()) {
                int thisArrival = state0.getTripTimes().getArrivalTime(stopIndex - 1);
                int numTrips = getPattern().getNumTrips(); 
                int nextArrival;

                s1.setLastNextArrivalDelta(Integer.MAX_VALUE);

                for (int tripIndex = 0; tripIndex < numTrips; tripIndex++) {
                    nextArrival = getPattern().getArrivalTime(stopIndex - 1, tripIndex);
        
                    if (nextArrival > thisArrival) {
                        s1.setLastNextArrivalDelta(nextArrival - thisArrival);
                        break;
                    }
                }
            }

            return s1.makeState();
        }
    }

    public State optimisticTraverse(State state0) {
        StateEditor s1 = state0.edit(this);
        // following line will work only for arriveby searches
        // it produces inadmissible heuristics for depart-after searches
        // s1.incrementWeight(state0.getOptions().boardCost);
        return s1.makeState();
    }

    /* See comment at weightLowerBound. */
    public double timeLowerBound(RoutingContext rctx) {
        if (rctx.opt.isArriveBy())
            return 0;
        else if (! rctx.opt.getModes().get(modeMask)) {
            return Double.POSITIVE_INFINITY;
        }
        int serviceId = getPattern().getServiceId();
        for (ServiceDay sd : rctx.serviceDays)
            if (sd.serviceIdRunning(serviceId))
                return 0;
        return Double.POSITIVE_INFINITY;
    }

    /*
     * If the main search is proceeding backward, board cost is added at alight edges. The lower
     * bound search is proceeding forward and if it has reached an alight edge the pattern was
     * already deemed useful at board time. If the main search is proceeding forward, the lower
     * bound search is proceeding backward, Check the mode or serviceIds of this pattern at board
     * time to see whether this pattern is worth exploring.
     */
    public double weightLowerBound(RoutingRequest options) {
        if (options.isArriveBy())
            return options.getBoardCostLowerBound();
        else
            return timeLowerBound(options);
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public String toString() {
        return "PatternAlight(" + getFromVertex() + ", " + getToVertex() + ")";
    }
}
