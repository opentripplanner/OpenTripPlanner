package org.opentripplanner.updater.vehicle_rental;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public class VehicleRentalUpdaterParameters implements PollingGraphUpdaterParameters {
  private final String configRef;
  private final String networks;
  private final int frequencySec;
  private final VehicleRentalDataSourceParameters source;

  public VehicleRentalUpdaterParameters(
      String configRef,
      String networks,
      int frequencySec,
      VehicleRentalDataSourceParameters source
  ) {
    this.configRef = configRef;
    this.networks = networks;
    this.frequencySec = frequencySec;
    this.source = source;
  }

  // TODO OTP2 - What is the difference between this and the "network" in the source
  String getNetworks() { return networks; }

  @Override
  public int getFrequencySec() {
    return frequencySec;
  }

  /**
   * The config name/type for the updater. Used to reference the configuration element.
   */
  @Override
  public String getConfigRef() {
    return configRef;
  }

  VehicleRentalDataSourceParameters sourceParameters() {
    return source;
  }
}
