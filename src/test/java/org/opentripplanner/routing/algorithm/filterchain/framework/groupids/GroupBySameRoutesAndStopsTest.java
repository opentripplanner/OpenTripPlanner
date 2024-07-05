package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.network.Route;

class GroupBySameRoutesAndStopsTest implements PlanTestConstants {

  Route routeA = TransitModelForTest.route("A").build();
  Route routeB = TransitModelForTest.route("B").build();

  Itinerary i1 = newItinerary(A)
    .bus(routeA, 21, T11_06, T11_28, E)
    .bus(routeB, 41, T11_30, T11_32, D)
    .build();
  Itinerary i2 = newItinerary(A)
    .bus(routeA, 22, T11_09, T11_30, E)
    .bus(routeB, 42, T11_32, T11_33, D)
    .build();
  Itinerary i3 = newItinerary(A)
    .bus(routeA, 23, T11_10, T11_32, E)
    .bus(routeB, 43, T11_33, T11_50, D)
    .build();
  Itinerary withWalking = newItinerary(F, T11_00)
    .walk(D10m, A)
    .bus(routeA, 23, T11_10, T11_32, E)
    .bus(routeB, 43, T11_33, T11_50, D)
    .build();

  Itinerary i4 = newItinerary(A).bus(routeA, 31, T11_09, T11_20, D).build();
  Itinerary i5 = newItinerary(A).bus(routeB, 31, T11_09, T11_20, D).build();

  Itinerary walkOnly = newItinerary(A, T11_00).walk(D10m, B).build();

  @Test
  public void shouldMatchTimeshiftedDuplicates() {
    var first = new GroupBySameRoutesAndStops(i1);
    var second = new GroupBySameRoutesAndStops(i2);
    var third = new GroupBySameRoutesAndStops(i3);
    var walking = new GroupBySameRoutesAndStops(withWalking);

    assertTrue(first.match(first));

    assertTrue(first.match(second));
    assertTrue(second.match(first));

    assertTrue(first.match(third));
    assertTrue(third.match(first));

    assertTrue(third.match(second));
    assertTrue(second.match(third));

    assertTrue(walking.match(first));
  }

  @Test
  public void shouldOnlyMatchTransit() {
    var first = new GroupBySameRoutesAndStops(i1);
    var second = new GroupBySameRoutesAndStops(walkOnly);

    assertFalse(first.match(second));
    assertFalse(second.match(first));
  }

  @Test
  public void shouldNotMatch() {
    var first = new GroupBySameRoutesAndStops(i1);
    var second = new GroupBySameRoutesAndStops(i4);

    assertFalse(first.match(second));
    assertFalse(second.match(first));
  }

  @Test
  public void differentRoute() {
    var first = new GroupBySameRoutesAndStops(i4);
    var second = new GroupBySameRoutesAndStops(i5);

    assertFalse(first.match(second));
    assertFalse(second.match(first));
  }

  @Test
  public void merge() {
    var first = new GroupBySameRoutesAndStops(i1);
    var second = new GroupBySameRoutesAndStops(i4);

    assertSame(first, first.merge(second));
  }
}
