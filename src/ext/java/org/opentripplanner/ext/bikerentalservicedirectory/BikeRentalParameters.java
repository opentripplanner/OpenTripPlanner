package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.UpdaterDataSourceParameters;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;

public class BikeRentalParameters implements BikeRentalUpdater.Parameters {

  private final UpdaterDataSourceParameters parameters;

  private final String url;

  private final int frequencySec;

  private final String network;

  private final String configRef;

  public BikeRentalParameters(
      String configRef,
      String network,
      String url,
      int frequencySec,
      UpdaterDataSourceParameters parameters
  ) {
    this.configRef = configRef;
    this.network = network;
    this.url = url;
    this.frequencySec = frequencySec;
    this.parameters = parameters;
  }

  @Override
  public UpdaterDataSourceParameters getSourceParameters() {
    return parameters;
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

  @Override
  public String getConfigRef() {
    return configRef;
  }
}
