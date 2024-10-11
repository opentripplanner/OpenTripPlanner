package org.opentripplanner.routing.algorithm.filterchain.filters.system.mcmax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class ItemTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Place A = TEST_MODEL.place("A", 10, 11);
  private static final Place B = TEST_MODEL.place("B", 10, 11);
  private static final Itinerary ITINERARY = newItinerary(A).bus(1, 1, 2, B).build();

  @Test
  void betterThan() {
    var i1 = new Item(ITINERARY, 3);
    var i2 = new Item(ITINERARY, 7);

    // i1 is better than i2 because the index is lower
    assertTrue(i1.betterThan(i2));
    assertFalse(i2.betterThan(i1));

    // Incrementing both does not change anything
    i1.incGroupCount();
    i2.incGroupCount();
    assertTrue(i1.betterThan(i2));
    assertFalse(i2.betterThan(i1));

    // Incrementing i2 make it better
    i2.incGroupCount();
    assertFalse(i1.betterThan(i2));
    assertTrue(i2.betterThan(i1));
  }

  @Test
  void item() {
    assertSame(ITINERARY, new Item(ITINERARY, 7).item());
  }

  @Test
  void testToString() {
    Item item = new Item(ITINERARY, 7);
    assertEquals("Item #7 {count:0, A ~ BUS 1 0:00:01 0:00:02 ~ B [C₁121]}", item.toString());
    item.incGroupCount();
    assertEquals("Item #7 {count:1, A ~ BUS 1 0:00:01 0:00:02 ~ B [C₁121]}", item.toString());
    item.decGroupCount();
    assertEquals("Item #7 {count:0, A ~ BUS 1 0:00:01 0:00:02 ~ B [C₁121]}", item.toString());
  }
}
