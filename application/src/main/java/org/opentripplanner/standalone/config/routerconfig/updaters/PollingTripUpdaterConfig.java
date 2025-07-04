package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V1_5;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.ForwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.updater.http.PollingTripUpdaterParameters;

public class PollingTripUpdaterConfig {

  public static PollingTripUpdaterParameters create(String configRef, NodeAdapter c) {
    var url = c
      .of("url")
      .since(V1_5)
      .summary("The URL of the GTFS-RT resource.")
      .description(
        "`file:` URLs are also supported if you want to read a file from the local disk."
      )
      .asString();

    var headers = HttpHeadersConfig.headers(c, V2_3);

    return new PollingTripUpdaterParameters(
      configRef,
      c
        .of("frequency")
        .since(V1_5)
        .summary("How often the data should be downloaded.")
        .asDuration(Duration.ofMinutes(1)),
      c
        .of("fuzzyTripMatching")
        .since(V1_5)
        .summary("If the trips should be matched fuzzily.")
        .asBoolean(false),
      c
        .of("forwardsDelayPropagationType")
        .since(V2_8)
        .summary(ForwardsDelayPropagationType.DEFAULT.typeDescription())
        .description(docEnumValueList(ForwardsDelayPropagationType.values()))
        .asEnum(ForwardsDelayPropagationType.DEFAULT),
      c
        .of("backwardsDelayPropagationType")
        .since(V2_2)
        .summary(BackwardsDelayPropagationType.REQUIRED_NO_DATA.typeDescription())
        .description(docEnumValueList(BackwardsDelayPropagationType.values()))
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA),
      c.of("feedId").since(V1_5).summary("Which feed the updates apply to.").asString(),
      url,
      headers
    );
  }
}
