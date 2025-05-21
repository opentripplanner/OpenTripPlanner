package org.opentripplanner.raptor.api.debug;

public enum DebugTopic {
  /**
   * Log computed heuristic information for stops listed in the debug request stops and trip list.
   * If not stops are specified some of the first stops are listed.
   */
  HEURISTICS,

  /**
   * Log multi criteria stop arrivals statistics. Logs average and total stop arrivals for each
   * stop and number of stops visited.
   */
  STOP_ARRIVALS_STATISTICS,
}
