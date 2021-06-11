package org.opentripplanner.standalone.config.sandbox;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.ext.bikerentalservicedirectory.api.BikeRentalServiceDirectoryFetcherParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BikeRentalServiceDirectoryFetcherConfig {
  public static BikeRentalServiceDirectoryFetcherParameters create(NodeAdapter c) {

    if(c.isEmpty()) { return null; }

    Map<String, String> headers = new HashMap<>();

    List<NodeAdapter> headersConfig = c.path("headers").asList();

    for (NodeAdapter nodeAdapter : headersConfig) {
      headers.put(nodeAdapter.asText("name"), nodeAdapter.asText("value"));
    }

    return new BikeRentalServiceDirectoryFetcherParameters(
        c.asUri("url"),
        c.asText("sourcesName", "systems"),
        c.asText("updaterUrlName", "url"),
        c.asText("updaterNetworkName", "id"),
        headers
    );
  }
}
