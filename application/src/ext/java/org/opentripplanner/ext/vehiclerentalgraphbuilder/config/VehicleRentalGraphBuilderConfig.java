package org.opentripplanner.ext.vehiclerentalgraphbuilder.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import org.opentripplanner.ext.vehiclerentalgraphbuilder.parameters.VehicleRentalGraphBuilderParameters;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Maps the {@code vehicleRentalGraphBuilder} section of {@code build-config.json}.
 */
public class VehicleRentalGraphBuilderConfig {

  public static VehicleRentalGraphBuilderParameters mapConfig(
    String parameterName,
    NodeAdapter root
  ) {
    var c = root
      .of(parameterName)
      .since(V2_10)
      .summary("Load GBFS geofencing zones into the graph at build time.")
      .description(
        """
        Discovers the networks a provider publishes from a GBFS v3 `manifest.json` and, for each
        network configured with `"geofencingZones": "permanent"` in the `gbfs` section of
        `otp-config.json`, loads its geofencing zones and applies them to the street graph during
        the graph build. This moves the cost of computing zone boundaries off the runtime path.

        A network is only loaded if its GBFS feed actually publishes a `geofencing_zones` feed;
        this is checked against the feed list in `gbfs.json` before the feed is fetched.

        Vehicles and stations remain runtime data, so a vehicle rental updater is still required.

        Note: a GBFS updater configured directly under `updaters` in `router-config.json` does not
        read the shared `gbfs` section. Enabling `geofencingZones` on such an updater for a network
        that is also built here applies the zones twice.
        """
      )
      .asObject();

    if (c.isEmpty()) {
      return new VehicleRentalGraphBuilderParameters(null, null, HttpHeaders.empty());
    }

    return new VehicleRentalGraphBuilderParameters(
      c
        .of("url")
        .since(V2_10)
        .summary("URL or file path of the GBFS v3 `manifest.json`.")
        .description(
          "Can be either a remote URL (http/https) or a local file path (file://). " +
            "The manifest must conform to the GBFS v3.0 specification."
        )
        .asUri(),
      c
        .of("language")
        .since(V2_10)
        .summary("Language code requested from the GBFS feeds.")
        .asString(null),
      HttpHeaders.of(
        c
          .of("headers")
          .since(V2_10)
          .summary("HTTP headers to add to the requests. Any header key, value can be inserted.")
          .asStringMap()
      )
    );
  }
}
