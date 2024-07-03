package org.opentripplanner.routing.impl;

import java.util.Optional;
import org.opentripplanner.street.search.state.State;

public class BatteryValidator {

  public static boolean wouldBatteryRunOut(Object current) {
    double drivenBatteryMeters =
      ((org.opentripplanner.street.search.state.State) current).drivenBatteryMeters;
    Optional<Double> currentRangeMeters = ((State) current).currentRangeMeters;
    if (currentRangeMeters.isEmpty()) {
      return false;
    }
    return currentRangeMeters.get() < drivenBatteryMeters;
  }
}
