package org.opentripplanner.model.plan;

import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.framework.model.Grams;

@Sandbox
public class Emissions {

  private Grams co2;

  public Grams getCo2() {
    return co2;
  }

  public void setCo2(Grams co2) {
    this.co2 = co2;
  }
}
