package org.opentripplanner.ext.flex.flexpathcalculator;

import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.trip.DurationModifier;
import org.opentripplanner.street.model.vertex.Vertex;

public class DurationModifierCalculator implements FlexPathCalculator {

  private final FlexPathCalculator delegate;
  private final DurationModifier factors;

  public DurationModifierCalculator(FlexPathCalculator delegate, DurationModifier factors) {
    this.delegate = delegate;
    this.factors = factors;
  }

  @Nullable
  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex) {
    var path = delegate.calculateFlexPath(fromv, tov, fromStopIndex, toStopIndex);

    if (path == null) {
      return null;
    } else {
      return path.withDurationModifier(factors);
    }
  }
}
