package org.opentripplanner.updater.alert.gtfs;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.updater.AlertRealTimeUpdateContext;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteDomain;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GTFS-RT alerts updater
 */
public class GtfsRealtimeAlertsUpdater extends PollingGraphUpdater<AlertRealTimeUpdateContext> {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeAlertsUpdater.class);

  private final String url;
  private final AlertsUpdateHandler updateHandler;
  private final HttpHeaders headers;
  private final OtpHttpClient otpHttpClient;
  private Long lastTimestamp = Long.MIN_VALUE;

  public GtfsRealtimeAlertsUpdater(GtfsRealtimeAlertsUpdaterParameters config) {
    super(config);
    this.url = config.url();
    this.headers = HttpHeaders.of().acceptProtobuf().add(config.headers()).build();

    this.updateHandler = new AlertsUpdateHandler(config.fuzzyTripMatching());
    this.updateHandler.setEarlyStart(Duration.ofSeconds(config.earlyStartSec()));
    this.updateHandler.setFeedId(config.feedId());
    this.otpHttpClient = new OtpHttpClientFactory().create(LOG);
    LOG.info("Creating real-time alert updater running every {}: {}", pollingPeriod(), url);
  }

  @Override
  public WriteDomain<AlertRealTimeUpdateContext> writeDomain() {
    return WriteDomain.ALERT;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addStr("url", url).toString();
  }

  @Override
  protected void runPolling() throws InterruptedException, ExecutionException {
    final FeedMessage feed = otpHttpClient.getAndMap(URI.create(url), this.headers, response ->
      FeedMessage.parseFrom(response.body())
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
    updateGraph(context ->
      updateHandler.update(
        feed,
        context.gtfsRealtimeFuzzyTripMatcher(),
        context.transitAlertRepository()
      )
    );

    lastTimestamp = feedTimestamp;
  }
}
