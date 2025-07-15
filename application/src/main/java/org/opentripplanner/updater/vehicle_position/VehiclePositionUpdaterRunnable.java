package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.RealTimeUpdateContext;

class VehiclePositionUpdaterRunnable implements GraphWriterRunnable {

  private final List<VehiclePosition> updates;
  private final RealtimeVehicleRepository realtimeVehicleRepository;
  private final String feedId;
  private final boolean fuzzyTripMatching;
  private final Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures;

  public VehiclePositionUpdaterRunnable(
    RealtimeVehicleRepository realtimeVehicleRepository,
    Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures,
    String feedId,
    boolean fuzzyTripMatching,
    List<VehiclePosition> updates
  ) {
    this.updates = Objects.requireNonNull(updates);
    this.feedId = feedId;
    this.realtimeVehicleRepository = realtimeVehicleRepository;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.vehiclePositionFeatures = vehiclePositionFeatures;
  }

  @Override
  public void run(RealTimeUpdateContext context) {
    RealtimeVehiclePatternMatcher matcher = new RealtimeVehiclePatternMatcher(
      feedId,
      context.transitService()::getTrip,
      context.transitService()::findPattern,
      context.transitService()::findPattern,
      realtimeVehicleRepository,
      context.transitService().getTimeZone(),
      fuzzyTripMatching ? context.gtfsRealtimeFuzzyTripMatcher() : null,
      vehiclePositionFeatures
    );
    // Apply new vehicle positions
    matcher.applyRealtimeVehicleUpdates(updates);
  }
}
