package org.opentripplanner.ext.carpooling.updater;

import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.support.siri.SiriFileLoader;
import org.opentripplanner.updater.support.siri.SiriHttpLoader;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableSource;
import org.opentripplanner.updater.trip.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.ServiceDelivery;

/**
 * Update OTP stop timetables from some a Siri-ET HTTP sources.
 */
public class SiriETCarpoolingUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETCarpoolingUpdater.class);
  private final EstimatedTimetableSource updateSource;

  private final CarpoolingRepository repository;
  private final CarpoolSiriMapper mapper;

  public SiriETCarpoolingUpdater(
    DefaultSiriETUpdaterParameters config,
    CarpoolingRepository repository
  ) {
    super(config);
    this.updateSource = new SiriETHttpTripUpdateSource(config, siriLoader(config));
    this.repository = repository;
    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

    LOG.info("Creating SIRI-ET updater running every {}: {}", pollingPeriod(), updateSource);

    this.mapper = new CarpoolSiriMapper();
  }

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve carpooling trip updates.
   */
  @Override
  public void runPolling() {
    boolean moreData;
    do {
      moreData = fetchAndProcessUpdates();
    } while (moreData);
  }

  /**
   * Fetches updates from the source and processes them.
   *
   * @return true if there is more data available to fetch
   */
  private boolean fetchAndProcessUpdates() {
    var updates = updateSource.getUpdates();
    if (updates.isEmpty()) {
      return false;
    }

    ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
    processEstimatedTimetableDeliveries(serviceDelivery.getEstimatedTimetableDeliveries());
    return Boolean.TRUE.equals(serviceDelivery.isMoreData());
  }

  /**
   * Processes a list of estimated timetable deliveries.
   *
   * @param deliveries the list of estimated timetable deliveries, may be null
   */
  private void processEstimatedTimetableDeliveries(
    List<EstimatedTimetableDeliveryStructure> deliveries
  ) {
    if (deliveries == null || deliveries.isEmpty()) {
      return;
    }

    for (EstimatedTimetableDeliveryStructure delivery : deliveries) {
      var frames = delivery.getEstimatedJourneyVersionFrames();
      for (var frame : frames) {
        var estimatedVehicleJourneys = frame.getEstimatedVehicleJourneies();

        if (estimatedVehicleJourneys == null || estimatedVehicleJourneys.isEmpty()) {
          LOG.warn("Received an empty EstimatedJourneyVersionFrame, skipping");
          continue;
        }

        estimatedVehicleJourneys.forEach(this::processEstimatedVehicleJourney);
      }
    }
  }

  /**
   * Maps a single estimated vehicle journey to a carpool trip and upserts it, or removes the
   * trip when the journey is cancelled or has fewer than 2 non-cancelled calls.
   */
  void processEstimatedVehicleJourney(EstimatedVehicleJourney estimatedVehicleJourney) {
    try {
      FeedScopedId tripId = mapper.tripId(estimatedVehicleJourney);
      if (Boolean.TRUE.equals(estimatedVehicleJourney.isCancellation())) {
        repository.removeCarpoolTrip(tripId);
        return;
      }
      var carpoolTrip = mapper.mapSiriToCarpoolTrip(estimatedVehicleJourney);
      if (carpoolTrip == null) {
        repository.removeCarpoolTrip(tripId);
        return;
      }
      repository.upsertCarpoolTrip(carpoolTrip);
    } catch (Exception e) {
      LOG.warn("Failed to process EstimatedVehicleJourney: {}", e.getMessage());
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SiriETCarpoolingUpdater.class)
      .addStr("source", updateSource.toString())
      .addDuration("frequency", pollingPeriod())
      .toString();
  }

  private static SiriLoader siriLoader(DefaultSiriETUpdaterParameters config) {
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
