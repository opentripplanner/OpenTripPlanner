package org.opentripplanner.raptor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.raptor.service.HeuristicToRunResolver.resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;

public class HeuristicToRunResolverTest {

  public static final boolean FWD = true;
  public static final boolean REV = true;
  public static final boolean DEST = true;
  public static final boolean EDT = true;
  public static final boolean LAT = true;
  public static final boolean WIN = true;
  public static final boolean _x_ = false;

  private boolean forward = false;
  private boolean reverse = false;

  // Request to test
  private RaptorRequest<TestTripSchedule> request;
  private String msg;

  @Test
  public void resolveHeuristicToRun() {
    // Alternatives with both EAT & LAT FALSE is skipped.
    // Either EAT or LAT is required and request is not possible to create.

    given(DEST, EDT, LAT, WIN).expect(_x_, REV);
    given(DEST, EDT, LAT, _x_).expect(_x_, REV);
    given(DEST, EDT, _x_, WIN).expect(_x_, REV);
    given(DEST, EDT, _x_, _x_).expect(_x_, REV);
    given(DEST, _x_, LAT, WIN).expect(_x_, REV);
    given(DEST, _x_, LAT, _x_).expect(_x_, REV);
    // Skip alternatives with both EAT & LAT off.
    given(_x_, EDT, LAT, WIN).expect(_x_, _x_);
    given(_x_, EDT, LAT, _x_).expect(FWD, _x_);
    given(_x_, EDT, _x_, WIN).expect(_x_, _x_);
    given(_x_, EDT, _x_, _x_).expect(FWD, _x_);
    given(_x_, _x_, LAT, WIN).expect(_x_, REV);
    given(_x_, _x_, LAT, _x_).expect(_x_, REV);
    // Skip alternatives with both EAT & LAT off.
  }

  @Test
  public void resolveHeuristicOffForNoneRangeRaptorProfile() {
    RaptorRequestBuilder<TestTripSchedule> b = new RaptorRequestBuilder<>();
    b.profile(RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME);
    // Add some dummy legs
    b.searchParams().accessPaths().add(dummyAccessEgress());
    b.searchParams().egressPaths().add(dummyAccessEgress());
    b.searchParams().earliestDepartureTime(10_000);

    resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters(
      b.build(),
      this::enableForward,
      this::enableReverse
    );
    assertFalse(forward);
    assertFalse(reverse);
  }

  private HeuristicToRunResolverTest given(boolean dest, boolean edt, boolean lat, boolean win) {
    RaptorRequestBuilder<TestTripSchedule> b = new RaptorRequestBuilder<>();
    b.profile(RaptorProfile.MULTI_CRITERIA);
    // Add some dummy legs
    b.searchParams().accessPaths().add(dummyAccessEgress());
    b.searchParams().egressPaths().add(dummyAccessEgress());
    msg = "Params:";

    if (dest) {
      msg += " DEST";
      b.enableOptimization(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
    }

    if (edt) {
      msg += " EDT";
      b.searchParams().earliestDepartureTime(10_000);
    }
    if (lat) {
      msg += " LAT";
      b.searchParams().latestArrivalTime(20_000);
    }
    if (win) {
      msg += " WIN";
      b.searchParams().searchWindowInSeconds(6_000);
    }
    request = b.build();
    return this;
  }

  private void expect(boolean forwardExpected, boolean reverseExpected) {
    forward = false;
    reverse = false;

    resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters(
      request,
      this::enableForward,
      this::enableReverse
    );
    assertEquals(forwardExpected, forward, msg + " - Forward");
    assertEquals(reverseExpected, reverse, msg + " - Reverse");
  }

  private void enableForward() {
    forward = true;
  }

  private void enableReverse() {
    reverse = true;
  }

  private RaptorAccessEgress dummyAccessEgress() {
    return TestAccessEgress.walk(1, 10);
  }
}
