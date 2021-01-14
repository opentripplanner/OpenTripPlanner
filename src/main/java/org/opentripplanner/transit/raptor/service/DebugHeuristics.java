package org.opentripplanner.transit.raptor.service;

import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.request.DebugRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.util.CompareIntArrays;
import org.opentripplanner.transit.raptor.util.IntUtils;

import static java.util.Comparator.comparingInt;
import static org.opentripplanner.transit.raptor.api.debug.DebugTopic.HEURISTICS;

/**
 * Utility class to log computed heuristic data.
 */
public class DebugHeuristics {
    // Any big negative number will do, but -1 is a legal value
    private static final int UNREACHED = -9999;

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
        if (debug.logger().isEnabled(HEURISTICS)) {
            new DebugHeuristics(aName, bName, debug).debug(h1, h2, request.searchDirection());
        }
    }

    private void log(String message) {
        logger.debug(HEURISTICS, message);
    }

    private void debug(Heuristics fwdHeur, Heuristics revHeur, SearchDirection direction) {
        log(CompareIntArrays.compare(
                "NUMBER OF TRANSFERS",
                aName, fwdHeur.bestNumOfTransfersToIntArray(UNREACHED),
                bName, revHeur.bestNumOfTransfersToIntArray(UNREACHED),
                UNREACHED,
                stops,
                comparingInt(i -> i)
        ));
        log(CompareIntArrays.compareTime(
                "TRAVEL DURATION",
                aName, fwdHeur.bestTravelDurationToIntArray(UNREACHED),
                bName, revHeur.bestTravelDurationToIntArray(UNREACHED),
                UNREACHED,
                stops,
                direction.isForward() ? comparingInt(i -> i) : (l, r) -> r - l
        ));
    }
}
