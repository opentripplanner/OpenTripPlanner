package org.opentripplanner.standalone.config.routerequest;

import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class TimeCostPenaltyMapper {

  public static TimeAndCostPenalty map(NodeAdapter c) {
    return TimeAndCostPenalty.of(
      c
        .of("timePenalty")
        .summary("TODO")
        .description(TimePenalty.DOC)
        .since(OtpVersion.V2_4)
        .asString(TimeAndCostPenalty.ZERO.timePenalty().toString()),
      c
        .of("costFactor")
        .summary("TODO")
        .since(OtpVersion.V2_4)
        .asDouble(TimeAndCostPenalty.ZERO.costFactor())
    );
  }
}
