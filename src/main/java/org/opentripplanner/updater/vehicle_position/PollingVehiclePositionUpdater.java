package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
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
    TransitModel transitModel
  ) {
    super(params);
    this.vehiclePositionSource =
      new GtfsRealtimeHttpVehiclePositionSource(params.url(), params.headers());
    var index = transitModel.getTransitModelIndex();
    var fuzzyTripMatcher = params.fuzzyTripMatching()
      ? new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(transitModel))
      : null;
    this.realtimeVehiclePatternMatcher =
      new RealtimeVehiclePatternMatcher(
        params.feedId(),
        tripId -> index.getTripForId().get(tripId),
        trip -> index.getPatternForTrip().get(trip),
        (trip, date) -> getPatternIncludingRealtime(transitModel, trip, date),
        realtimeVehicleRepository,
        transitModel.getTimeZone(),
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
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
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

  private static TripPattern getPatternIncludingRealtime(
    TransitModel transitModel,
    Trip trip,
    LocalDate sd
  ) {
    return Optional
      .ofNullable(transitModel.getTimetableSnapshot())
      .map(snapshot -> snapshot.getRealtimeAddedTripPattern(trip.getId(), sd))
      .orElseGet(() -> transitModel.getTransitModelIndex().getPatternForTrip().get(trip));
  }
}
