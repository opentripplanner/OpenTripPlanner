package org.opentripplanner.standalone.config.sandbox;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;

public class VehicleRentalServiceDirectoryFetcherConfig {
  public static VehicleRentalServiceDirectoryFetcherParameters create(NodeAdapter c) {

    if(c.isEmpty()) { return null; }

    return new VehicleRentalServiceDirectoryFetcherParameters(
        c.asUri("url"),
        c.asText("sourcesName", "systems"),
        c.asText("updaterUrlName", "url"),
        c.asText("updaterNetworkName", "id"),
        c.asText("language", null),
        c.asMap("headers", NodeAdapter::asText)
    );
  }
}
