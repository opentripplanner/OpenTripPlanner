package org.opentripplanner.updater.bike_rental;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;

public class BikeRentalUpdaterParameters implements PollingGraphUpdaterParameters {
  private final String configRef;
  private final String url;
  private final String networks;
  private final int frequencySec;
  private final BikeRentalDataSourceParameters source;

  public BikeRentalUpdaterParameters(
      String configRef,
      String url,
      String networks,
      int frequencySec,
      BikeRentalDataSourceParameters source
  ) {
    this.configRef = configRef;
    this.url = url;
    this.networks = networks;
    this.frequencySec = frequencySec;
    this.source = source;
  }

  // TODO OTP2 - What is the difference between this and the "network" in the source
  String getNetworks() { return networks; }

  public String getUrl() {
    return url;
  }

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

  BikeRentalDataSourceParameters sourceParameters() {
    return source;
  }
}
