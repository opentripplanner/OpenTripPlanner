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
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.trippattern.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public abstract class GtfsRealtimeAbstractUpdateStreamer implements UpdateStreamer {

    private static final Logger LOG = LoggerFactory
            .getLogger(GtfsRealtimeAbstractUpdateStreamer.class);

    private static final SimpleDateFormat ymdParser = new SimpleDateFormat("yyyyMMdd");
    {
        ymdParser.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Autowired
    private GraphService graphService;

    @Setter
    private String defaultAgencyId;

    protected abstract FeedMessage getFeedMessage();

    @Override
    public List<Update> getUpdates() {
        FeedMessage feed = getFeedMessage();
        if (feed == null)
            return null;

        FeedHeader header = feed.getHeader();
        long timestamp = header.getTimestamp();
        List<Update> updates = new ArrayList<Update>();
        for (FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) {
                continue;
            }

            TripUpdate tripUpdate = entity.getTripUpdate();
            TripDescriptor descriptor = tripUpdate.getTrip();
            String trip = descriptor.getTripId();
            AgencyAndId tripId = new AgencyAndId(defaultAgencyId, trip);

            long midnight = (new Date().getTime() / 1000) - (new Date().getTime() / 1000)
                    % (24 * 60 * 60); // TODO: use real midnight
            if (descriptor.hasStartDate()) {
                try {
                    Date date = ymdParser.parse(descriptor.getStartDate());
                    midnight = date.getTime() / 1000;
                } catch (ParseException e) {
                    LOG.warn("Failed to parse startDate in gtfs-rt feed: ", e);
                }
            }

            TripDescriptor.ScheduleRelationship sr;
            if (tripUpdate.getTrip().hasScheduleRelationship()) {
                sr = tripUpdate.getTrip().getScheduleRelationship();
            } else {
                sr = TripDescriptor.ScheduleRelationship.SCHEDULED;
            }

            switch (sr) {
            case SCHEDULED:
                updates.addAll(GtfsRealtimeUpdate.getUpdatesForScheduledTrip(tripId, tripUpdate,
                        timestamp, midnight));
                break;
            case CANCELED:
                updates.addAll(getUpdateForCanceledTrip(tripId, timestamp));
                break;
            case ADDED:
                LOG.warn("ScheduleRelationship.ADDED trips are currently not handled.");
                break;
            case REPLACEMENT:
                LOG.warn("ScheduleRelationship.REPLACEMENT trips are currently not handled.");
                break;
            case UNSCHEDULED:
                LOG.warn("ScheduleRelationship.UNSCHEDULED trips are currently not handled.");
                break;
            }
        }
        return updates;
    }

    private List<GtfsRealtimeUpdate> getUpdateForCanceledTrip(AgencyAndId tripId, long timestamp) {

        List<GtfsRealtimeUpdate> updates = new LinkedList<GtfsRealtimeUpdate>();

        RouteVariant variant = graphService.getGraph().getService(TransitIndexService.class)
                .getVariantForTrip(tripId);

        int stopSequence = 0;
        for (Stop stop : variant.getStops()) {
            updates.add(GtfsRealtimeUpdate.getUpdateForCanceledTrip(tripId, stop, stopSequence,
                    timestamp));
            stopSequence++;
        }

        return updates;
    }
}
