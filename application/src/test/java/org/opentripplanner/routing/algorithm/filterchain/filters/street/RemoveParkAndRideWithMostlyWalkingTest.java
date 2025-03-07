package org.opentripplanner.routing.algorithm.filterchain.filters.street;

import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

public class RemoveParkAndRideWithMostlyWalkingTest {

  private static final int D5m = DurationUtils.durationInSeconds("5m");
  private static final int D3h = DurationUtils.durationInSeconds("3h");

  private static final int T10_10 = TimeUtils.hm2time(10, 10);
  private static final int T10_20 = TimeUtils.hm2time(10, 20);

  private final RemoveParkAndRideWithMostlyWalkingFilter subject =
    new RemoveParkAndRideWithMostlyWalkingFilter(0.5);

  @Test
  public void name() {
    Assertions.assertEquals("park-and-ride-vs-walk-filter", subject.name());
  }

  @Test
  public void filter() {
    Itinerary w1 = newItinerary(A, T10_10).walk(20, E).build();
    Itinerary t1 = newItinerary(A).drive(T10_10, T10_20, B).walk(D5m, E).build();
    Itinerary t2 = newItinerary(A).drive(T10_10, T10_20, B).walk(D5m, E).build();
    Itinerary t3 = newItinerary(A).drive(T10_10, T10_20, B).walk(D3h, E).build();

    var input = List.of(w1, t1, t2, t3);
    var expected = List.of(w1, t1, t2);

    Assertions.assertEquals(
      Itinerary.toStr(expected),
      Itinerary.toStr(subject.removeMatchesForTest(input))
    );
  }
}
