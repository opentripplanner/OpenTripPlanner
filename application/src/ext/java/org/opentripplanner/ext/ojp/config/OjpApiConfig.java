package org.opentripplanner.ext.ojp.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_9;

import org.opentripplanner.ext.ojp.parameters.OjpApiParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class OjpApiConfig {

  public static OjpApiParameters mapParameters(String parameterName, NodeAdapter root) {
    var c = root.of(parameterName).since(V2_9).summary("Configuration for the OJP API.").asObject();

    var hideFeedId = c
      .of("hideFeedId")
      .since(V2_9)
      .summary("Hide the feed id in all API output, and add it to input ids.")
      .description(
        "Only turn this feature on if you have unique ids across all feeds, without the " +
          "feedId prefix."
      )
      .asBoolean(false);
    var hardcodedInputFeedId = c
      .of("hardcodedInputFeedId")
      .since(V2_9)
      .summary("The hardcoded feedId to add to all input ids.")
      .description(
        "Only turn this feature on if you have unique ids across all feeds, without the " +
          "feedId prefix _and_ `hideFeedId` is set to `true`.`"
      )
      .asString(null);

    return new OjpApiParameters(hideFeedId, hardcodedInputFeedId);
  }
}
