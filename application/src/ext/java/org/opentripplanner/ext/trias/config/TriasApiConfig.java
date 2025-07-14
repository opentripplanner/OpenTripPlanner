package org.opentripplanner.ext.trias.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import org.opentripplanner.ext.trias.parameters.TriasApiParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TriasApiConfig {

  public static TriasApiParameters mapParameters(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_8)
      .summary("Configuration for the TRIAS API.")
      .asObject();

    var hideFeedId = c
      .of("hideFeedId")
      .since(V2_8)
      .summary("Hide the feed id in all API output, and add it to input ids.")
      .description(
        "Only turn this feature on if you have unique ids across all feeds, without the " +
        "feedId prefix."
      )
      .asBoolean(false);
    var hardcodedInputFeedId = c
      .of("hardcodedInputFeedId")
      .since(V2_8)
      .summary("The hardcoded feedId to add to all input ids.")
      .description(
        "Only turn this feature on if you have unique ids across all feeds, without the " +
        "feedId prefix _and_ `hideFeedId` is set to `true`.`"
      )
      .asString(null);
    var timeZone = c
      .of("timeZone")
      .since(V2_8)
      .summary("If you don't want to use the feed's timezone, configure it here.")
      .description(
        """
        By default the input feed's timezone is used. However, there may be cases when you want the
        API to use a different timezone.

        **Think hard about changing the timezone! We recommend that you keep the feed's time zone and
        convert the time in the client which will make debugging OTP much easier.**
        """
      )
      .asZoneId(null);

    return new TriasApiParameters(hideFeedId, hardcodedInputFeedId, timeZone);
  }
}
