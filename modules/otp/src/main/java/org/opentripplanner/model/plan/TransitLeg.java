package org.opentripplanner.model.plan;

import org.opentripplanner.transit.model.basic.TransitMode;

public interface TransitLeg extends Leg {
  /**
   * The mode (e.g., <code>BUS</code>) used when traversing this leg.
   */
  TransitMode getMode();

  @Override
  default boolean isTransitLeg() {
    return true;
  }

  @Override
  default boolean hasSameMode(Leg other) {
    return other instanceof TransitLeg trLeg && getMode().equals(trLeg.getMode());
  }
}
