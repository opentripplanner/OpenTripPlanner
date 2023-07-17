package org.opentripplanner.raptor.service;

import static java.util.Comparator.comparingInt;
import static org.opentripplanner.raptor.api.debug.DebugTopic.HEURISTICS;
import static org.opentripplanner.raptor.api.model.RaptorConstants.N_TRANSFERS_UNREACHED;
import static org.opentripplanner.raptor.api.model.RaptorConstants.UNREACHED_HIGH;

import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.raptor.api.debug.DebugLogger;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.util.CompareIntArrays;

/**
 * Utility class to log computed heuristic data.
 */
public class DebugHeuristics {

  private final String aName;
  private final String bName;
  private final DebugLogger logger;
  private final int[] stops;

  private DebugHeuristics(String aName, String bName, DebugRequest debugRequest) {
    this.aName = aName;
    this.bName = bName;
    this.logger = debugRequest.logger();
    this.stops = IntUtils.concat(debugRequest.stops(), debugRequest.path());
  }

  public static void debug(
    String aName,
    Heuristics h1,
    String bName,
    Heuristics h2,
    RaptorRequest<?> request
  ) {
    DebugRequest debug = request.debug();
    if (debug.logger().isEnabled()) {
      new DebugHeuristics(aName, bName, debug).debug(h1, h2, request.searchDirection());
    }
  }

  private void log(String message) {
    logger.debug(HEURISTICS, message);
  }

  private void debug(Heuristics fwdHeur, Heuristics revHeur, SearchDirection direction) {
    log(
      CompareIntArrays.compare(
        "NUMBER OF TRANSFERS",
        aName,
        fwdHeur.bestNumOfTransfersToIntArray(N_TRANSFERS_UNREACHED),
        bName,
        revHeur.bestNumOfTransfersToIntArray(N_TRANSFERS_UNREACHED),
        N_TRANSFERS_UNREACHED,
        stops,
        comparingInt(i -> i)
      )
    );
    log(
      CompareIntArrays.compareTime(
        "TRAVEL DURATION",
        aName,
        fwdHeur.bestTravelDurationToIntArray(UNREACHED_HIGH),
        bName,
        revHeur.bestTravelDurationToIntArray(UNREACHED_HIGH),
        UNREACHED_HIGH,
        stops,
        direction.isForward() ? comparingInt(i -> i) : (l, r) -> r - l
      )
    );
    log(
      CompareIntArrays.compareTime(
        "GENERALIZED COST",
        aName,
        fwdHeur.bestGeneralizedCostToIntArray(UNREACHED_HIGH),
        bName,
        revHeur.bestGeneralizedCostToIntArray(UNREACHED_HIGH),
        UNREACHED_HIGH,
        stops,
        direction.isForward() ? comparingInt(i -> i) : (l, r) -> r - l
      )
    );
  }
}
