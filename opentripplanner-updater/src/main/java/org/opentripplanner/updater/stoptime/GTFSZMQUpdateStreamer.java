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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.trippattern.Update;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class GTFSZMQUpdateStreamer implements UpdateStreamer {

    private static final File file = new File("/var/otp/data/nl/gtfs-rt.protobuf");
    
    @Override
    public List<Update> getUpdates() {
        try {
            InputStream is = new FileInputStream(file);
            FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(is);
            // System.out.println(feed);
            FeedHeader header = feed.getHeader();
            long timestamp = header.getTimestamp();
            List<Update> updates = new ArrayList<Update>();
            for (FeedEntity entity : feed.getEntityList()) {
                System.out.println(entity);
                TripUpdate tUpdate = entity.getTripUpdate();
                String trip = tUpdate.getTrip().getTripId();
                AgencyAndId tripId = new AgencyAndId("agency", trip);
                for (StopTimeUpdate sUpdate : tUpdate.getStopTimeUpdateList()) {
                    Update u = new Update(tripId, 
                            sUpdate.getStopId(),
                            sUpdate.getStopSequence(), 
                            (int) sUpdate.getArrival().getTime(), 
                            (int) sUpdate.getDeparture().getTime(),
                            Update.Status.UNKNOWN,
                            0);
                    updates.add(u);
                }
            }
            return updates;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
