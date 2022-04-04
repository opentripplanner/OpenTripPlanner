package org.opentripplanner.model.plan.legreference;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

/**
 * A reference which can be used to rebuild an exact copy of a {@link ScheduledTransitLeg} using the
 * {@Link RoutingService}
 */
public record ScheduledTransitLegReference(
  FeedScopedId trip,
  ServiceDate serviceDate,
  int fromStopPositionInPattern,
  int toStopPositionInPattern
)
  implements LegReference {}
