package org.opentripplanner.standalone.config.routerconfig.updaters;

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
        .summary("How forwards propagation should be handled.")
        .description(
          """
            NONE:
            Do not propagate delays forwards. Reject real-time updates if not all arrival / departure times
            are specified until the end of the trip.

            Note that this will also reject all updates containing NO_DATA, or all updates containing
            SKIPPED stops without a time provided. Only use this value when you can guarantee that the
            real-time feed contains all departure and arrival times for all future stops, including
            SKIPPED stops.

            DEFAULT:
            Default value. Propagate delays forwards for stops without arrival / departure times given.

            For NO_DATA stops, the scheduled time is used unless a previous delay fouls the scheduled time
            at the stop, in such case the minimum amount of delay is propagated to make the times
            non-decreasing.

            For SKIPPED stops without time given, interpolate the estimated time using the ratio between
            scheduled and real times from the previous to the next stop.
          """
        )
        .asEnum(ForwardsDelayPropagationType.DEFAULT),
      c
        .of("backwardsDelayPropagationType")
        .since(V2_2)
        .summary("How backwards propagation should be handled.")
        .description(
          """
            NONE:
            Do not propagate delays backwards. Reject real-time updates if the times are not specified
            from the beginning of the trip.

            REQUIRED_NO_DATA:
            Default value. Only propagates delays backwards when it is required to ensure that the times
            are increasing, and it sets the NO_DATA flag on the stops so these automatically updated times
            are not exposed through APIs.

            REQUIRED:
            Only propagates delays backwards when it is required to ensure that the times are increasing.
            The updated times are exposed through APIs.

            ALWAYS:
            Propagates delays backwards on stops with no estimates regardless if it's required or not.
            The updated times are exposed through APIs.
          """
        )
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA),
      c.of("feedId").since(V1_5).summary("Which feed the updates apply to.").asString(),
      url,
      headers
    );
  }
}
