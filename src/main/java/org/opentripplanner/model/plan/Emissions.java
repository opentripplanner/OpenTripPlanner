package org.opentripplanner.model.plan;

import org.opentripplanner.framework.lang.Sandbox;

@Sandbox
public class Emissions {

  private Double co2Grams;

  public Double getCo2Grams() {
    return co2Grams;
  }

  public void setCo2Grams(Double co2Grams) {
    this.co2Grams = co2Grams;
  }
}
