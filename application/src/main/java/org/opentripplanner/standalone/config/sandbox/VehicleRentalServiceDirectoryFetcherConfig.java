package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import java.util.List;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.NetworkParameters;
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
      HttpHeadersConfig.headers(c, V2_1),
      mapNetworkParameters("networks", c)
    );
  }

  private static List<NetworkParameters> mapNetworkParameters(
    String parameterName,
    NodeAdapter root
  ) {
    return root
      .of(parameterName)
      .since(V2_4)
      .summary(
        "List all networks to include. Use \"network\": \"" +
        VehicleRentalServiceDirectoryFetcherParameters.DEFAULT_NETWORK_NAME +
        "\" to set defaults."
      )
      .description(
        """
        If no default network exists only the listed networks are used. Configure a network with
        name "{{default-network}}" to include all unlisted networks. If not present, all unlisted
        networks are dropped. Note! The values in the "{{default-network}}" are not used to set
        missing field values in networks listed.
        """.replace(
            "{{default-network}}",
            VehicleRentalServiceDirectoryFetcherParameters.DEFAULT_NETWORK_NAME
          )
      )
      .asObjects(c ->
        new NetworkParameters(
          c.of("network").since(V2_4).summary("The network name").asString(),
          c
            .of("geofencingZones")
            .since(V2_4)
            .summary("Enables geofencingZones for the given network")
            .description(
              "See the regular [GBFS documentation](../GBFS-Config.md) for more information."
            )
            .asBoolean(false),
          c
            .of("allowKeepingVehicleAtDestination")
            .since(V2_5)
            .summary("Enables `allowKeepingVehicleAtDestination` for the given network.")
            .description(
              """
              Configures if a vehicle rented from a station must be returned to another one or can
              be kept at the end of the trip.

              See the regular [GBFS documentation](../GBFS-Config.md) for more information.
              """
            )
            .asBoolean(false)
        )
      );
  }
}
