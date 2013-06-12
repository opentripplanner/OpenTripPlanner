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

package org.opentripplanner.updater.stoptime;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.trippattern.Update;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class GtfsRealtimeUpdate extends Update {

    private GtfsRealtimeUpdate(AgencyAndId tripId, String stopId, int stopSeq, int arrive,
            int depart, Status status, long timestamp) {
        super(tripId, stopId, stopSeq, arrive, depart, status, timestamp);
    }

    static public List<GtfsRealtimeUpdate> getUpdatesForScheduledTrip(AgencyAndId tripId,
            TripUpdate tripUpdate, long timestamp, long today) {
        List<GtfsRealtimeUpdate> updates = new LinkedList<GtfsRealtimeUpdate>();

        for (StopTimeUpdate stopTimeUpdate : tripUpdate.getStopTimeUpdateList()) {
            if (stopTimeUpdate.hasScheduleRelationship()
                    && StopTimeUpdate.ScheduleRelationship.NO_DATA == stopTimeUpdate
                            .getScheduleRelationship()) {
                GtfsRealtimeUpdate u = new GtfsRealtimeUpdate(tripId, stopTimeUpdate.getStopId(),
                        stopTimeUpdate.getStopSequence(), 0, 0, Update.Status.PLANNED, timestamp);
                updates.add(u);
                continue;
            }

            // TODO: handle cases where only the delay is provided
            long now = new Date().getTime() / 1000;
            long arrivalTime = stopTimeUpdate.getArrival().getTime();
            long departureTime = stopTimeUpdate.getDeparture().getTime();

            Update.Status status = Update.Status.PREDICTION;
            if (stopTimeUpdate.hasScheduleRelationship()
                    && StopTimeUpdate.ScheduleRelationship.SKIPPED == stopTimeUpdate
                            .getScheduleRelationship()) {
                status = Update.Status.CANCEL;
                /*
                 * } else if(arrivalTime <= now){ if( departureTime <= now) { //status =
                 * Update.Status.PASSED; arrivalTime = arrivalTime - today;; departureTime =
                 * departureTime - today; } else { status = Update.Status.ARRIVED; arrivalTime =
                 * arrivalTime - today;; departureTime = departureTime - today; }
                 */
            } else {
                arrivalTime = arrivalTime - today;
                departureTime = departureTime - today;
            }

            GtfsRealtimeUpdate u = new GtfsRealtimeUpdate(tripId, stopTimeUpdate.getStopId(),
                    stopTimeUpdate.getStopSequence(), (int) arrivalTime, (int) departureTime,
                    status, timestamp);

            updates.add(u);
        }

        return updates;
    }

    public static GtfsRealtimeUpdate getUpdateForCanceledTrip(AgencyAndId tripId, Stop stop,
            int stopSequence, long timestamp) {
        return new GtfsRealtimeUpdate(tripId, stop.getId().getId(), stopSequence, 0, 0,
                Update.Status.CANCEL, timestamp);
    }
}
