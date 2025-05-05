package org.opentripplanner.ext.emission.model;

import java.util.Locale;

/**
 * Produce a report of imported emissions.
 */
public class EmissionSummary {

  private final int routeTotal;
  private final int tripTotal;

  public EmissionSummary(int routeTotal, int tripTotal) {
    this.routeTotal = routeTotal;
    this.tripTotal = tripTotal;
  }

  @Override
  public String toString() {
    return String.format(
      Locale.ROOT,
      "Emission Summary - route: %,d / trip: %,d / total: %,d",
      routeTotal,
      tripTotal,
      total()
    );
  }

  private int total() {
    return routeTotal + tripTotal;
  }
}
