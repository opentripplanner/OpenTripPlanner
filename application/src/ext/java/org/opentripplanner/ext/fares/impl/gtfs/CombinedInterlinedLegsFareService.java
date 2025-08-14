package org.opentripplanner.ext.fares.impl.gtfs;

import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;

/**
 * This calculator allows you to configure if two interlined legs (those with a stay-seated transfer
 * in between them) should be treated as a single one for the purpose of fare calculation.
 * <p>
 * It's maintained by IBI.
 */
public class CombinedInterlinedLegsFareService extends DefaultFareService {

  private final CombinationMode mode;

  public CombinedInterlinedLegsFareService(CombinationMode mode) {
    this.mode = mode;
  }

  public CombinationMode mode() {
    return mode;
  }

  @Override
  protected boolean shouldCombineInterlinedLegs(
    ScheduledTransitLeg previousLeg,
    ScheduledTransitLeg currentLeg
  ) {
    return switch (mode) {
      case ALWAYS -> true;
      case SAME_ROUTE -> currentLeg.route().getId().equals(previousLeg.route().getId());
    };
  }

  public enum CombinationMode {
    ALWAYS,
    SAME_ROUTE,
  }
}
