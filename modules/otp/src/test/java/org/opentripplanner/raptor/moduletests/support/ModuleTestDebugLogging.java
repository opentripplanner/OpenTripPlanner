package org.opentripplanner.raptor.moduletests.support;

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
 * ARRIVAL  |   LEG   | RND |  STOP |  ARRIVE  |    C1     | TRIP | DETAILS
 *  Accept  |  Access |   0 |     2 | 00:09:30 |    12 000 |      | Accepted element: Walk 30s ~ 2 (cost: 12000)
 *
 * **  RUN RAPTOR FOR MINUTE: 0:08  **
 *
 * ARRIVAL  |   LEG   | RND |  STOP |  ARRIVE  |    C1     | TRIP | DETAILS
 *   Drop   |  Access |   0 |     2 | 00:09:30 |    12 000 |      | Droped element: Walk 30s ~ 2 (cost: 12000)
 *   ->by   |  Access |   0 |     2 | 00:08:30 |    12 000 |      | ->by element: Walk 30s ~ 2 (cost: 12000)
 *  Accept  |  Access |   0 |     2 | 00:08:30 |    12 000 |      | Accepted element: Walk 30s ~ 2 (cost: 12000)
 * </pre>
 * <p>
 * You can enable debug logging from the module tests by either:
 * <ul>
 *     <li> setting the variable 'DEBUG' below to true
 *     <li> setting the environment variable 'debugRaptor' to an arbitrary value
 *     <li> setting the system property 'debugRaptor' to an arbitrary value
 * </ul>
 * <p>
 * Remember to revert to {@code DEBUG=false} before committing or use the other options.
 * <p>
 * Tip! Setting the system properties can be done on individual test in IntelliJ, or in the JUnit
 * template. Add {@code -DdebugRaptorOff} or {@code -DdebugRaptor} to the JUnit template. You can
 * easily change it when needed in the specific unit-test config, since the template is copied
 * before the first test run, and not before re-runs.
 */
public final class ModuleTestDebugLogging {

  private static final boolean DEBUG = false;

  public static void setupDebugLogging(
    TestTransitData data,
    RaptorRequestBuilder<TestTripSchedule> requestBuilder
  ) {
    // We always run with debugging enabled, but be skip logging(dryRun=true).
    // We do this to make sure the logging works for all test-cases, and do not throw exceptions.
    boolean sysDebug = System.getProperties().containsKey("debugRaptor");
    boolean envDebug = System.getenv().containsKey("debugRaptor");

    var dryRun = !(DEBUG || sysDebug || envDebug);
    data.debugToStdErr(requestBuilder, dryRun);
  }
}
