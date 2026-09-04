package org.opentripplanner.ext.taxizone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.util.List;
import org.opentripplanner.ext.taxizone.parameters.TaxiZoneFeedParameters;
import org.opentripplanner.ext.taxizone.parameters.TaxiZoneParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping taxi zone JSON configuration into parameters.
 */
public class TaxiZoneConfig {

  public static TaxiZoneParameters mapTaxiZoneConfig(String parameterName, NodeAdapter root) {
    var c = root.of(parameterName).since(V2_10).summary("Taxi zone configuration.").asObject();
    return TaxiZoneParameters.of().addFeeds(mapFeeds(c)).build();
  }

  private static List<TaxiZoneFeedParameters> mapFeeds(NodeAdapter c) {
    return c
      .of("feeds")
      .since(V2_10)
      .summary("List of taxi zone feeds.")
      .asObjects(List.of(), TaxiZoneConfig::mapFeed);
  }

  private static TaxiZoneFeedParameters mapFeed(NodeAdapter c) {
    return new TaxiZoneFeedParameters(
      c
        .of("feedId")
        .since(V2_10)
        .summary("Specify the feed id to use for matching transit ids in the taxi zone data.")
        .asString(),
      c.of("source").since(V2_10).summary("Specify the feed data source url.").asUri()
    );
  }
}
