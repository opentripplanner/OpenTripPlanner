package org.opentripplanner.updater.stoptime;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.lang.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsRealtimeHttpTripUpdateSource implements TripUpdateSource {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeHttpTripUpdateSource.class);
  /**
   * Feed id that is used to match trip ids in the TripUpdates
   */
  private final String feedId;
  private final String url;
  /**
   * True iff the last list with updates represent all updates that are active right now, i.e. all
   * previous updates should be disregarded
   */
  private boolean fullDataset = true;

  public GtfsRealtimeHttpTripUpdateSource(Parameters config) {
    this.feedId = config.getFeedId();
    this.url = config.getUrl();
  }

  @Override
  public List<TripUpdate> getUpdates() {
    FeedMessage feedMessage = null;
    List<FeedEntity> feedEntityList = null;
    List<TripUpdate> updates = null;
    fullDataset = true;
    try {
      InputStream is = HttpUtils.getData(
        URI.create(url),
        Map.of(
          "Accept",
          "application/x-google-protobuf, application/x-protobuf, application/protobuf, application/octet-stream, */*"
        )
      );
      if (is != null) {
        // Decode message
        feedMessage = FeedMessage.PARSER.parseFrom(is);
        feedEntityList = feedMessage.getEntityList();

        // Change fullDataset value if this is an incremental update
        if (
          feedMessage.hasHeader() &&
          feedMessage.getHeader().hasIncrementality() &&
          feedMessage
            .getHeader()
            .getIncrementality()
            .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)
        ) {
          fullDataset = false;
        }

        // Create List of TripUpdates
        updates = new ArrayList<>(feedEntityList.size());
        for (FeedEntity feedEntity : feedEntityList) {
          if (feedEntity.hasTripUpdate()) updates.add(feedEntity.getTripUpdate());
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
    }
    return updates;
  }

  @Override
  public boolean getFullDatasetValueOfLastUpdates() {
    return fullDataset;
  }

  @Override
  public String getFeedId() {
    return this.feedId;
  }

  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addStr("feedId", feedId)
      .addStr("url", url)
      .toString();
  }

  interface Parameters {
    String getFeedId();

    String getUrl();
  }
}
