package org.opentripplanner.ext.vehiclerentalgraphbuilder.parameters;

import java.net.URI;
import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;

/**
 * Configuration for the build-time vehicle rental graph builder.
 * <p>
 * The networks to load are discovered from the GBFS manifest rather than listed here; their
 * per-network settings live in the shared {@code gbfs} section of {@code otp-config.json}.
 *
 * @param url the GBFS v3 {@code manifest.json}, or {@code null} when the sandbox is not configured
 * @param language the language to request from the GBFS feeds
 * @param headers HTTP headers added to every request made by this builder
 */
public record VehicleRentalGraphBuilderParameters(
  @Nullable URI url,
  @Nullable String language,
  HttpHeaders headers
) {
  public VehicleRentalGraphBuilderParameters {
    if (headers == null) {
      headers = HttpHeaders.empty();
    }
  }

  /** The sandbox is activated by the presence of a manifest url; there is no feature flag. */
  public boolean hasUrl() {
    return url != null;
  }
}
