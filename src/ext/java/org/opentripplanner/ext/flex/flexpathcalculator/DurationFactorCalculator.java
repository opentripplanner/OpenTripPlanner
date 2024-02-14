package org.opentripplanner.ext.flex.flexpathcalculator;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.vertex.Vertex;

public class DurationFactorCalculator implements FlexPathCalculator {

  private final FlexPathCalculator underlying;

  public DurationFactorCalculator(FlexPathCalculator underlying) {
    this.underlying = underlying;
  }

  @Nullable
  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex) {
    var path = underlying.calculateFlexPath(fromv, tov, fromStopIndex, toStopIndex);

    if (path == null) {
      return null;
    } else {
      return path.withDurationFactors(1.5f, Duration.ofMinutes(10));
    }
  }
}
