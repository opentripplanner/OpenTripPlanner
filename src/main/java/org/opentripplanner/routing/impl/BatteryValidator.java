package org.opentripplanner.routing.impl;

import org.opentripplanner.street.search.state.State;

public class BatteryValidator {

  public static boolean wouldBatteryRunOut(Object current) {
    State state = (State) current;
    double traversedBatteryMeters = state.traversedBatteryMeters;
    double currentRangeMeters = state.currentRangeMeters;
    if (currentRangeMeters == Double.POSITIVE_INFINITY) {
      return false;
    }
    return currentRangeMeters < traversedBatteryMeters;
  }
}
