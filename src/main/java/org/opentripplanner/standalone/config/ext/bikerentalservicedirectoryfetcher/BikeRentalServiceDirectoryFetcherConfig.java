package org.opentripplanner.standalone.config.ext.bikerentalservicedirectoryfetcher;

import org.opentripplanner.standalone.config.NodeAdapter;

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
