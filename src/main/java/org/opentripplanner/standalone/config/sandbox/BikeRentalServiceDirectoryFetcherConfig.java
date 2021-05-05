package org.opentripplanner.standalone.config.sandbox;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.ext.bikerentalservicedirectory.api.BikeRentalServiceDirectoryFetcherParameters;

public class BikeRentalServiceDirectoryFetcherConfig {
  public static BikeRentalServiceDirectoryFetcherParameters create(NodeAdapter c) {
    return new BikeRentalServiceDirectoryFetcherParameters(
        c.asUri("url", null),
        c.asText("sourcesName", "systems"),
        c.asText("updaterUrlName", "url"),
        c.asText("updaterNetworkName", "id")
    );
  }
}
