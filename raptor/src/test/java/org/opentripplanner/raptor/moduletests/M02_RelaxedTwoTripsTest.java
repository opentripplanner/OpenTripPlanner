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
 * The relaxed limited transfer search should return two trips on the same route.
 */
public class M02_RelaxedTwoTripsTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorConfig<TestTripSchedule> config = RaptorConfig.defaultConfigForTest();

  @BeforeEach
  void setup() {
    data.withRoute(
      route(pattern("R1", STOP_A, STOP_B)).withTimetable(
        schedule("00:02, 00:04"),
        schedule("00:03, 00:05")
      )
    );
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D20s))
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D10m);
    requestBuilder.withMultiCriteria(mc ->
      mc.withRelaxedLimitedTransferRequest(rlt -> rlt.withEnabled(true))
    );
  }

  @Test
  void testRelaxedLimitedTransferSearch() {
    var result = config.createRelaxedLimitedTransferSearch(data, requestBuilder.build()).route();
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:04 ~ B ~ Walk 20s [0:01:30 0:04:20 2m50s Tₓ0 C₁820]\n" +
      "Walk 30s ~ A ~ BUS R1 0:03 0:05 ~ B ~ Walk 20s [0:02:30 0:05:20 2m50s Tₓ0 C₁820]",
      pathsToString(result)
    );
  }
}
