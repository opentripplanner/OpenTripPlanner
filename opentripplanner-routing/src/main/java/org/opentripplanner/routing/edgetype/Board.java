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
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Models boarding a vehicle - that is to say, traveling from a station off vehicle to a station on
 * vehicle. When traversed forward, the the resultant state has the time of the next departure, in
 * addition the pattern that was boarded. When traversed backward, the result state is unchanged.
 */
public class Board extends AbstractEdge implements OnBoardForwardEdge {

    String start_id; // a street vertex's id

    String end_id; // a transit node's GTFS id

    public Hop hop;

    private boolean wheelchairAccessible;

    private String zone;

    private Trip trip;

    public static final int SECS_IN_DAY = 86400;

    private static final long serialVersionUID = 2L;

    public Board(Vertex startStation, Vertex startJourney, Hop hop, boolean wheelchairAccessible,
                 String zone, Trip trip) {
        super(startStation, startJourney);
        this.hop = hop;
        this.wheelchairAccessible = wheelchairAccessible;
        this.zone = zone;
        this.trip = trip;
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
        return "board vehicle";
    }

    public Trip getTrip() {
        return hop.getTrip();
    }

    public State traverse(State state0) {
        TraverseOptions options = state0.getOptions();
        if (options.wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        if (options.isArriveBy()) {
            // backward traversal
            StateEditor s1 = state0.edit(this);
            s1.setTripId(null);
            s1.setLastAlightedTime(state0.getTime());
            s1.setPreviousStop(fromv);
            return s1.makeState();
        } else {
            // forward traversal: find a relevant transit trip
            if (!options.getModes().contains(hop.getMode())) {
                return null;
            }
            if (options.getModes().getBicycle() && !hop.getBikesAllowed()) {
                return null;
            }
            if (options.wheelchairAccessible && !wheelchairAccessible) {
                return null;
            }

            long current_time = state0.getTime();

            /* check if this trip is running or not */
            AgencyAndId serviceId = hop.getServiceId();
            int wait = -1;
            for (ServiceDay sd : options.serviceDays) {
                int secondsSinceMidnight = sd.secondsSinceMidnight(current_time);
                // only check for service on days that are not in the future
                // this avoids unnecessarily examining tomorrow's services
                if (secondsSinceMidnight < 0)
                    continue;
                if (sd.serviceIdRunning(serviceId)) {
                    int newWait = hop.getStartStopTime().getDepartureTime() - secondsSinceMidnight;
                    if (wait < 0 || newWait < wait) {
                        wait = newWait;
                    }
                }
            }
            if (wait < 0) {
                return null;
            }

            StateEditor s1 = state0.edit(this);
            s1.incrementTimeInSeconds(wait);
            s1.incrementWeight(wait * options.waitReluctance + options.boardCost);
            s1.incrementNumBoardings();
            s1.setTripId(trip.getId());
            s1.setZone(zone);
            s1.setRoute(trip.getRoute().getId());
            return s1.makeState();
        }
    }

    @Override
    public State optimisticTraverse(State s0) {
        StateEditor s1 = s0.edit(this);
        return s1.makeState();
    }
}
