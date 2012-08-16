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
import org.opentripplanner.routing.core.RouteSpec;
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

import com.vividsolutions.jts.geom.Geometry;

public class FrequencyAlight extends Edge  implements OnBoardReverseEdge {
    private static final long serialVersionUID = 3388162982920747289L;

    private static final Logger _log = LoggerFactory.getLogger(FrequencyAlight.class);
            
    private int stopIndex;
    private FrequencyBasedTripPattern pattern;
    private int modeMask;

    private int serviceId;


    public FrequencyAlight(TransitVertex from, TransitVertex to,
            FrequencyBasedTripPattern pattern, int stopIndex, TraverseMode mode, int serviceId) {
        super(from, to);
        this.pattern = pattern;
        this.stopIndex = stopIndex;
        this.modeMask = new TraverseModeSet(mode).getMask();
        this.serviceId = serviceId;
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

    public String getName() {
        return "leave street network for transit network";
    }

    public State traverse(State state0) {
        RoutingContext rctx = state0.getContext();
        RoutingRequest options = state0.getOptions();
        Trip trip = pattern.getTrip();

        if (options.isArriveBy()) {
            /* backward traversal: find a transit trip on this pattern */

            if (!options.getModes().get(modeMask)) {
                return null;
            }
            /* find next boarding time */
            /*
             * check lists of transit serviceIds running yesterday, today, and tomorrow (relative to
             * initial state) if this pattern's serviceId is running look for the next boarding time
             * choose the soonest boarding time among trips starting yesterday, today, or tomorrow
             */
            long currentTime = state0.getTime();
            int bestWait = -1;
            int bestPatternIndex = -1;
            TraverseMode mode = state0.getNonTransitMode(options);
            if (options.bannedTrips.contains(trip.getId())) {
                //This behaves a little differently than with ordinary trip patterns,
                //because trips don't really have strong identities in frequency-based
                //plans.  I expect that reasonable plans will still be produced, since
                //we used to use route banning and that was not so bad.
                return null;
            }

            for (ServiceDay sd : rctx.serviceDays) {
                int secondsSinceMidnight = sd.secondsSinceMidnight(currentTime);
                // only check for service on days that are not in the future
                // this avoids unnecessarily examining tomorrow's services
                if (secondsSinceMidnight < 0)
                    continue;
                if (sd.serviceIdRunning(serviceId)) {
                    int startTime = pattern.getPreviousArrivalTime(stopIndex, secondsSinceMidnight,
                            options.wheelchairAccessible, mode == TraverseMode.BICYCLE, true);
                    if (startTime >= 0) {
                        // a trip was found, wait will be non-negative
                        
                        int wait = (int) (currentTime - sd.time(startTime));
                        if (wait < 0)
                            _log.error("negative wait time on alight");
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

            StateEditor s1 = state0.edit(this);
            int type = pattern.getBoardType(stopIndex);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            //s1.setTrip(bestPatternIndex); is this necessary? (AMB)
            s1.incrementTimeInSeconds(bestWait);
            s1.incrementNumBoardings();
            s1.setTripId(trip.getId());
            s1.setZone(pattern.getZone(stopIndex));
            s1.setRoute(trip.getRoute().getId());

            long wait_cost = bestWait;
            if (state0.getNumBoardings() == 0) {
                wait_cost *= options.waitAtBeginningFactor;
            } else {
                wait_cost *= options.waitReluctance;
            }
            s1.incrementWeight(preferences_penalty);
            s1.incrementWeight(wait_cost + options.getBoardCost(mode));
            s1.setBackMode(TraverseMode.ALIGHTING);
            return s1.makeState();
        } else {
            /* forward traversal: not so much to do */
            // do not alight immediately when arrive-depart dwell has been eliminated
            // this affects multi-itinerary searches
            if (state0.getBackEdge() instanceof FrequencyAlight) {
                return null;
            }
            StateEditor s1 = state0.edit(this);
            int type = pattern.getBoardType(stopIndex);
            if (TransitUtils.handleBoardAlightType(s1, type)) {
                return null;
            }
            s1.setTripId(null);
            s1.setLastAlightedTime(state0.getTime());
            s1.setPreviousStop(fromv);
            s1.setBackMode(TraverseMode.ALIGHTING);
            return s1.makeState();
        }
    }

    public State optimisticTraverse(State state0) {
        StateEditor s1 = state0.edit(this);
        // no cost (see patternalight)
        s1.setBackMode(TraverseMode.ALIGHTING);
        return s1.makeState();
    }

    /* See weightLowerBound comment. */
    public double timeLowerBound(RoutingContext rctx) {
        if (rctx.opt.isArriveBy()) {
            if (! rctx.opt.getModes().get(modeMask)) {
                return Double.POSITIVE_INFINITY;
            }
            int serviceId = pattern.getServiceId();
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
        return "FrequencyAlight(" + getFromVertex() + ", " + getToVertex() + ")";
    }
}
