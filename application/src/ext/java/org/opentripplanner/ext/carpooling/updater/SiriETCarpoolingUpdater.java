package org.opentripplanner.ext.carpooling.updater;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.support.siri.SiriFileLoader;
import org.opentripplanner.updater.support.siri.SiriHttpLoader;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableSource;
import org.opentripplanner.updater.trip.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.ServiceDelivery;

/**
 * Update OTP stop timetables from some a Siri-ET HTTP sources.
 */
public class SiriETCarpoolingUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETCarpoolingUpdater.class);
  /**
   * Update streamer
   */
  private final EstimatedTimetableSource updateSource;

  private final CarpoolingRepository repository;

  /**
   * Feed id that is used for the trip ids in the TripUpdates
   */
  private final String feedId;

  private final Consumer<UpdateResult> metricsConsumer;

  public SiriETCarpoolingUpdater(
    SiriETCarpoolingUpdaterParameters config,
    CarpoolingRepository repository
  ) {
    super(config);
    this.feedId = config.feedId();

    SiriLoader siriHttpLoader = siriLoader(config);
    updateSource = new SiriETHttpTripUpdateSource(config.sourceParameters(), siriHttpLoader);

    this.repository = repository;

    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

    LOG.info("Creating SIRI-ET updater running every {}: {}", pollingPeriod(), updateSource);

    this.metricsConsumer = TripUpdateMetrics.streaming(config);
  }

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
   * applies those updates to the graph.
   */
  @Override
  public void runPolling() {
    boolean moreData = false;
    do {
      var updates = updateSource.getUpdates();
      if (updates.isPresent()) {
        var incrementality = updateSource.incrementalityOfLastUpdates();
        ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
        moreData = Boolean.TRUE.equals(serviceDelivery.isMoreData());
        // Mark this updater as primed after last page of updates. Copy moreData into a final
        // primitive, because the object moreData persists across iterations.
        final boolean markPrimed = !moreData;
        List<EstimatedTimetableDeliveryStructure> etds =
          serviceDelivery.getEstimatedTimetableDeliveries();
        if (etds != null) {
          LOG.info("Received {} estimated timetable deliveries", etds.size());
        }
      }
    } while (moreData);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SiriETCarpoolingUpdater.class)
      .addStr("source", updateSource.toString())
      .addDuration("frequency", pollingPeriod())
      .toString();
  }

  public interface Parameters extends UrlUpdaterParameters, PollingGraphUpdaterParameters {
    String url();

    boolean blockReadinessUntilInitialized();

    boolean fuzzyTripMatching();
  }

  private static SiriLoader siriLoader(SiriETCarpoolingUpdaterParameters config) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(config.url())) {
      return new SiriFileLoader(config.url());
    }
    return new SiriHttpLoader(
      config.url(),
      config.timeout(),
      config.httpRequestHeaders(),
      config.previewInterval()
    );
  }
}
