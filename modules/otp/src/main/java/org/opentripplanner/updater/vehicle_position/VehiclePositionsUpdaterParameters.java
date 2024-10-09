package org.opentripplanner.updater.vehicle_position;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record VehiclePositionsUpdaterParameters(
  String configRef,
  String feedId,
  URI url,
  Duration frequency,
  HttpHeaders headers,
  boolean fuzzyTripMatching,
  Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures
)
  implements PollingGraphUpdaterParameters {
  public VehiclePositionsUpdaterParameters {
    Objects.requireNonNull(feedId, "feedId is required");
    Objects.requireNonNull(url, "url is required");
  }
}
