package org.opentripplanner.updater.vehicle_rental;

import java.time.Duration;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public class VehicleRentalUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final Duration frequency;
  private final Duration startupRetryPeriod;
  private final VehicleRentalDataSourceParameters source;

  public VehicleRentalUpdaterParameters(
    String configRef,
    Duration frequency,
    Duration startupRetryPeriod,
    VehicleRentalDataSourceParameters source
  ) {
    this.configRef = configRef;
    this.frequency = frequency;
    this.startupRetryPeriod = startupRetryPeriod;
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

  /**
   * How long to retry loading the vehicle rental data source on startup if it initially fails.
   */
  public Duration startupRetryPeriod() {
    return startupRetryPeriod;
  }

  public VehicleRentalDataSourceParameters sourceParameters() {
    return source;
  }
}
