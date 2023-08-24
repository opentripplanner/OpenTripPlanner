package org.opentripplanner.ext.vehiclerentalservicedirectory.api;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.updater.spi.HttpHeaders;

public class VehicleRentalServiceDirectoryFetcherParameters {

  private final URI url;

  private final String sourcesName;

  private final String sourceUrlName;

  private final String sourceNetworkName;

  private final HttpHeaders headers;

  private final String language;

  private final Map<String, NetworkParameters> parametersForNetwork;

  public VehicleRentalServiceDirectoryFetcherParameters(
    URI url,
    String sourcesName,
    String updaterUrlName,
    String networkName,
    String language,
    HttpHeaders headers,
    Collection<NetworkParameters> networkParameters
  ) {
    this.url = url;
    this.sourcesName = sourcesName;
    this.sourceUrlName = updaterUrlName;
    this.sourceNetworkName = networkName;
    this.language = language;
    this.headers = headers;
    this.parametersForNetwork =
      networkParameters.stream().collect(Collectors.toMap(NetworkParameters::network, it -> it));
  }

  /**
   * Endpoint for the VehicleRentalServiceDirectory
   * <p>
   * This is required.
   */
  public URI getUrl() {
    return url;
  }

  /**
   * Json tag name for updater sources
   * <p>
   * Optional, default values is "systems".
   */
  public String getSourcesName() {
    return sourcesName;
  }

  /**
   * Json tag name for endpoint urls for each source
   * <p>
   * Optional, default values is "url".
   */
  public String getSourceUrlName() {
    return sourceUrlName;
  }

  /**
   * Json tag name for the network name for each source
   * <p>
   * Optional, default values is "id".
   */
  public String getSourceNetworkName() {
    return sourceNetworkName;
  }

  /**
   * Json tag name for http headers
   * <p>
   * Optional, default value is null
   */
  public HttpHeaders getHeaders() {
    return headers;
  }

  public String getLanguage() {
    return language;
  }

  public NetworkParameters networkParameters(String network) {
    return parametersForNetwork.get(network);
  }
}
