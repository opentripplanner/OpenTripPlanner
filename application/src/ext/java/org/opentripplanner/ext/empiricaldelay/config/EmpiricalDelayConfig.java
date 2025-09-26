package org.opentripplanner.ext.empiricaldelay.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import java.util.List;
import org.opentripplanner.ext.empiricaldelay.parameters.EmpiricalDelayFeedParameters;
import org.opentripplanner.ext.empiricaldelay.parameters.EmpiricalDelayParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping empirical delay configuration into parameters.
 */
public class EmpiricalDelayConfig {

  public static EmpiricalDelayParameters mapEmissionsConfig(
    String parameterName,
    NodeAdapter root
  ) {
    var c = root.of(parameterName).since(V2_8).summary("Empirical delay configuration.").asObject();
    return EmpiricalDelayParameters.of().addFeeds(mapFeeds(c)).build();
  }

  private static List<EmpiricalDelayFeedParameters> mapFeeds(NodeAdapter c) {
    return c
      .of("feeds")
      .since(V2_8)
      .summary("List of feeds.")
      .asObjects(List.of(), EmpiricalDelayConfig::mapFeed);
  }

  private static EmpiricalDelayFeedParameters mapFeed(NodeAdapter c) {
    return new EmpiricalDelayFeedParameters(
      c
        .of("feedId")
        .since(V2_8)
        .summary("Specify the feed id to use for matching transit ids in input data.")
        .asString(),
      c.of("source").since(V2_8).summary("Specify the feed data source url.").asUri()
    );
  }
}
