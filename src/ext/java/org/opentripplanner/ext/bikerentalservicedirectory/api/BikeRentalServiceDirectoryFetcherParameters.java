package org.opentripplanner.ext.bikerentalservicedirectory.api;

import java.net.URI;

public class BikeRentalServiceDirectoryFetcherParameters {

  /**
   * Endpoint for the BikeRentalServiceDirectory
   * <p>
   * This is required.
   */
  private final URI url;

  /**
   * Json tag name for updater sources
   * <p>
   * Optonal, default values is "systems".
   */
  private final String sourcesName;

  /**
   * Json tag name for endpoint urls for each source
   * <p>
   * Optonal, default values is "url".
   */
  private final String sourceUrlName;

  /**
   * Json tag name for the network name for each source
   * <p>
   * Optonal, default values is "id".
   */
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
