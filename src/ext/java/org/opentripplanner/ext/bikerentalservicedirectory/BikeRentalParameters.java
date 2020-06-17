package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.UpdaterDataSourceConfig;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;

public class BikeRentalParameters implements BikeRentalUpdater.Parameters {

  private final UpdaterDataSourceConfig sourceConfig;

  private final String url;

  private final int frequencySec;

  private final String network;

  public BikeRentalParameters(
      UpdaterDataSourceConfig sourceConfig,
      String url,
      int frequencySec,
      String network
  ) {
    this.sourceConfig = sourceConfig;
    this.url = url;
    this.frequencySec = frequencySec;
    this.network = network;
  }

  @Override
  public UpdaterDataSourceConfig getSourceConfig() {
    return sourceConfig;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public int getFrequencySec() {
    return frequencySec;
  }

  @Override
  public String getNetwork() {
    return network;
  }

  @Override
  public String getNetworks() {
    return null;
  }

  @Override
  public String getApiKey() {
    return null;
  }
}
