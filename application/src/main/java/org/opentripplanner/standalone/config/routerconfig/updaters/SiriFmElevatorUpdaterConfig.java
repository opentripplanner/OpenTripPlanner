package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_11;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.elevator.siri.SiriFmElevatorUpdaterParameters;
import org.opentripplanner.updater.elevator.siri.UnknownElevatorStatus;

public class SiriFmElevatorUpdaterConfig {

  public static SiriFmElevatorUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriFmElevatorUpdaterParameters(
      configRef,
      c
        .of("url")
        .since(V2_11)
        .summary("The URL of the SIRI-FM feed with the status of the elevators.")
        .description(
          """
          The feed is polled with HTTP GET. Each response must contain the status of all the
          elevators: an elevator that is not in it is in service. The `FacilityRef` of a
          `FacilityCondition` is the id of the elevator, and must match an id of an elevator in the
          street graph, see `elevatorRefTags` in the build configuration.
          """
        )
        .asUri(),
      c
        .of("frequency")
        .since(V2_11)
        .summary("How often the feed is polled.")
        .asDuration(Duration.ofMinutes(1)),
      HttpHeadersConfig.headers(c, V2_11),
      c
        .of("unknownStatus")
        .since(V2_11)
        .summary(UnknownElevatorStatus.IN_SERVICE.typeDescription())
        .description(
          "The status `unknown` and `partiallyAvailable`, or a missing status, is unknown.\n\n" +
            docEnumValueList(UnknownElevatorStatus.values())
        )
        .asEnum(UnknownElevatorStatus.IN_SERVICE)
    );
  }
}
