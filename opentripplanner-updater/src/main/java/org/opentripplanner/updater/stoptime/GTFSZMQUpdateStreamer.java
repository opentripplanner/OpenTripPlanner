package org.opentripplanner.updater.stoptime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.routing.trippattern.UpdateList;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class GTFSZMQUpdateStreamer {

    private static final File file = new File("/var/otp/data/nl/gtfs-rt.protobuf");
    
    public UpdateList getUpdates() {
        try {
            InputStream is = new FileInputStream(file);
            FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(is);
            // System.out.println(feed);
            FeedHeader header = feed.getHeader();
            long timestamp = header.getTimestamp();
            UpdateList updates = new UpdateList(null);
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
                            (int) sUpdate.getDeparture().getTime());
                    updates.addUpdate(u);
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
