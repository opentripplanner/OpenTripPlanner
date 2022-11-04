package org.opentripplanner.routing.alertpatch;

import java.util.Collection;

public class StopConditionsHelper {

  /**
   * Returns true if at least one StopCondition in the entity selector matches the requested
   * conditions. Also returns true, if the EntitySelector or the request don't specify any stop
   * conditions.
   *
   * @param stopConditions StopConditions set of the EntitySelector
   * @param requestConditions StopConditions requested by the client/matching the usage of the stop
   *                          in a leg
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
