package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

import java.util.List;

import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class RemoveBikeRentalWithMostlyWalkingTest {

  private static final int D5m = DurationUtils.duration("5m");
  private static final int D3h = DurationUtils.duration("3h");

  private static final int T10_10 = TimeUtils.hm2time(10,10);
  private static final int T10_20 = TimeUtils.hm2time(10,20);

  private final RemoveBikerentalWithMostlyWalkingFilter subject =
      new RemoveBikerentalWithMostlyWalkingFilter(0.3);

  @Test
  public void name() {
    Assertions.assertEquals("bikerental-vs-walk-filter", subject.name());
  }

  @Test
  public void filter() {
    Itinerary w1 = newItinerary(A, T10_10).walk(20, E).build();
    Itinerary t1 = newItinerary(A).bicycle(T10_10, T10_20, B).walk(D5m, E).build();
    Itinerary t2 = newItinerary(A).rentedBicycle(T10_10, T10_20, B).walk(D5m, E).build();
    Itinerary t3 = newItinerary(A).rentedBicycle(T10_10, T10_20, B).walk(D3h, E).build();

    var input = List.of(w1, t1, t2, t3);
    var expected = List.of(w1, t1, t2);

    Assertions.assertEquals(Itinerary.toStr(expected), Itinerary.toStr(subject.filter(input)));
  }

  @Test
  public void removeItineraries() {
    Assertions.assertTrue(subject.removeItineraries());
  }
}
