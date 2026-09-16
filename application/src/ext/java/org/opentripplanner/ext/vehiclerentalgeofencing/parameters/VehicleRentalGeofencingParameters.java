package org.opentripplanner.ext.vehiclerentalgeofencing.parameters;

import java.net.URI;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;

/**
 * Configuration for the vehicle rental geofencing graph builder. These parameters exist only when
 * the sandbox is configured; there is no feature flag.
 * <p>
 * The networks to load are discovered from the GBFS manifest rather than listed here; their
 * per-network settings live in the shared {@code gbfs} section of {@code otp-config.json}.
 *
 * @param url the GBFS v3 {@code manifest.json}
 * @param language the language to request from the GBFS feeds
 * @param headers HTTP headers added to every request made by this builder
 */
public record VehicleRentalGeofencingParameters(
  URI url,
  @Nullable String language,
  HttpHeaders headers
) {
  public VehicleRentalGeofencingParameters {
    Objects.requireNonNull(url);
    if (headers == null) {
      headers = HttpHeaders.empty();
    }
  }
}
