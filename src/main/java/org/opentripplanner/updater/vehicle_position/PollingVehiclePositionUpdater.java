package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map vehicle positions to
 * {@link org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle} and add them to OTP
 * patterns via a GTFS-RT source.
 */
public class PollingVehiclePositionUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(PollingVehiclePositionUpdater.class);

  /**
   * Update streamer
   */
  private final GtfsRealtimeHttpVehiclePositionSource vehiclePositionSource;

  private final RealtimeVehiclePatternMatcher realtimeVehiclePatternMatcher;

  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private WriteToGraphCallback saveResultOnGraph;

  public PollingVehiclePositionUpdater(
    VehiclePositionsUpdaterParameters params,
    RealtimeVehicleRepository realtimeVehicleRepository,
    TransitService transitService
  ) {
    super(params);
    this.vehiclePositionSource =
      new GtfsRealtimeHttpVehiclePositionSource(params.url(), params.headers());
    var fuzzyTripMatcher = params.fuzzyTripMatching()
      ? new GtfsRealtimeFuzzyTripMatcher(transitService)
      : null;
    this.realtimeVehiclePatternMatcher =
      new RealtimeVehiclePatternMatcher(
        params.feedId(),
        transitService::getTripForId,
        transitService::getPatternForTrip,
        transitService::getPatternForTrip,
        realtimeVehicleRepository,
        transitService.getTimeZone(),
        fuzzyTripMatcher,
        params.vehiclePositionFeatures()
      );

    LOG.info(
      "Creating vehicle position updater running every {}: {}",
      pollingPeriod(),
      vehiclePositionSource
    );
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
   * applies those updates to the graph.
   */
  @Override
  public void runPolling() {
    // Get update lists from update source
    List<VehiclePosition> updates = vehiclePositionSource.getPositions();

    if (updates != null) {
      // Handle updating trip positions via graph writer runnable
      var runnable = new VehiclePositionUpdaterRunnable(updates, realtimeVehiclePatternMatcher);
      saveResultOnGraph.execute(runnable);
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("source", vehiclePositionSource).toString();
  }
}
