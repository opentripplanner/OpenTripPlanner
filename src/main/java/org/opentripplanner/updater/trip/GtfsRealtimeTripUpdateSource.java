package org.opentripplanner.updater.trip;

import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import de.mfdz.MfdzRealtimeExtensions;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsRealtimeTripUpdateSource {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeTripUpdateSource.class);
  /**
   * Feed id that is used to match trip ids in the TripUpdates
   */
  private final String feedId;
  private final String url;
  private final HttpHeaders headers;
  private boolean fullDataset = true;
  private final ExtensionRegistry registry = ExtensionRegistry.newInstance();

  public GtfsRealtimeTripUpdateSource(PollingTripUpdaterParameters config) {
    this.feedId = config.feedId();
    this.url = config.url();
    this.headers = HttpHeaders.of().acceptProtobuf().add(config.headers()).build();
    MfdzRealtimeExtensions.registerAllExtensions(registry);
  }

  public List<TripUpdate> getUpdates() {
    FeedMessage feedMessage;
    List<FeedEntity> feedEntityList;
    List<TripUpdate> updates = null;
    fullDataset = true;
    try {
      InputStream is = HttpUtils.openInputStream(URI.create(url), this.headers.asMap());
      if (is != null) {
        // Decode message
        feedMessage = FeedMessage.parseFrom(is, registry);
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
      } else {
        LOG.error("GTFS-RT feed at {} did not return usable data", url);
      }
    } catch (Exception e) {
      LOG.error("Failed to parse GTFS-RT feed from {}", url, e);
    }
    return updates;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addStr("feedId", feedId)
      .addStr("url", url)
      .toString();
  }

  /**
   * @return true iff the last list with updates represent all updates that are active right now,
   * i.e. all previous updates should be disregarded
   */
  public boolean getFullDatasetValueOfLastUpdates() {
    return fullDataset;
  }
}
