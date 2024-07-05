package org.opentripplanner.model.plan;

import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.framework.model.Grams;

/**
 * Represents the emissions of a journey. Each type of emissions has its own field and unit.
 */
@Sandbox
public class Emissions {

  private Grams co2;

  public Emissions(Grams co2) {
    if (co2 != null) {
      this.co2 = co2;
    }
  }

  public Grams getCo2() {
    return co2;
  }
}
