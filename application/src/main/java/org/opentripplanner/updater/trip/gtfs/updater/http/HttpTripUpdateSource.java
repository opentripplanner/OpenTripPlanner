package org.opentripplanner.updater.trip.gtfs.updater.http;

import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import de.mfdz.MfdzRealtimeExtensions;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpTripUpdateSource {

  private static final Logger LOG = LoggerFactory.getLogger(HttpTripUpdateSource.class);
  /**
   * Feed id that is used to match trip ids in the TripUpdates
   */
  private final String feedId;
  private final String url;
  private final HttpHeaders headers;
  private UpdateIncrementality updateIncrementality = FULL_DATASET;
  private final ExtensionRegistry registry = ExtensionRegistry.newInstance();
  private final OtpHttpClient otpHttpClient;

  public HttpTripUpdateSource(PollingTripUpdaterParameters config) {
    this.feedId = config.feedId();
    this.url = config.url();
    this.headers = HttpHeaders.of().acceptProtobuf().add(config.headers()).build();
    MfdzRealtimeExtensions.registerAllExtensions(registry);
    otpHttpClient = new OtpHttpClientFactory().create(LOG);
  }

  public List<TripUpdate> getUpdates() {
    FeedMessage feedMessage;
    List<FeedEntity> feedEntityList;
    List<TripUpdate> updates = null;
    updateIncrementality = FULL_DATASET;
    try {
      // Decode message
      feedMessage = otpHttpClient.getAndMap(URI.create(url), this.headers.asMap(), is ->
        FeedMessage.parseFrom(is, registry)
      );
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
        updateIncrementality = DIFFERENTIAL;
      }

      // Create List of TripUpdates
      updates = new ArrayList<>(feedEntityList.size());
      for (FeedEntity feedEntity : feedEntityList) {
        if (feedEntity.hasTripUpdate()) updates.add(feedEntity.getTripUpdate());
      }
    } catch (Exception e) {
      LOG.error("Failed to process GTFS-RT TripUpdates feed from {}", url, e);
    }
    return updates;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addStr("feedId", feedId)
      .addStr("url", url)
      .toString();
  }

  /**
   * @return the incrementality of the last list with updates, i.e. if all previous updates
   * should be disregarded
   */
  public UpdateIncrementality incrementalityOfLastUpdates() {
    return updateIncrementality;
  }
}
