package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexConfig {

  public static final FlexConfig DEFAULT = new FlexConfig();

  private static final Duration MAX_TRANSFER_DURATION = Duration.ofMinutes(5);
  private static final Duration MAX_FLEX_TRIP_DURATION = Duration.ofMinutes(45);

  private static final Logger LOG = LoggerFactory.getLogger(FlexConfig.class);

  private final Duration maxTransferDuration;
  private final Duration maxFlexTripDuration;

  private FlexConfig() {
    maxTransferDuration = MAX_TRANSFER_DURATION;
    maxFlexTripDuration = MAX_FLEX_TRIP_DURATION;
  }

  public FlexConfig(NodeAdapter root, String parameterName) {
    var json = root
      .of(parameterName)
      .since(V2_1)
      .summary("Configuration for flex routing.")
      .asObject();

    this.maxTransferDuration =
      json
        .of("maxTransferDuration")
        .since(V2_3)
        .summary(
          "How long should a passenger be allowed to walk after getting out of a flex vehicle " +
          "and transferring to a flex or transit one."
        )
        .description(
          """
            This was mainly introduced to improve performance which is also the reason for not
            using the existing value with the same name: fixed schedule transfers are computed
            during the graph build but flex ones are calculated at request time and are more
            sensitive to slowdown.
            
            A lower value means that the routing is faster.
            """
        )
        .asDuration(MAX_TRANSFER_DURATION);

    maxFlexTripDuration =
      json
        .of("maxFlexTripDuration")
        .since(V2_3)
        .summary("How long can a non-scheduled flex trip at maximum be.")
        .description(
          "This is used for all trips which are of type `UnscheduledTrip`. The value includes " +
          "the access/egress duration to the boarding/alighting of the flex trip, as well as the " +
          "connection to the transit stop."
        )
        .asDuration(MAX_FLEX_TRIP_DURATION);
  }

  public Duration maxFlexTripDuration() {
    return maxFlexTripDuration;
  }

  public Duration maxTransferDuration() {
    return maxTransferDuration;
  }
}
