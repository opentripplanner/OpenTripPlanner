package org.opentripplanner.ext.dataoverlay.routing;

import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.ext.dataoverlay.configuration.ParameterBinding;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class Parameter {

  private final ParameterBinding binding;
  private final double threshold;
  private final double penalty;

  public Parameter(ParameterBinding binding, double threshold, double penalty) {
    this.binding = binding;
    this.threshold = threshold;
    this.penalty = penalty;
  }

  public ParameterName getName() {
    return binding.getName();
  }

  public String getVariable() {
    return binding.getVariable();
  }

  public double getThreshold() {
    return threshold;
  }

  public double getPenalty() {
    return penalty;
  }

  public String getFormula() {
    return binding.getFormula();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(Parameter.class)
      .addObj("binding", binding)
      .addNum("threshold", threshold)
      .addNum("penalty", penalty)
      .toString();
  }
}
