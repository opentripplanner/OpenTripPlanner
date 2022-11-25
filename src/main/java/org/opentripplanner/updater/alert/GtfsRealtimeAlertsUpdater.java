package org.opentripplanner.updater.alert;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GTFS-RT alerts updater
 * <p>
 * Usage example:
 *
 * <pre>
 * myalert.type = real-time-alerts
 * myalert.frequencySec = 60
 * myalert.url = http://host.tld/path
 * myalert.earlyStartSec = 3600
 * myalert.feedId = TA
 * </pre>
 */
public class GtfsRealtimeAlertsUpdater extends PollingGraphUpdater implements TransitAlertProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeAlertsUpdater.class);
  private final String url;
  private final AlertsUpdateHandler updateHandler;
  private final TransitAlertService transitAlertService;
  private WriteToGraphCallback saveResultOnGraph;
  private Long lastTimestamp = Long.MIN_VALUE;

  public GtfsRealtimeAlertsUpdater(
    GtfsRealtimeAlertsUpdaterParameters config,
    TransitModel transitModel
  ) {
    super(config);
    this.url = config.getUrl();
    TransitAlertService transitAlertService = new TransitAlertServiceImpl(transitModel);

    var fuzzyTripMatcher = config.fuzzyTripMatching()
      ? new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(transitModel))
      : null;

    this.transitAlertService = transitAlertService;

    this.updateHandler = new AlertsUpdateHandler();
    this.updateHandler.setEarlyStart(config.getEarlyStartSec());
    this.updateHandler.setFeedId(config.getFeedId());
    this.updateHandler.setTransitAlertService(transitAlertService);
    this.updateHandler.setFuzzyTripMatcher(fuzzyTripMatcher);

    LOG.info(
      "Creating real-time alert updater running every {} seconds : {}",
      pollingPeriodSeconds(),
      url
    );
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  public TransitAlertService getTransitAlertService() {
    return transitAlertService;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addStr("url", url).toString();
  }

  @Override
  protected void runPolling() {
    try {
      InputStream data = HttpUtils.getData(
        URI.create(url),
        Map.of(
          "Accept",
          "application/x-google-protobuf, application/x-protobuf, application/protobuf, application/octet-stream, */*"
        )
      );
      if (data == null) {
        throw new RuntimeException("Failed to get data from url " + url);
      }

      final FeedMessage feed = FeedMessage.PARSER.parseFrom(data);

      long feedTimestamp = feed.getHeader().getTimestamp();
      if (feedTimestamp == lastTimestamp) {
        LOG.debug("Ignoring feed with a timestamp that has not been updated from {}", url);
        return;
      }
      if (feedTimestamp < lastTimestamp) {
        LOG.info("Ignoring feed with older than previous timestamp from {}", url);
        return;
      }

      // Handle update in graph writer runnable
      saveResultOnGraph.execute((graph, transitModel) -> updateHandler.update(feed));

      lastTimestamp = feedTimestamp;
    } catch (Exception e) {
      LOG.error("Error reading gtfs-realtime feed from " + url, e);
    }
  }
}
