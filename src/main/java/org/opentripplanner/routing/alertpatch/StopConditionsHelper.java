package org.opentripplanner.routing.alertpatch;

import java.util.Collection;

public class StopConditionsHelper {

  /**
   * Matches an EntitySelector's stopConditions with the stopConditions required. Returns true
   * if at least one StopCondition  matches the requirement
   *
   * @param stopConditions StopConditions set for EntitySelector
   * @param requestConditions StopConditions to compare with
   * @return true if stopConditions match
   */
  public static boolean matchesStopCondition(
    Collection<StopCondition> stopConditions,
    Collection<StopCondition> requestConditions
  ) {
    if (stopConditions == null || stopConditions.isEmpty()) {
      //No StopConditions are set - return true
      return true;
    }
    if (requestConditions == null || requestConditions.isEmpty()) {
      //No StopConditions to filter on - return true
      return true;
    }
    for (StopCondition requestCondition : requestConditions) {
      if (stopConditions.contains(requestCondition)) {
        // Return true on first match
        return true;
      }
    }
    return false;
  }

  public static boolean matchesStopCondition(
    EntitySelector entitySelector,
    Collection<StopCondition> requestConditions
  ) {
    if (entitySelector instanceof EntitySelector.Stop stop) {
      return matchesStopCondition(stop.stopConditions(), requestConditions);
    } else if (entitySelector instanceof EntitySelector.StopAndRoute stopAndRoute) {
      return matchesStopCondition(stopAndRoute.stopConditions(), requestConditions);
    } else if (entitySelector instanceof EntitySelector.StopAndTrip stopAndTrip) {
      return matchesStopCondition(stopAndTrip.stopConditions(), requestConditions);
    }
    // Always return true when no StopConditions exist
    return true;
  }
}
