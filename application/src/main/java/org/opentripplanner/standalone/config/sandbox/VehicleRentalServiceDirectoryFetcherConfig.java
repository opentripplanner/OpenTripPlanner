package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;

import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.updaters.HttpHeadersConfig;

public class VehicleRentalServiceDirectoryFetcherConfig {

  public static VehicleRentalServiceDirectoryFetcherParameters create(
    String parameterName,
    NodeAdapter root
  ) {
    var c = root
      .of(parameterName)
      .since(V2_0)
      .summary("Configuration for the vehicle rental service directory using GBFS v3 manifest.")
      .description(
        """
        Per-network settings are configured in the shared `gbfs` section of `otp-config.json`,
        which decides both which networks are loaded and how each behaves. A network present in
        the manifest but not configured there is skipped with a warning.
        """
      )
      .asObject();

    if (c.isEmpty()) {
      return null;
    }

    return new VehicleRentalServiceDirectoryFetcherParameters(
      c
        .of("url")
        .since(V2_1)
        .summary("URL or file path to the GBFS v3 manifest.json")
        .description(
          "Can be either a remote URL (http/https) or a local file path (file://). " +
            "The manifest must conform to the GBFS v3.0 specification."
        )
        .asUri(),
      c.of("language").since(V2_1).summary("Language code for GBFS feeds.").asString(null),
      HttpHeadersConfig.headers(c, V2_1)
    );
  }
}
