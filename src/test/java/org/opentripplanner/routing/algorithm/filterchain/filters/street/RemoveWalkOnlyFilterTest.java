package org.opentripplanner.routing.algorithm.filterchain.filters.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.plan.Itinerary;

public class RemoveWalkOnlyFilterTest {

  private static final int D5m = DurationUtils.durationInSeconds("5m");

  private static final int T10_10 = TimeUtils.hm2time(10, 10);
  private static final int T10_20 = TimeUtils.hm2time(10, 20);

  private final RemoveWalkOnlyFilter subject = new RemoveWalkOnlyFilter();

  @Test
  public void name() {
    assertEquals("remove-walk-only-filter", subject.name());
  }

  @Test
  public void filter() {
    Itinerary w1 = newItinerary(A, T10_10).walk(20, E).build();
    Itinerary w2 = newItinerary(A, T10_10).walk(20, B).walk(12, E).build();
    Itinerary t1 = newItinerary(A).bicycle(T10_10, T10_20, E).build();
    Itinerary t2 = newItinerary(A).bus(31, T10_10, T10_20, E).build();
    Itinerary t3 = newItinerary(A, T10_10).walk(D5m, B).bus(31, T10_10, T10_20, E).build();
    Itinerary t4 = newItinerary(A).bicycle(T10_10, T10_20, B).walk(D5m, E).build();

    var input = List.of(t1, w1, t2, w2, t3, t4);
    var expected = List.of(t1, t2, t3, t4);

    assertEquals(Itinerary.toStr(expected), Itinerary.toStr(subject.removeMatchesForTest(input)));
  }
}
