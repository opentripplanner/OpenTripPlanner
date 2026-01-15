package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.time.Duration;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class FlexConfig implements FlexParameters {

  private static final FlexParameters DEFAULT = FlexParameters.defaultValues();

  public static final String ACCESS_EGRESS_DESCRIPTION = """
    If you have multiple overlapping flex zones the high default value can lead to performance problems.
    A lower value means faster routing.

    Depending on your service this might be what you want to do anyway: many flex services are used
    by passengers with mobility problems so offering a long walk might be problematic. In other words,
    if you can walk 45 minutes to a flex stop/zone you're unlikely to be the target audience for those
    services.
    """;
  public static final String AREA_STOP_DESCRIPTION =
    """
    Area stops is the internal OTP name. In GTFS they are called "locations" and
    NeTEx FlexibleArea.
    """;

  private final Duration maxTransferDuration;
  private final Duration maxFlexTripDuration;
  private final Duration maxAccessWalkDuration;
  private final Duration maxEgressWalkDuration;
  private final Cost areaStopAlightCost;
  private final Cost areaStopBoardCost;

  public FlexConfig(NodeAdapter root, String parameterName) {
    var json = root
      .of(parameterName)
      .since(V2_1)
      .summary("Configuration for flex routing.")
      .asObject();

    this.maxTransferDuration = json
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
      .asDuration(DEFAULT.maxTransferDuration());

    maxFlexTripDuration = json
      .of("maxFlexTripDuration")
      .since(V2_3)
      .summary("How long can a non-scheduled flex trip at maximum be.")
      .description(
        "This is used for all trips which are of type `UnscheduledTrip`. The value includes " +
          "the access/egress duration to the boarding/alighting of the flex trip, as well as the " +
          "connection to the transit stop."
      )
      .asDuration(DEFAULT.maxFlexTripDuration());

    maxAccessWalkDuration = json
      .of("maxAccessWalkDuration")
      .since(V2_3)
      .summary(
        "The maximum duration the passenger will be allowed to walk to reach a flex stop or zone."
      )
      .description(ACCESS_EGRESS_DESCRIPTION)
      .asDuration(DEFAULT.maxAccessWalkDuration());

    maxEgressWalkDuration = json
      .of("maxEgressWalkDuration")
      .since(V2_3)
      .summary(
        "The maximum duration the passenger will be allowed to walk after leaving the flex vehicle at the final destination."
      )
      .description(ACCESS_EGRESS_DESCRIPTION)
      .asDuration(DEFAULT.maxEgressWalkDuration());

    var areaStop = json
      .of("areaStops")
      .summary(
        "Configuration properties for area stops. These stops are called 'locations' in GTFS and 'flexible stops' in NeTEx."
      )
      .asObject();
    areaStopBoardCost = Cost.costOfSeconds(
      areaStop
        .of("boardCost")
        .summary("Cost to board an area stop, in seconds.")
        .description(AREA_STOP_DESCRIPTION)
        .asInt(DEFAULT.areaStopBoardCost().toSeconds())
    );
    areaStopAlightCost = Cost.costOfSeconds(
      areaStop
        .of("alightCost")
        .summary("Cost to alight an area stop, in seconds.")
        .description(AREA_STOP_DESCRIPTION)
        .asInt(DEFAULT.areaStopAlightCost().toSeconds())
    );
  }

  public Duration maxFlexTripDuration() {
    return maxFlexTripDuration;
  }

  public Duration maxTransferDuration() {
    return maxTransferDuration;
  }

  public Duration maxAccessWalkDuration() {
    return maxAccessWalkDuration;
  }

  public Duration maxEgressWalkDuration() {
    return maxEgressWalkDuration;
  }

  @Override
  public Cost areaStopBoardCost() {
    return areaStopBoardCost;
  }

  @Override
  public Cost areaStopAlightCost() {
    return areaStopAlightCost;
  }
}
