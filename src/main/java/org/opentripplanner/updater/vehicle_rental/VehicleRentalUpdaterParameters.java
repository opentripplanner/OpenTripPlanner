package org.opentripplanner.updater.vehicle_rental;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public class VehicleRentalUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final int frequencySec;
  private final VehicleRentalDataSourceParameters source;

  public VehicleRentalUpdaterParameters(
    String configRef,
    int frequencySec,
    VehicleRentalDataSourceParameters source
  ) {
    this.configRef = configRef;
    this.frequencySec = frequencySec;
    this.source = source;
  }

  @Override
  public int frequencySec() {
    return frequencySec;
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
