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

import org.onebusaway.gtfs.model.AgencyAndId;
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
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
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

    public PatternBoard(TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex, 
            TripPattern pattern, int stopIndex, TraverseMode mode) {
        super(fromStopVertex, toPatternVertex, pattern);
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

    public State traverse(State state0) {
        RoutingContext rctx = state0.getContext();
        RoutingRequest options = state0.getOptions();
        if (options.isArriveBy()) {
            /* reverse traversal, not so much to do */
            // do not alight immediately when arrive-depart dwell has been eliminated
            // this affects multi-itinerary searches
            if (state0.getBackEdge() instanceof PatternAlight) {
                return null;
            }
            Trip trip = pattern.getTrip(state0.getTrip());
            EdgeNarrative en = new TransitNarrative(trip, this);
            StateEditor s1 = state0.edit(this, en);
            int type = pattern.getBoardType(stopIndex);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setTripId(null);
            s1.setLastAlightedTime(state0.getTime());
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
            long current_time = state0.getTime();
            int bestWait = -1;
            int bestPatternIndex = -1;
            AgencyAndId serviceId = getPattern().getExemplar().getServiceId();
            SD: for (ServiceDay sd : rctx.serviceDays) {
                int secondsSinceMidnight = sd.secondsSinceMidnight(current_time);
                // only check for service on days that are not in the future
                // this avoids unnecessarily examining tomorrow's services
                if (secondsSinceMidnight < 0)
                    continue;
                if (sd.serviceIdRunning(serviceId)) {
                    int patternIndex = getPattern().getNextTrip(stopIndex, secondsSinceMidnight, options);
                    if (patternIndex >= 0) {
                        Trip trip = pattern.getTrip(patternIndex);
                        // a trip was found, index is valid, wait will be non-negative
                        int wait = (int) (sd.time(pattern.getDepartureTime(stopIndex,
                                patternIndex)) - current_time);
                        if (wait < 0)
                            _log.error("negative wait time on board");
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
            int type = pattern.getBoardType(stopIndex);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setTrip(bestPatternIndex);
            s1.incrementTimeInSeconds(bestWait);
            s1.incrementNumBoardings();
            s1.setTripId(trip.getId());
            s1.setZone(getPattern().getZone(stopIndex));
            s1.setRoute(trip.getRoute().getId());

            long wait_cost = bestWait;
            if (state0.getNumBoardings() == 0) {
                wait_cost *= options.waitAtBeginningFactor;
            } else {
                wait_cost *= options.waitReluctance;
            }
            s1.incrementWeight(preferences_penalty);
            TraverseMode mode = state0.getNonTransitMode(options); // why is this method on State not RoutingRequest?
            s1.incrementWeight(wait_cost + options.getBoardCost(mode));
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
        if (rctx.opt.isArriveBy()) {
            if (! rctx.opt.getModes().get(modeMask)) {
                return Double.POSITIVE_INFINITY;
            }
            AgencyAndId serviceId = getPattern().getExemplar().getServiceId();
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
        return "PatternBoard(" + getFromVertex() + ", " + getToVertex() + ")";
    }
}
