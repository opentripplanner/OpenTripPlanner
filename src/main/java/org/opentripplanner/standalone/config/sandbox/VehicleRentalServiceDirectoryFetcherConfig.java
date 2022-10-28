package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VehicleRentalServiceDirectoryFetcherConfig {

  public static VehicleRentalServiceDirectoryFetcherParameters create(NodeAdapter c) {
    if (c.isEmpty()) {
      return null;
    }

    return new VehicleRentalServiceDirectoryFetcherParameters(
      c.of("url").since(NA).summary("TODO").asUri(),
      c.of("sourcesName").since(NA).summary("TODO").asString("systems"),
      c.of("updaterUrlName").since(NA).summary("TODO").asString("url"),
      c.of("updaterNetworkName").since(NA).summary("TODO").asString("id"),
      c.of("language").since(NA).summary("TODO").asString(null),
      c.of("headers").since(NA).summary("TODO").asStringMap()
    );
  }
}
