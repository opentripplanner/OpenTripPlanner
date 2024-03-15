package org.opentripplanner.ext.flex.flexpathcalculator;

import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.trip.FlexDurationFactors;
import org.opentripplanner.street.model.vertex.Vertex;

public class DurationFactorCalculator implements FlexPathCalculator {

  private final FlexPathCalculator delegate;
  private final FlexDurationFactors factors;

  public DurationFactorCalculator(FlexPathCalculator delegate, FlexDurationFactors factors) {
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
      return path.withDurationFactors(factors);
    }
  }
}
