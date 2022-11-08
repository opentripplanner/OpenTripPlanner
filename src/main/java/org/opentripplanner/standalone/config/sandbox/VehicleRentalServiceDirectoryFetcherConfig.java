package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;

import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VehicleRentalServiceDirectoryFetcherConfig {

  public static VehicleRentalServiceDirectoryFetcherParameters create(
    String parameterName,
    NodeAdapter root
  ) {
    var c = root
      .of(parameterName)
      .since(V2_0)
      .summary("Configuration for the vehicle rental service directory.")
      .asObject();

    if (c.isEmpty()) {
      return null;
    }

    return new VehicleRentalServiceDirectoryFetcherParameters(
      c.of("url").since(NA).summary("Endpoint for the VehicleRentalServiceDirectory").asUri(),
      c
        .of("sourcesName")
        .since(NA)
        .summary("Json tag name for updater sources.")
        .asString("systems"),
      c
        .of("updaterUrlName")
        .since(NA)
        .summary("Json tag name for endpoint urls for each source.")
        .asString("url"),
      c
        .of("updaterNetworkName")
        .since(NA)
        .summary("Json tag name for the network name for each source.")
        .asString("id"),
      c.of("language").since(NA).summary("Language code.").asString(null),
      c.of("headers").since(NA).summary("Http headers.").asStringMap()
    );
  }
}
