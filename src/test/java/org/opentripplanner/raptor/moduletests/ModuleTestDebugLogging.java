package org.opentripplanner.raptor.moduletests;

import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;

/**
 * The static {@code setupDebugLogging(...)} method is used to setup Raptor debugging. To enable
 * debug output set the DEBUG constant to {@code true}. The debugger will help with tracing errors
 * in the Raptor algorithm and can also be used to study how Raptor works.
 * <p/>
 * The debugger print every stop-arrival-event like ACCEPTED, DROPPED, REJECTED. The log looks like
 * this:
 * <pre>
 * **  RUN RAPTOR FOR MINUTE: 0:09  **
 *
 * ARRIVAL  |   LEG   | RND |  STOP |  ARRIVE  |    COST   | TRIP | DETAILS
 *  Accept  |  Access |   0 |     2 | 00:09:30 |    12 000 |      | Accepted element: Walk 30s ~ 2 (cost: 12000)
 *
 * **  RUN RAPTOR FOR MINUTE: 0:08  **
 *
 * ARRIVAL  |   LEG   | RND |  STOP |  ARRIVE  |    COST   | TRIP | DETAILS
 *   Drop   |  Access |   0 |     2 | 00:09:30 |    12 000 |      | Droped element: Walk 30s ~ 2 (cost: 12000)
 *   ->by   |  Access |   0 |     2 | 00:08:30 |    12 000 |      | ->by element: Walk 30s ~ 2 (cost: 12000)
 *  Accept  |  Access |   0 |     2 | 00:08:30 |    12 000 |      | Accepted element: Walk 30s ~ 2 (cost: 12000)
 * </pre>
 * <p>
 * Enable debugging by setting the DEBUG to true.
 */
public final class ModuleTestDebugLogging {

  private static final boolean DEBUG = false;

  static void setupDebugLogging(
    TestTransitData data,
    RaptorRequestBuilder<TestTripSchedule> requestBuilder
  ) {
    if (DEBUG) {
      data.debugToStdErr(requestBuilder);
    }
  }
}
