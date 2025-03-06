package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.model.SearchDirection.REVERSE;
import static org.opentripplanner.raptor.api.request.Optimization.PARALLEL;
import static org.opentripplanner.raptor.api.request.Optimization.PARETO_CHECK_AGAINST_DESTINATION;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

class RaptorRequestTest {

  public static final int STOP_A = 1;
  public static final int STOP_B = 5;
  private final RaptorRequest<RaptorTripSchedule> subject = RaptorRequest.defaults()
    .mutate()
    .profile(STANDARD)
    .searchDirection(REVERSE)
    .enableOptimization(PARALLEL)
    .searchParams()
    .earliestDepartureTime(0)
    .latestArrivalTime(3600)
    .searchWindow(Duration.ofMinutes(20))
    .addAccessPaths(TestAccessEgress.free(STOP_A))
    .addEgressPaths(TestAccessEgress.free(STOP_B))
    .build();

  @Test
  void alias() {
    assertEquals("Standard-Rev-LL", subject.alias());
  }

  @Test
  void searchParams() {
    assertNotNull(subject.searchParams());
  }

  @Test
  void useConstrainedTransfers() {
    assertFalse(subject.useConstrainedTransfers());
  }

  @Test
  void testIsDynamicSearch() {
    var s = subject.mutate().searchParams().searchWindowInSeconds(0).build();
    assertFalse(s.isDynamicSearch());
    assertTrue(s.mutate().profile(MULTI_CRITERIA).build().isDynamicSearch());
    assertTrue(s.mutate().searchParams().searchWindowInSeconds(3600).build().isDynamicSearch());
  }

  @Test
  void profile() {
    assertEquals(STANDARD, subject.profile());
  }

  @Test
  void searchDirection() {
    assertEquals(REVERSE, subject.searchDirection());
  }

  @Test
  void optimizations() {
    assertEquals("[PARALLEL]", subject.optimizations().toString());
  }

  @Test
  void runInParallel() {
    assertTrue(subject.optimizationEnabled(PARALLEL));
    assertFalse(subject.optimizationEnabled(PARETO_CHECK_AGAINST_DESTINATION));
  }
}
