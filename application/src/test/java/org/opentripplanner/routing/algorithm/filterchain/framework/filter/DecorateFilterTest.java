package org.opentripplanner.routing.algorithm.filterchain.framework.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;

class DecorateFilterTest implements ItineraryDecorator, PlanTestConstants {

  private static final Itinerary i1 = newItinerary(A, 6).walk(1, B).build();
  private static final Itinerary i2 = newItinerary(A).bicycle(6, 8, B).build();

  private Iterator<Itinerary> expectedQueue;

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    if (expectedQueue == null) {
      fail("The expected queue is null, this method should not be called.");
    }

    if (!expectedQueue.hasNext()) {
      fail("No itineraries in list of expected, but filter is still processing: " + itinerary);
    }

    var current = expectedQueue.next();
    assertEquals(current, itinerary);

    return itinerary;
  }

  @Test
  void filterEmptyList() {
    // Make sure the filter does nothing and does not crash on empty lists
    new DecorateFilter(this).filter(List.of());
  }

  @Test
  void filterOneElement() {
    var input = List.of(i1);
    expectedQueue = input.iterator();
    new DecorateFilter(this).filter(input);
    assertTrue(!expectedQueue.hasNext(), "All elements are processed");
  }

  @Test
  void filterTwoElements() {
    var input = List.of(i1, i2);
    expectedQueue = input.iterator();
    new DecorateFilter(this).filter(input);
    assertTrue(!expectedQueue.hasNext(), "All elements are processed");
  }
}
