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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import lombok.Setter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.trippattern.TripUpdate;
import org.opentripplanner.routing.trippattern.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public abstract class GtfsRealtimeAbstractUpdateStreamer implements UpdateStreamer {

    private static final Logger LOG = LoggerFactory
            .getLogger(GtfsRealtimeAbstractUpdateStreamer.class);

    private static final SimpleDateFormat ymdParser = new SimpleDateFormat("yyyyMMdd");
    {
        ymdParser.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    @Setter
    private String defaultAgencyId;

    protected abstract GtfsRealtime.FeedMessage getFeedMessage();

    @Override
    public List<org.opentripplanner.routing.trippattern.TripUpdate> getUpdates() {
        GtfsRealtime.FeedMessage feed = getFeedMessage();
        if (feed == null)
            return null;

        GtfsRealtime.FeedHeader header = feed.getHeader();
        long timestamp = header.getTimestamp();
        List<org.opentripplanner.routing.trippattern.TripUpdate> updates = new ArrayList<org.opentripplanner.routing.trippattern.TripUpdate>();
        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) {
                continue;
            }

            GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
            GtfsRealtime.TripDescriptor descriptor = tripUpdate.getTrip();
            String trip = descriptor.getTripId();
            AgencyAndId tripId = new AgencyAndId(defaultAgencyId, trip);

            ServiceDate serviceDate = new ServiceDate();
            if (descriptor.hasStartDate()) {
                try {
                    Date date = ymdParser.parse(descriptor.getStartDate());
                    serviceDate = new ServiceDate(date);
                } catch (ParseException e) {
                    LOG.warn("Failed to parse startDate in gtfs-rt feed: ", e);
                }
            }

            GtfsRealtime.TripDescriptor.ScheduleRelationship sr;
            if (tripUpdate.getTrip().hasScheduleRelationship()) {
                sr = tripUpdate.getTrip().getScheduleRelationship();
            } else {
                sr = GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
            }

            switch (sr) {
            case SCHEDULED:
                updates.add(getUpdateForScheduledTrip(tripId, tripUpdate, timestamp, serviceDate));
                break;
            case CANCELED:
                updates.add(getUpdateForCanceledTrip(tripId, timestamp, serviceDate));
                break;
            case ADDED:
                updates.add(getUpdateForAddedTrip(tripId, tripUpdate, timestamp, serviceDate));
                break;
            case UNSCHEDULED:
                LOG.warn("ScheduleRelationship.UNSCHEDULED trips are currently not handled.");
                break;
            case REPLACEMENT:
                LOG.warn("ScheduleRelationship.REPLACEMENT trips are currently not handled.");
                break;
            }
        }
        return updates;
    }

    protected TripUpdate getUpdateForCanceledTrip(AgencyAndId tripId, long timestamp, ServiceDate serviceDate) {
        return TripUpdate.forCanceledTrip(tripId, timestamp, serviceDate);
    }

    protected TripUpdate getUpdateForScheduledTrip(AgencyAndId tripId,
            GtfsRealtime.TripUpdate tripUpdate, long timestamp, ServiceDate serviceDate) {
        List<Update> updates = new LinkedList<Update>();

        for (StopTimeUpdate stopTimeUpdate : tripUpdate.getStopTimeUpdateList()) {
            Update u = getStopTimeUpdateForTrip(tripId, timestamp, serviceDate, stopTimeUpdate);
            updates.add(u);
        }

        return TripUpdate.forUpdatedTrip(tripId, timestamp, serviceDate, updates);
    }

    private Update getStopTimeUpdateForTrip(AgencyAndId tripId, long timestamp,
            ServiceDate serviceDate, StopTimeUpdate stopTimeUpdate) {
        
        AgencyAndId stopId = new AgencyAndId(defaultAgencyId, stopTimeUpdate.getStopId());
        
        if (stopTimeUpdate.hasScheduleRelationship()
                && StopTimeUpdate.ScheduleRelationship.NO_DATA == stopTimeUpdate
                        .getScheduleRelationship()) {
            Update u = new Update(tripId, stopId, stopTimeUpdate.getStopSequence(),
                    0, 0, Update.Status.PLANNED, timestamp, serviceDate);
            return u;
        }

        // TODO: handle cases where only the delay is provided
        long today = serviceDate.getAsDate(TimeZone.getTimeZone("GMT")).getTime() / 1000;
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

        Update u = new Update(tripId, stopId, stopTimeUpdate.getStopSequence(),
                (int) arrivalTime, (int) departureTime, status, timestamp, serviceDate);
        return u;
    }

    private TripUpdate getUpdateForAddedTrip(AgencyAndId tripId, GtfsRealtime.TripUpdate tripUpdate, long timestamp, ServiceDate serviceDate) {
        Trip trip = new Trip();
        trip.setId(tripId);

        Route route = new Route();
        AgencyAndId routeId = new AgencyAndId(defaultAgencyId, tripUpdate.getTrip().getRouteId());
        route.setId(routeId);
        trip.setRoute(route);

        long today = serviceDate.getAsDate(TimeZone.getTimeZone("GMT")).getTime() / 1000;

        List<Update> updates = new LinkedList<Update>();
        for(StopTimeUpdate stopTimeUpdate : tripUpdate.getStopTimeUpdateList()) {
            
            AgencyAndId stopId = new AgencyAndId(defaultAgencyId, stopTimeUpdate.getStopId());
            long arrivalTime = stopTimeUpdate.getArrival().getTime();
            long departureTime = stopTimeUpdate.getDeparture().getTime();
            
            arrivalTime = arrivalTime - today;
            departureTime = departureTime - today;
            
            Update u = new Update(tripId, stopId, stopTimeUpdate.getStopSequence(),
                    (int) arrivalTime, (int) departureTime, Update.Status.PREDICTION,
                    timestamp, serviceDate);

            updates.add(u);
        }
        
        return TripUpdate.forAddedTrip(trip, timestamp, serviceDate, updates);
    }
}
