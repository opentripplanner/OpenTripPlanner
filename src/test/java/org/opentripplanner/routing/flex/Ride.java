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
package org.opentripplanner.routing.flex;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Model a transit trip so that assertions can be made against it
 *
 * Similar to DefaultFareServiceImpl.Ride - could be merged
 * Cherry-picked from MTA code
 * */
public class Ride {

    private String startZone;

    private String endZone;

    private List<String> zones = new ArrayList<>();

    private AgencyAndId route;

    private String agency;

    private long startTime;

    private long endTime;

    private Stop firstStop;

    private Stop lastStop;

    private AgencyAndId trip;

    public String getStartZone() {
        return startZone;
    }

    public String getEndZone() {
        return endZone;
    }

    public List<String> getZones() {
        return zones;
    }

    public AgencyAndId getRoute() {
        return route;
    }

    public String getAgency() {
        return agency;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Stop getFirstStop() {
        return firstStop;
    }

    public Stop getLastStop() {
        return lastStop;
    }

    public AgencyAndId getTrip() {
        return trip;
    }

    public String getFirstStopId() {
        return firstStop.getId().getId();
    }

    public String getLastStopId() {
        return lastStop.getId().getId();
    }

    public static List<Ride> createRides(GraphPath path) {
        List<Ride> rides = new ArrayList<>();
        Ride ride = null;
        for (State state : path.states) {
            Edge edge = state.getBackEdge();
            if ( ! (edge instanceof HopEdge))
                continue;
            HopEdge hEdge = (HopEdge) edge;
            if (ride == null || ! state.getRoute().equals(ride.route)) {
                ride = new Ride();
                rides.add(ride);
                ride.startZone = hEdge.getBeginStop().getZoneId();
                ride.zones.add(ride.startZone);
                ride.agency = state.getBackTrip().getRoute().getAgency().getId();
                ride.route = state.getRoute();
                ride.startTime = state.getBackState().getTimeSeconds();
                ride.firstStop = hEdge.getBeginStop();
                ride.trip = state.getTripId();
            }
            ride.lastStop = hEdge.getEndStop();
            ride.endZone  = ride.lastStop.getZoneId();
            ride.zones.add(ride.endZone);
            ride.endTime = state.getTimeSeconds();
            // in default fare service, classify rides by mode
        }
        return rides;
    }
}
