package org.opentripplanner.updater.alert;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import java.net.URI;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GTFS-RT alerts updater
 */
public class GtfsRealtimeAlertsUpdater extends PollingGraphUpdater implements TransitAlertProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeAlertsUpdater.class);

  private final String url;
  private final AlertsUpdateHandler updateHandler;
  private final TransitAlertService transitAlertService;
  private final HttpHeaders headers;
  private final OtpHttpClient otpHttpClient;
  private WriteToGraphCallback saveResultOnGraph;
  private Long lastTimestamp = Long.MIN_VALUE;

  public GtfsRealtimeAlertsUpdater(
    GtfsRealtimeAlertsUpdaterParameters config,
    TransitModel transitModel
  ) {
    super(config);
    this.url = config.url();
    this.headers = HttpHeaders.of().acceptProtobuf().add(config.headers()).build();
    TransitAlertService transitAlertService = new TransitAlertServiceImpl(transitModel);

    var fuzzyTripMatcher = config.fuzzyTripMatching()
      ? new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(transitModel))
      : null;

    this.transitAlertService = transitAlertService;

    this.updateHandler = new AlertsUpdateHandler();
    this.updateHandler.setEarlyStart(config.earlyStartSec());
    this.updateHandler.setFeedId(config.feedId());
    this.updateHandler.setTransitAlertService(transitAlertService);
    this.updateHandler.setFuzzyTripMatcher(fuzzyTripMatcher);
    this.otpHttpClient = new OtpHttpClient();
    LOG.info(
      "Creating real-time alert updater running every {} seconds : {}",
      pollingPeriod(),
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
      final FeedMessage feed = otpHttpClient.getAndMap(
        URI.create(url),
        this.headers.asMap(),
        FeedMessage.PARSER::parseFrom
      );

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
