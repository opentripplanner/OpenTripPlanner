package org.opentripplanner.ext.vehiclerentalservicedirectory.api;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.updater.spi.HttpHeaders;

/**
 * Parameters for fetching vehicle rental services from a GBFS v3 manifest.json file.
 * The manifest can be loaded from a remote URL or a local file path.
 */
public class VehicleRentalServiceDirectoryFetcherParameters {

  public static final String DEFAULT_NETWORK_NAME = "default-network";
  private final URI url;

  private final HttpHeaders headers;

  private final String language;

  private final Map<String, NetworkParameters> parametersForNetwork;

  @Nullable
  private final NetworkParameters defaultNetwork;

  public VehicleRentalServiceDirectoryFetcherParameters(
    URI url,
    String language,
    HttpHeaders headers,
    Collection<NetworkParameters> networkParameters
  ) {
    this.url = url;
    this.language = language;
    this.headers = headers;
    this.parametersForNetwork = networkParameters
      .stream()
      .collect(Collectors.toMap(NetworkParameters::network, it -> it));
    this.defaultNetwork = parametersForNetwork.get(DEFAULT_NETWORK_NAME);
  }

  /**
   * URL or file path to the GBFS v3 manifest.json
   * <p>
   * This is required. Can be either:
   * - A remote URL (http/https)
   * - A local file path (file://)
   */
  public URI getUrl() {
    return url;
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

  public Optional<NetworkParameters> networkParameters(String network) {
    return Optional.ofNullable(parametersForNetwork.getOrDefault(network, defaultNetwork));
  }
}
