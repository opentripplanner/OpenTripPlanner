package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.TransitRealTimeUpdateContext;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;

class VehiclePositionUpdaterRunnable implements GraphWriterRunnable<TransitRealTimeUpdateContext> {

  private final List<VehiclePosition> updates;
  private final String feedId;
  private final boolean fuzzyTripMatching;
  private final Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures;

  public VehiclePositionUpdaterRunnable(
    Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures,
    String feedId,
    boolean fuzzyTripMatching,
    List<VehiclePosition> updates
  ) {
    this.updates = Objects.requireNonNull(updates);
    this.feedId = feedId;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.vehiclePositionFeatures = vehiclePositionFeatures;
  }

  @Override
  public void run(TransitRealTimeUpdateContext context) {
    // Vehicle positions are matched against the last committed timetable snapshot. This updater
    // only reads transit data, so it must not depend on uncommitted changes in the timetable
    // write buffer: a vehicle referencing a trip added in the current, uncommitted transaction
    // is matched on the next polling cycle, after that transaction has committed.
    var transitService = context.committedTransitService();
    RealtimeVehiclePatternMatcher matcher = new RealtimeVehiclePatternMatcher(
      feedId,
      transitService::getTrip,
      transitService::findPattern,
      transitService::findPattern,
      transitService.getTripCalendars()::listServiceDates,
      context.realtimeVehicleRepository(),
      transitService.getTimeZone(),
      fuzzyTripMatching ? new GtfsRealtimeFuzzyTripMatcher(transitService) : null,
      vehiclePositionFeatures
    );
    // Apply new vehicle positions
    matcher.applyRealtimeVehicleUpdates(updates);
  }
}
