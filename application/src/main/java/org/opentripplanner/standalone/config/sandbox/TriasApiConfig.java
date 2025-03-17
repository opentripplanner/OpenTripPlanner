package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TriasApiConfig {

  private final boolean hideFeedId;
  private final String hardcodedInputFeedId;

  public TriasApiConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_8)
      .summary("Configuration for the TRIAS API.")
      .asObject();

    hideFeedId = c
      .of("hideFeedId")
      .since(V2_8)
      .summary("Hide the feed id in all API output, and add it to input ids.")
      .description(
        "Only turn this feature on if you have unique ids across all feeds, without the " +
        "feedId prefix."
      )
      .asBoolean(false);
    hardcodedInputFeedId = c
      .of("hardcodedInputFeedId")
      .since(V2_8)
      .summary("The hardcoded feedId to add to all input ids.")
      .description(
        "Only turn this feature on if you have unique ids across all feeds, without the " +
        "feedId prefix _and_ `hideFeedId` is set to `true`.`"
      )
      .asString(null);

    if (hideFeedId && hardcodedInputFeedId == null) {
      throw new IllegalArgumentException(
        "If `hideFeedId` is set to `true`, `hardcodedInputFeedId` must also be set."
      );
    }
  }

  public boolean hideFeedId() {
    return hideFeedId;
  }

  public String hardcodedInputFeedId() {
    return hardcodedInputFeedId;
  }
}
