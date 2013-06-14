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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

public abstract class GtfsRealtimeAbstractUpdateStreamer implements UpdateStreamer {

    private static final Logger LOG = LoggerFactory
            .getLogger(GtfsRealtimeAbstractUpdateStreamer.class);

    private static final SimpleDateFormat ymdParser = new SimpleDateFormat("yyyyMMdd");
    {
        ymdParser.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    /**
     * The agencyId to use when entities lack one.
     */
    @Setter
    private String defaultAgencyId;
    
    /**
     * The timestamp of the last feed parsed.
     */
    private long lastTimestamp = Long.MIN_VALUE;
    
    /**
     * Entities seen in the feed, along with the TripUpdate it was converted to. 
     * Used to avoid reusing updates already processed.
     */
    private Map<String, TripUpdate> seenEntities = new HashMap<String, TripUpdate>();
    
    /**
     * Retrieves a GTFS-rt feed message.
     */
    protected abstract GtfsRealtime.FeedMessage getFeedMessage();

    /**
     * Converts a GTFS-rt feed into TripUpdates for updating the graph.
     * 
     * If a feed doesn't have a timestamp newer than the last processed feed, than it is discarded. 
     */
    @Override
    public List<org.opentripplanner.routing.trippattern.TripUpdate> getUpdates() {
        GtfsRealtime.FeedMessage feed = getFeedMessage();
        if (feed == null)
            return null;

        GtfsRealtime.FeedHeader header = feed.getHeader();
        long feedTimestamp = header.getTimestamp();
        if(feedTimestamp < lastTimestamp) {
            LOG.info("Ignoring feed with old timestamp.");
            return Collections.emptyList();
        }
        lastTimestamp = feedTimestamp;
        
        List<org.opentripplanner.routing.trippattern.TripUpdate> updates = new ArrayList<org.opentripplanner.routing.trippattern.TripUpdate>();
        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) {
                continue;
            }

            GtfsRealtime.TripUpdate rtTripUpdate = entity.getTripUpdate();
            GtfsRealtime.TripDescriptor descriptor = rtTripUpdate.getTrip();
            
            long timestamp = rtTripUpdate.hasTimestamp() ? rtTripUpdate.getTimestamp() : feedTimestamp;

            if(seenEntities.containsKey(entity.getId())) {
                TripUpdate processed = seenEntities.get(entity.getId());
                if(timestamp <= processed.getTimestamp())
                    continue;
            }
            
            String trip = descriptor.getTripId();
            AgencyAndId tripId = new AgencyAndId(defaultAgencyId, trip);
            
            ServiceDate serviceDate = new ServiceDate();
            if (descriptor.hasStartDate()) {
                try {
                    Date date = ymdParser.parse(descriptor.getStartDate());
                    serviceDate = new ServiceDate(date);
                } catch (ParseException e) {
                    LOG.warn("Failed to parse startDate in gtfs-rt feed: \n{}", entity);
                    continue;
                }
            }

            GtfsRealtime.TripDescriptor.ScheduleRelationship sr;
            if (rtTripUpdate.getTrip().hasScheduleRelationship()) {
                sr = rtTripUpdate.getTrip().getScheduleRelationship();
            } else {
                sr = GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
            }

            TripUpdate tripUpdate = null;
            switch (sr) {
            case SCHEDULED:
                tripUpdate = getUpdateForScheduledTrip(tripId, rtTripUpdate, timestamp, serviceDate);
                break;
            case CANCELED:
                tripUpdate = getUpdateForCanceledTrip(tripId, rtTripUpdate, timestamp, serviceDate);
                break;
            case ADDED:
                tripUpdate = getUpdateForAddedTrip(tripId, rtTripUpdate, timestamp, serviceDate);
                break;
            case UNSCHEDULED:
                tripUpdate = getUpdateForUnscheduledTrip(tripId, rtTripUpdate, timestamp, serviceDate);
                break;
            case REPLACEMENT:
                tripUpdate = getUpdateForReplacementTrip(tripId, rtTripUpdate, timestamp, serviceDate);
                break;
            }
            
            if(tripUpdate != null) {
                seenEntities.put(entity.getId(), tripUpdate);
                updates.add(tripUpdate);
            } else {
                LOG.warn("Failed to parse tripUpdate: \n{}", entity);
            }
        }
        return updates;
    }

    private TripUpdate getUpdateForReplacementTrip(AgencyAndId tripId,
            GtfsRealtime.TripUpdate rtTripUpdate, long timestamp, ServiceDate serviceDate) {
        
        LOG.warn("ScheduleRelationship.REPLACEMENT trips are currently not handled.");
        return null;
    }

    private TripUpdate getUpdateForUnscheduledTrip(AgencyAndId tripId,
            GtfsRealtime.TripUpdate rtTripUpdate, long timestamp, ServiceDate serviceDate) {
        
        LOG.warn("ScheduleRelationship.UNSCHEDULED trips are currently not handled.");
        return null;
    }

    protected TripUpdate getUpdateForCanceledTrip(AgencyAndId tripId,
            GtfsRealtime.TripUpdate tripUpdate, long timestamp, ServiceDate serviceDate) {
        
        if(!validateTripDescriptor(tripUpdate.getTrip())) {
            return null;
        }

        return TripUpdate.forCanceledTrip(tripId, timestamp, serviceDate);
    }

    protected TripUpdate getUpdateForAddedTrip(AgencyAndId tripId,
            GtfsRealtime.TripUpdate tripUpdate, long timestamp, ServiceDate serviceDate) {
        
        if(!validateTripDescriptor(tripUpdate.getTrip())) {
            return null;
        }
        
        if(!tripUpdate.getTrip().hasRouteId()) {
            LOG.warn("A routeId must be provided for added/unscheduled trips.");
            return null;
        }

        Trip trip = new Trip();
        trip.setId(tripId);
        
        Route route = new Route();
        AgencyAndId routeId = new AgencyAndId(defaultAgencyId, tripUpdate.getTrip().getRouteId());
        route.setId(routeId);
        trip.setRoute(route);

        List<Update> updates = new LinkedList<Update>();
        for(GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate
                : tripUpdate.getStopTimeUpdateList()) {
            
            Update u = getStopTimeUpdateForTrip(tripId, timestamp, serviceDate, stopTimeUpdate);
            if(u == null) {
                return null;
            }
            updates.add(u);
        }
        
        if(updates.size() < 2) {
            LOG.warn("At least two stop times must be provided for an added trip.");
            return null;
        }
        
        return TripUpdate.forAddedTrip(trip, timestamp, serviceDate, updates);
    }

    protected TripUpdate getUpdateForScheduledTrip(AgencyAndId tripId,
            GtfsRealtime.TripUpdate tripUpdate, long timestamp, ServiceDate serviceDate) {

        if(!validateTripDescriptor(tripUpdate.getTrip())) {
            return null;
        }
        
        List<Update> updates = new LinkedList<Update>();

        for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate
                : tripUpdate.getStopTimeUpdateList()) {
            
            Update u = getStopTimeUpdateForTrip(tripId, timestamp, serviceDate, stopTimeUpdate);
            if(u == null) {
                return null;
            }
            updates.add(u);
        }

        if(updates.isEmpty()) {
            return null;
        }

        return TripUpdate.forUpdatedTrip(tripId, timestamp, serviceDate, updates);
    }

    protected Update getStopTimeUpdateForTrip(AgencyAndId tripId, long timestamp,
            ServiceDate serviceDate, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate) {
        
        if(!(stopTimeUpdate.hasStopId() || stopTimeUpdate.hasStopSequence())) {
            LOG.warn("A stopId or stopSequence must be provided: \n{}", stopTimeUpdate);
            return null;
        }
        
        AgencyAndId stopId = null;
        if(stopTimeUpdate.hasStopId())
            stopId = new AgencyAndId(defaultAgencyId, stopTimeUpdate.getStopId());
        
        Integer stopSequence = null;
        if(stopTimeUpdate.hasStopSequence())
            stopSequence = stopTimeUpdate.getStopSequence();
        
        if (stopTimeUpdate.hasScheduleRelationship()
                && GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA == stopTimeUpdate
                        .getScheduleRelationship()) {
            Update u = new Update(tripId, stopId, stopSequence,
                    0, 0, Update.Status.PLANNED, timestamp, serviceDate);
            return u;
        }

        if (stopTimeUpdate.hasScheduleRelationship()
                && GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED == stopTimeUpdate
                        .getScheduleRelationship()) {
            Update u = new Update(tripId, stopId, stopSequence,
                    0, 0, Update.Status.CANCEL, timestamp, serviceDate);
            return u;
        } else {
            if(!(stopTimeUpdate.hasArrival() || stopTimeUpdate.hasDeparture())) {
                LOG.warn("Neither an arrival or departure was provided for: \n{}", stopTimeUpdate);
                return null;
            }
            
            long today = serviceDate.getAsDate(TimeZone.getTimeZone("GMT")).getTime() / 1000;
            long arrivalTime = -1, departureTime = -1;
            
            if(stopTimeUpdate.hasArrival()) {
                GtfsRealtime.TripUpdate.StopTimeEvent event = stopTimeUpdate.getArrival();
                if(event.hasTime()) {
                    arrivalTime = event.getTime();
                    arrivalTime = arrivalTime - today;
                } else if(event.hasDelay()) {
                    LOG.warn("Providing only a delay is not supported yet: \n{}", stopTimeUpdate);
                    return null;
                }
            }
            if(stopTimeUpdate.hasDeparture()) {
                GtfsRealtime.TripUpdate.StopTimeEvent event = stopTimeUpdate.getDeparture();
                if(event.hasTime()) {
                    departureTime = event.getTime();
                    departureTime = departureTime - today;
                } else if(event.hasDelay()) {
                    LOG.warn("Providing only a delay is not supported yet: \n{}", stopTimeUpdate);
                    return null;
                }
            }
            
            if(arrivalTime < 0 && departureTime < 0) {
                LOG.warn("Neither an arrival or departure time was provided for: \n{}", stopTimeUpdate);
                return null;
            }
            
            if(arrivalTime == -1) {
                arrivalTime = departureTime;
            }
            if(departureTime == -1) {
                departureTime = arrivalTime;
            }

            Update u = new Update(tripId, stopId, stopSequence,
                    (int) arrivalTime, (int) departureTime, Update.Status.PREDICTION,
                    timestamp, serviceDate);
            return u;
        }
    }
    
    protected boolean validateTripDescriptor(GtfsRealtime.TripDescriptor tripDescriptor) {

        if(tripDescriptor.hasStartTime()) {
            LOG.warn("Frequency-expanded trips are not supported...");
            return false;
        }
        
        return tripDescriptor.hasTripId();
    }
}
