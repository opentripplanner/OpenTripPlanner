package org.opentripplanner.model.plan;

import org.opentripplanner.framework.lang.Sandbox;

@Sandbox
public class Emissions {

  private Double co2grams;

  public Double getCo2grams() {
    return co2grams;
  }

  public void setCo2grams(Double co2grams) {
    this.co2grams = co2grams;
  }
}
