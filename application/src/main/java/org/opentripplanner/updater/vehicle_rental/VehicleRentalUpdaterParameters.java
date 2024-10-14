package org.opentripplanner.updater.vehicle_rental;

import java.time.Duration;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public class VehicleRentalUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final Duration frequency;
  private final VehicleRentalDataSourceParameters source;

  public VehicleRentalUpdaterParameters(
    String configRef,
    Duration frequency,
    VehicleRentalDataSourceParameters source
  ) {
    this.configRef = configRef;
    this.frequency = frequency;
    this.source = source;
  }

  @Override
  public Duration frequency() {
    return frequency;
  }

  /**
   * The config name/type for the updater. Used to reference the configuration element.
   */
  @Override
  public String configRef() {
    return configRef;
  }

  public VehicleRentalDataSourceParameters sourceParameters() {
    return source;
  }
}
