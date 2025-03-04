package org.opentripplanner.updater.alert.gtfs;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.utils.tostring.ToStringBuilder;
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
  private Long lastTimestamp = Long.MIN_VALUE;

  public GtfsRealtimeAlertsUpdater(
    GtfsRealtimeAlertsUpdaterParameters config,
    TimetableRepository timetableRepository
  ) {
    super(config);
    this.url = config.url();
    this.headers = HttpHeaders.of().acceptProtobuf().add(config.headers()).build();
    TransitAlertService transitAlertService = new TransitAlertServiceImpl(timetableRepository);

    this.transitAlertService = transitAlertService;

    this.updateHandler = new AlertsUpdateHandler(config.fuzzyTripMatching());
    this.updateHandler.setEarlyStart(config.earlyStartSec());
    this.updateHandler.setFeedId(config.feedId());
    this.updateHandler.setTransitAlertService(transitAlertService);
    this.otpHttpClient = new OtpHttpClientFactory().create(LOG);
    LOG.info("Creating real-time alert updater running every {}: {}", pollingPeriod(), url);
  }

  public TransitAlertService getTransitAlertService() {
    return transitAlertService;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addStr("url", url).toString();
  }

  @Override
  protected void runPolling() throws InterruptedException, ExecutionException {
    final FeedMessage feed = otpHttpClient.getAndMap(
      URI.create(url),
      this.headers.asMap(),
      FeedMessage::parseFrom
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
    updateGraph(context -> updateHandler.update(feed, context.gtfsRealtimeFuzzyTripMatcher()));

    lastTimestamp = feedTimestamp;
  }
}
