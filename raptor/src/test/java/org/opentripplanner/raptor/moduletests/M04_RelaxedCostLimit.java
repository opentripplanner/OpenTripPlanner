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
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * The relaxed limited transfer search should only return trips in the search window
 */
public class M04_RelaxedCostLimit implements RaptorTestConstants {

  private final RaptorConfig<TestTripSchedule> config = RaptorConfig.defaultConfigForTest();

  ///  Expensive trips should be included if they are optimal on arrival or departure
  @Test
  void testIncludeExpensive() {
    var data = new TestTransitData();
    data.withRoute(
        route(pattern("FAST", STOP_A, STOP_B)).withTimetable(
          schedule("01:00, 01:10")
        ))
      .withRoute(
        route(pattern("SLOW", STOP_A, STOP_B)).withTimetable(
          schedule("00:05, 01:05"),
          schedule("01:05, 02:05")
        )
      );

    var result = config.createRelaxedLimitedTransferSearch(data, createRequest()).route();
    assertEquals(
      "A ~ BUS SLOW 0:05 1:05 ~ B [0:05 1:05 1h Tₓ0 C₁4_200]\n" +
        "A ~ BUS FAST 1:00 1:10 ~ B [1:00 1:10 10m Tₓ0 C₁1_200]\n" +
        "A ~ BUS SLOW 1:05 2:05 ~ B [1:05 2:05 1h Tₓ0 C₁4_200]",
      pathsToString(result)
    );
  }

  ///  Expensive trips should be rejected
  @Test
  void testRejectExpensive() {
    var data = new TestTransitData();
    data.withRoute(
        route(pattern("FAST", STOP_A, STOP_B)).withTimetable(
          schedule("01:00, 01:10")
        ))
      .withRoute(
        route(pattern("SLOWER", STOP_A, STOP_B)).withTimetable(
          schedule("01:00, 01:29")
        )
      )
      .withRoute(
        route(pattern("SLOWEST", STOP_A, STOP_B)).withTimetable(
          schedule("01:00, 01:30")
        )
      );

    var result = config.createRelaxedLimitedTransferSearch(data, createRequest()).route();
    assertEquals(
      "A ~ BUS FAST 1:00 1:10 ~ B [1:00 1:10 10m Tₓ0 C₁1_200]\n" +
        "A ~ BUS SLOWER 1:00 1:29 ~ B [1:00 1:29 29m Tₓ0 C₁2_340]",
      pathsToString(result)
    );
  }

  private RaptorRequest<TestTripSchedule> createRequest() {
    RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.free(STOP_A))
      .addEgressPaths(TestAccessEgress.free(STOP_B))
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D24h);
    requestBuilder.withMultiCriteria(mc ->
      mc.withRelaxedLimitedTransferRequest(rlt ->
        rlt.withEnabled(true)
          .withCostRelaxFunction(GeneralizedCostRelaxFunction.of(2))
      )
    );
    return requestBuilder.build();
  }
}
