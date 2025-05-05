package org.opentripplanner.ext.emission.internal.csvdata.trip;

import org.opentripplanner.framework.model.Gram;

record TripHopsRow(String tripId, String fromStopId, int fromStopSequence, Gram co2) {
  /**
   * Return the board stop posission in pattern (zero based) index, as opposed to
   * the {@code fromStopSequence} number witch start at 1.
   */
  int boardStopPosInPattern() {
    return fromStopSequence - 1;
  }
}
