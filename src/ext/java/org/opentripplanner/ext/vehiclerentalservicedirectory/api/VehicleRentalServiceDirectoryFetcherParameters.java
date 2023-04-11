package org.opentripplanner.ext.vehiclerentalservicedirectory.api;

import java.net.URI;
import org.opentripplanner.updater.spi.HttpHeaders;

public class VehicleRentalServiceDirectoryFetcherParameters {

  /**
   * Endpoint for the VehicleRentalServiceDirectory
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

  /**
   * Json tag name for http headers
   * <p>
   * Optional, default value is null
   */
  private final HttpHeaders headers;

  private final String language;

  public VehicleRentalServiceDirectoryFetcherParameters(
    URI url,
    String sourcesName,
    String updaterUrlName,
    String networkName,
    String language,
    HttpHeaders headers
  ) {
    this.url = url;
    this.sourcesName = sourcesName;
    this.sourceUrlName = updaterUrlName;
    this.sourceNetworkName = networkName;
    this.language = language;
    this.headers = headers;
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

  public HttpHeaders getHeaders() {
    return headers;
  }

  public String getLanguage() {
    return language;
  }
}
