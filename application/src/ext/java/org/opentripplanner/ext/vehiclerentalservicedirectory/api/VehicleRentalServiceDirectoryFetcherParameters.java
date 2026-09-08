package org.opentripplanner.ext.vehiclerentalservicedirectory.api;

import java.net.URI;
import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;

/**
 * Parameters for fetching vehicle rental services from a GBFS v3 manifest.json file.
 * The manifest can be loaded from a remote URL or a local file path.
 * <p>
 * Per-network settings are not configured here: they live in the shared {@code gbfs} section of
 * {@code otp-config.json} and reach the fetcher as
 * {@link org.opentripplanner.gbfs.network.GbfsNetworkOverrides}.
 */
public class VehicleRentalServiceDirectoryFetcherParameters {

  private final URI url;

  private final HttpHeaders headers;

  @Nullable
  private final String language;

  public VehicleRentalServiceDirectoryFetcherParameters(
    URI url,
    @Nullable String language,
    HttpHeaders headers
  ) {
    this.url = url;
    this.language = language;
    this.headers = headers;
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

  @Nullable
  public String getLanguage() {
    return language;
  }
}
