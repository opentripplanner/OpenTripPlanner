package org.opentripplanner.ext.bikerentalservicedirectory;

import java.net.URI;

public class BikeRentalServiceDirectoryFetcherParameters {
  private final URI url;
  private final String sourcesName;
  private final String sourceUrlName;
  private final String sourceNetworkName;

  public BikeRentalServiceDirectoryFetcherParameters(
      URI url, String sourcesName, String updaterUrlName, String networkName
  ) {
    this.url = url;
    this.sourcesName = sourcesName;
    this.sourceUrlName = updaterUrlName;
    this.sourceNetworkName = networkName;
  }

  public URI getUrl() {
    return url;
  }

  public String getSourcesName() {
    return sourcesName;
  }

  public String getSourceUrlName() {
    return sourceUrlName;
  }

  public String getSourceNetworkName() {
    return sourceNetworkName;
  }
}
