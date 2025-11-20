package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * The relaxed limited transfer search should only return trips in the search window
 */
public class M03_RelaxedSearchWindow implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorConfig<TestTripSchedule> config = RaptorConfig.defaultConfigForTest();

  @BeforeEach
  void setup() {
    data.withRoute(
      route(pattern("R1", STOP_A, STOP_B)).withTimetable(
        schedule("00:02, 00:03"),
        schedule("00:03, 00:04"),
        schedule("00:04, 00:05"),
        schedule("00:05, 00:06")
      )
    );
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1m))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D1m))
      .earliestDepartureTime(T00_02)
      .searchWindowInSeconds(D1m);
    requestBuilder.withMultiCriteria(mc ->
      mc.withRelaxedLimitedTransferRequest(rlt -> rlt.withEnabled(true))
    );
  }

  @Test
  void testRelaxedSearchWindow() {
    var result = config.createRelaxedLimitedTransferSearch(data, requestBuilder.build()).route();
    assertEquals(
      "Walk 1m ~ A ~ BUS R1 0:03 0:04 ~ B ~ Walk 1m [0:02 0:05 3m Tₓ0 C₁900]\n" +
      "Walk 1m ~ A ~ BUS R1 0:04 0:05 ~ B ~ Walk 1m [0:03 0:06 3m Tₓ0 C₁900]",
      pathsToString(result)
    );
  }
}
