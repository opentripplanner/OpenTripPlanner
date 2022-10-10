package org.opentripplanner.ext.fares.impl;

import org.opentripplanner.model.plan.ScheduledTransitLeg;

public class CombinedInterlinedLegsFareService extends DefaultFareService {

  private final CombinationMode mode;

  public CombinedInterlinedLegsFareService(CombinationMode mode) {
    this.mode = mode;
  }

  @Override
  protected boolean shouldCombineInterlinedLegs(
    ScheduledTransitLeg previousLeg,
    ScheduledTransitLeg currentLeg
  ) {
    return switch (mode) {
      case ALWAYS -> true;
      case SAME_ROUTE -> currentLeg.getRoute().getId().equals(previousLeg.getRoute().getId());
    };
  }

  public enum CombinationMode {
    ALWAYS,
    SAME_ROUTE,
  }
}
