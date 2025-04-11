package org.opentripplanner.model.plan;

import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * Represents the emission of a journey. Each type of emissions has its own field and unit.
 */
@Sandbox
public class Emission {

  private Gram co2;

  public Emission(Gram co2) {
    if (co2 != null) {
      this.co2 = co2;
    }
  }

  public Gram getCo2() {
    return co2;
  }
}
