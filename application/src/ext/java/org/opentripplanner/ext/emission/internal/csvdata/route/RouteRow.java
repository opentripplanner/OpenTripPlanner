package org.opentripplanner.ext.emission.internal.csvdata.route;

import org.opentripplanner.framework.model.Gram;

record RouteRow(String routeId, double avgCo2InGramPerKm, double avgPassengerCount) {
  private static final double KILOMETER = 1000.0;

  public Gram calculatePassengerCo2PerMeter() {
    return Gram.of(avgCo2InGramPerKm / (avgPassengerCount * KILOMETER));
  }
}
