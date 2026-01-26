package org.opentripplanner.ext.fares.service.gtfs.v2;

import java.time.Duration;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimit;
import org.opentripplanner.model.plan.TransitLeg;

class TimeLimitEvaluator {

  static boolean withinTimeLimit(FareTransferRule r, TransitLeg from, TransitLeg to) {
    return r
      .timeLimit()
      .map(limit -> withinTimeLimit(limit, from, to))
      .orElse(true);
  }

  static boolean withinTimeLimit(TimeLimit limit, TransitLeg from, TransitLeg to) {
    var duration = switch (limit.type()) {
      case DEPARTURE_TO_DEPARTURE -> Duration.between(from.startTime(), to.startTime());
      case DEPARTURE_TO_ARRIVAL -> Duration.between(from.startTime(), to.endTime());
      case ARRIVAL_TO_ARRIVAL -> Duration.between(from.endTime(), to.endTime());
      case ARRIVAL_TO_DEPARTURE -> Duration.between(from.endTime(), to.startTime());
    };

    return duration.compareTo(limit.duration()) <= 0;
  }
}
