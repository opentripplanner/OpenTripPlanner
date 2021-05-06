package org.opentripplanner.standalone.config.sandbox;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.ext.bikerentalservicedirectory.api.BikeRentalServiceDirectoryFetcherParameters;

public class BikeRentalServiceDirectoryFetcherConfig {
  public static BikeRentalServiceDirectoryFetcherParameters create(NodeAdapter c) {

    if(c.isEmpty()) { return null; }

    return new BikeRentalServiceDirectoryFetcherParameters(
        c.asUri("url"),
        c.asText("sourcesName", "systems"),
        c.asText("updaterUrlName", "url"),
        c.asText("updaterNetworkName", "id")
    );
  }
}
