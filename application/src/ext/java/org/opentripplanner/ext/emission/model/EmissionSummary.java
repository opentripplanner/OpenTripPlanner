package org.opentripplanner.ext.emission.model;

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
    return "Emission Summary - route: %,d / trip: %,d / total: %,d".formatted(
        routeTotal,
        tripTotal,
        total()
      );
  }

  private int total() {
    return routeTotal + tripTotal;
  }
}
