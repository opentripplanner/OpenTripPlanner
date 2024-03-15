package org.opentripplanner.ext.flex.flexpathcalculator;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.vertex.Vertex;

public class DurationFactorCalculator implements FlexPathCalculator {

  private final FlexPathCalculator delegate;
  private final float factor;
  private final Duration offset;

  public DurationFactorCalculator(FlexPathCalculator delegate, float factor, Duration offset) {
    this.delegate = delegate;
    this.factor = factor;
    this.offset = offset;
  }

  @Nullable
  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex) {
    var path = delegate.calculateFlexPath(fromv, tov, fromStopIndex, toStopIndex);

    if (path == null) {
      return null;
    } else {
      return path.withDurationFactors(factor, offset);
    }
  }
}
