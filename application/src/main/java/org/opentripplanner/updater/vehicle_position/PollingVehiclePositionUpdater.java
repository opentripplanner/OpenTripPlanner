package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Set;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map vehicle positions to
 * {@link RealtimeVehicle} and add them to OTP
 * patterns via a GTFS-RT source.
 */
public class PollingVehiclePositionUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(PollingVehiclePositionUpdater.class);

  /**
   * Update source
   */
  private final GtfsRealtimeHttpVehiclePositionSource vehiclePositionSource;
  private final Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures;

  private final String feedId;
  private final RealtimeVehicleRepository realtimeVehicleRepository;
  private final boolean fuzzyTripMatching;

  public PollingVehiclePositionUpdater(
    VehiclePositionsUpdaterParameters params,
    RealtimeVehicleRepository realtimeVehicleRepository
  ) {
    super(params);
    this.vehiclePositionSource = new GtfsRealtimeHttpVehiclePositionSource(
      params.url(),
      params.headers()
    );
    this.realtimeVehicleRepository = realtimeVehicleRepository;
    this.feedId = params.feedId();
    this.fuzzyTripMatching = params.fuzzyTripMatching();
    this.vehiclePositionFeatures = params.vehiclePositionFeatures();

    LOG.info(
      "Creating vehicle position updater running every {}: {}",
      pollingPeriod(),
      vehiclePositionSource
    );
  }

  /**
   * Repeatedly makes blocking calls to a source to retrieve new stop time updates, and
   * applies those updates to the graph.
   */
  @Override
  public void runPolling() {
    // Get update lists from update source
    List<VehiclePosition> updates = vehiclePositionSource.getPositions();

    // Handle updating trip positions via graph writer runnable
    var runnable = new VehiclePositionUpdaterRunnable(
      realtimeVehicleRepository,
      vehiclePositionFeatures,
      feedId,
      fuzzyTripMatching,
      updates
    );
    updateGraph(runnable);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("source", vehiclePositionSource).toString();
  }
}
