package org.opentripplanner.updater.stoptime;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Reads the GTFS-RT from a local file. */
public class GtfsRealtimeFileTripUpdateSource implements TripUpdateSource {
    private static final Logger LOG =
            LoggerFactory.getLogger(GtfsRealtimeFileTripUpdateSource.class);

    private final File file;

    /**
     * True iff the last list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private boolean fullDataset = true;
    
    /**
     * Default agency id that is used for the trip ids in the TripUpdates
     */
    private final String feedId;

    public GtfsRealtimeFileTripUpdateSource(Parameters config) {
        this.feedId = getFeedId();
        this.file = new File(config.getFile());
    }

    @Override
    public List<TripUpdate> getUpdates() {
        FeedMessage feedMessage = null;
        List<FeedEntity> feedEntityList = null;
        List<TripUpdate> updates = null;
        fullDataset = true;
        try {
            InputStream is = new FileInputStream(file);

            // Decode message
            feedMessage = FeedMessage.PARSER.parseFrom(is);
            feedEntityList = feedMessage.getEntityList();

            // Change fullDataset value if this is an incremental update
            if (feedMessage.hasHeader()
                    && feedMessage.getHeader().hasIncrementality()
                    && feedMessage.getHeader().getIncrementality()
                            .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)) {
                fullDataset = false;
            }

            // Create List of TripUpdates
            updates = new ArrayList<TripUpdate>(feedEntityList.size());
            for (FeedEntity feedEntity : feedEntityList) {
                if (feedEntity.hasTripUpdate()) {
                    updates.add(feedEntity.getTripUpdate());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed at " + file + ":", e);
        }
        return updates;
    }

    @Override
    public boolean getFullDatasetValueOfLastUpdates() {
        return fullDataset;
    }
    
    public String toString() {
        return "GtfsRealtimeFileTripUpdateSource(" + file + ")";
    }

    @Override
    public String getFeedId() {
        return this.feedId;
    }

    public interface Parameters {
        String getFeedId();
        String getFile();
    }
}
