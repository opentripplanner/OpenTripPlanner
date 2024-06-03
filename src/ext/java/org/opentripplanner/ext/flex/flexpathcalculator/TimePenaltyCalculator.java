package org.opentripplanner.ext.flex.flexpathcalculator;

import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A calculator to delegates the main computation to another instance and applies a time penalty
 * afterward.
 */
public class TimePenaltyCalculator implements FlexPathCalculator {

  private final FlexPathCalculator delegate;
  private final TimePenalty penalty;

  public TimePenaltyCalculator(FlexPathCalculator delegate, TimePenalty penalty) {
    this.delegate = delegate;
    this.penalty = penalty;
  }

  @Nullable
  @Override
  public FlexPath calculateFlexPath(
    Vertex fromv,
    Vertex tov,
    int boardStopPosition,
    int alightStopPosition
  ) {
    var path = delegate.calculateFlexPath(fromv, tov, boardStopPosition, alightStopPosition);

    if (path == null) {
      return null;
    } else {
      return path.withTimePenalty(penalty);
    }
  }
}
