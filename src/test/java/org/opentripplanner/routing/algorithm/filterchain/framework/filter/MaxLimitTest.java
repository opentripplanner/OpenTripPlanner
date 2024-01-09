package org.opentripplanner.routing.algorithm.filterchain.framework.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class MaxLimitTest implements PlanTestConstants {

  private static final Itinerary i1 = newItinerary(A, 6).walk(1, B).build();
  private static final Itinerary i2 = newItinerary(A).bicycle(6, 8, B).build();
  private static final Itinerary i3 = newItinerary(A).bus(21, 6, 8, B).build();

  @Test
  public void name() {
    MaxLimit subject = new MaxLimit("Test", 3);
    assertEquals("Test", subject.name());
  }

  @Test
  public void testNormalFilterMaxLimit3() {
    MaxLimit subject = new MaxLimit("Test", 3);
    List<Itinerary> itineraries = List.of(i1, i2, i3);
    assertEquals(toStr(itineraries), toStr(subject.removeMatchesForTest(itineraries)));
  }

  @Test
  public void testNormalFilterMaxLimit1() {
    MaxLimit subject = new MaxLimit("Test", 1);
    List<Itinerary> itineraries = List.of(i1, i2, i3);
    assertEquals(toStr(List.of(i1)), toStr(subject.removeMatchesForTest(itineraries)));
  }

  @Test
  public void testNormalFilterMaxLimit0() {
    MaxLimit subject = new MaxLimit("Test", 0);
    List<Itinerary> itineraries = List.of(i1, i2, i3);
    var result = subject.removeMatchesForTest(itineraries);
    assertEquals(toStr(List.of()), toStr(result));
  }
}
