package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.PollingTripUpdaterParameters;

public class PollingTripUpdaterConfig {

  public static PollingTripUpdaterParameters create(String configRef, NodeAdapter c) {
    String file = null;
    String url = null;

    if (c.exist("file")) {
      file = c.of("file").since(NA).summary("The path of the GTFS-RT file.").asString();
    } else if (c.exist("url")) {
      url = c.of("url").since(NA).summary("The URL of the GTFS-RT resource.").asString();
    } else {
      throw new OtpAppException(
        "Need either 'url' or 'file' properties to configure " +
        configRef +
        " but received: " +
        c.asText()
      );
    }

    return new PollingTripUpdaterParameters(
      configRef,
      c
        .of("frequencySec")
        .since(NA)
        .summary("How often the data should be downloaded in seconds.")
        .asInt(60),
      c
        .of("fuzzyTripMatching")
        .since(NA)
        .summary("If the trips should be matched fuzzily.")
        .asBoolean(false),
      c
        .of("backwardsDelayPropagationType")
        .since(V2_2)
        .summary("How backwards propagation should be handled.")
        .description(
          """
  REQUIRED_NO_DATA:
  Default value. Only propagates delays backwards when it is required to ensure that the times
  are increasing, and it sets the NO_DATA flag on the stops so these automatically updated times
  are not exposed through APIs.
  
  REQUIRED:
  Only propagates delays backwards when it is required to ensure that the times are increasing.
  The updated times are exposed through APIs.
  
  ALWAYS
  Propagates delays backwards on stops with no estimates regardless if it's required or not.
  The updated times are exposed through APIs.
"""
        )
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA),
      c.of("feedId").since(NA).summary("Which feed the updates apply to.").asString(null),
      url,
      file
    );
  }
}
