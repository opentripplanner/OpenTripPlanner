package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.algorithm.filterchain.framework.groupids.GroupByDistance.calculateTotalDistance;
import static org.opentripplanner.routing.algorithm.filterchain.framework.groupids.GroupByDistance.createKeySetOfLegsByLimit;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.PlanTestConstants;

public class GroupByDistanceTest implements PlanTestConstants {

  @Test
  public void calculateTotalDistanceTest() {
    Itinerary i = newItinerary(A)
      .bus(21, T11_01, T11_02, B)
      .walk(D2m, C)
      .bus(31, T11_05, T11_07, D)
      .build();

    Leg l1 = i.legs().get(0);
    Leg l2 = i.legs().get(1);
    Leg l3 = i.legs().get(2);

    // 3 minutes on a bus
    double expectedDistanceRidingABus = BUS_SPEED * 3 * 60;
    // 2 minute walking
    double expectedDistanceWalking = WALK_SPEED * 2 * 60;
    // total
    double expectedDistance = expectedDistanceRidingABus + expectedDistanceWalking;

    assertEquals(expectedDistanceRidingABus, calculateTotalDistance(List.of(l1, l3)), 0.001);
    assertEquals(expectedDistanceWalking, calculateTotalDistance(List.of(l2)), 0.001);
    assertEquals(expectedDistance, calculateTotalDistance(List.of(l1, l2, l3)), 0.001);
  }

  @Test
  public void getKeySetOfLegsByLimitTest() {
    Itinerary i = newItinerary(A)
      // 5 min bus ride
      .bus(11, T11_00, T11_05, B)
      // 2 min buss ride
      .bus(21, T11_10, T11_12, C)
      // 3 min buss ride
      .bus(31, T11_20, T11_23, D)
      .build();

    Leg l1 = i.legs().get(0);
    Leg l2 = i.legs().get(1);
    Leg l3 = i.legs().get(2);

    double d1 = l1.distanceMeters();
    double d3 = l3.distanceMeters();

    // These test relay on the internal sort by distance, which make the implementation
    // a bit simpler, but strictly is not something the method grantees
    assertEquals(List.of(l1), createKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 - 0.01));
    assertEquals(List.of(l1, l3), createKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 + 0.01));
    assertEquals(List.of(l1, l3), createKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 + d3 - 0.01));
    assertEquals(
      List.of(l1, l3, l2),
      createKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 + d3 + 0.01)
    );
  }

  @Test
  public void mainStreetModeAsKeyInStreatOnlyItinerary() {
    var itinerary = newItinerary(A, T11_00).bicycle(T11_00, T11_10, A).walk(D5m, B).build();
    var subject = new GroupByDistance(itinerary, 0.5);

    assertEquals(1, subject.size());
    assertEquals(itinerary.legs().get(0), subject.getKeySet().get(0));

    itinerary = newItinerary(A, T11_00).bicycle(T11_00, T11_02, A).walk(D12m, B).build();
    subject = new GroupByDistance(itinerary, 0.5);

    assertEquals(1, subject.size());
    assertEquals(itinerary.legs().get(1), subject.getKeySet().get(0));
  }

  @Test
  public void shortTransitMustTakeWalkLegsIntoAccount() {
    // walk 30 minutes, bus 1 minute => walking account for more than 50% of the distance

    // TEST ACCESS
    var itinerary = newItinerary(A, T11_00).walk(D10m, A).bus(11, T11_32, T11_33, B).build();
    var subject = new GroupByDistance(itinerary, 0.5);

    // The walk leg is the main part of the itinerary
    assertEquals(1, subject.size());
    assertSame(itinerary.legs().get(0), subject.getKeySet().get(0));

    // TEST EGRESS
    itinerary = newItinerary(A, T11_00).bus(11, T11_32, T11_33, B).walk(D10m, A).build();
    subject = new GroupByDistance(itinerary, 0.5);

    // The walk leg is the main part of the itinerary
    assertEquals(1, subject.size());
    assertSame(itinerary.legs().get(1), subject.getKeySet().get(0));
  }

  @Test
  public void mergeBasedOnKeySetSize() {
    var oneLeg = new GroupByDistance(newItinerary(A).bus(11, T11_00, T11_30, D).build(), 0.8);
    var twoLegs = new GroupByDistance(
      newItinerary(A).bus(11, T11_00, T11_10, B).bus(21, T11_20, T11_30, D).build(),
      0.8
    );

    // Make sure both legs is part of the key-set
    assertEquals(2, twoLegs.size());

    // Expect merge to return oneLeg every time
    assertSame(oneLeg, oneLeg.merge(twoLegs));
    assertSame(oneLeg, twoLegs.merge(oneLeg));
    assertSame(oneLeg, oneLeg.merge(oneLeg));
  }

  @Test
  public void matchDifferentTransitKeySet() {
    var g_11 = new GroupByDistance(newItinerary(A).bus(11, T11_00, T11_05, E).build(), 0.9);
    var g_21 = new GroupByDistance(newItinerary(A).bus(21, T11_00, T11_05, E).build(), 0.9);
    var g_11_21 = new GroupByDistance(
      newItinerary(A).bus(11, T11_00, T11_03, D).bus(21, T11_04, T11_06, E).build(),
      0.9
    );
    var g_31_11 = new GroupByDistance(
      newItinerary(A).bus(31, T11_01, T11_03, B).bus(11, T11_04, T11_06, E).build(),
      0.9
    );

    // Match itself
    assertTrue(g_11.match(g_11));
    // Match other with suffix leg
    assertTrue(g_11.match(g_11_21));
    assertTrue(g_11_21.match(g_11));
    // Match other with prefix leg
    assertTrue(g_11.match(g_31_11));
    assertTrue(g_31_11.match(g_11));

    // Do not match
    assertFalse(g_11.match(g_21));
    assertFalse(g_21.match(g_11));
    assertFalse(g_11_21.match(g_31_11));
    assertFalse(g_31_11.match(g_11_21));
  }

  @Test
  public void matchDifferentStreetModes() {
    var gAccessWalk = new GroupByDistance(
      newItinerary(A, T11_00).walk(D12m, A).bus(11, T11_32, T11_33, B).build(),
      0.9
    );
    var gAccessBicycle = new GroupByDistance(
      newItinerary(A, T11_00).bicycle(T11_00, T11_10, A).bus(11, T11_32, T11_33, B).build(),
      0.9
    );
    var gEgressWalk = new GroupByDistance(
      newItinerary(A, T11_00).bus(11, T11_32, T11_33, B).walk(D12m, A).build(),
      0.9
    );
    var gWalkTwice = new GroupByDistance(
      newItinerary(A, T11_00).walk(D10m, A).bus(11, T11_32, T11_33, B).walk(D10m, A).build(),
      0.9
    );

    // Access and egress do not overlap in time
    assertFalse(gAccessWalk.match(gEgressWalk));
    assertFalse(gEgressWalk.match(gAccessWalk));

    // Assert different street modes do not match: Bicycle != Walk
    assertFalse(gAccessWalk.match(gAccessBicycle));
    assertFalse(gAccessBicycle.match(gAccessWalk));

    // One walk leg matches two walk legs
    assertTrue(gAccessWalk.match(gWalkTwice));
    assertTrue(gWalkTwice.match(gAccessWalk));
  }

  @Test
  public void notMatchFrequencyTripsWithDifferentStartTime() {
    var g_11_00 = new GroupByDistance(
      newItinerary(A).frequencyBus(11, T11_00, T11_05, B).build(),
      0.9
    );
    var g_11_10 = new GroupByDistance(
      newItinerary(A).frequencyBus(11, T11_10, T11_15, B).build(),
      0.9
    );

    // Match itself
    assertTrue(g_11_00.match(g_11_00));
    // Match other with suffix leg
    assertFalse(g_11_00.match(g_11_10));
    assertFalse(g_11_10.match(g_11_00));
  }

  @Test
  public void illegalRangeForPUpperBound() {
    assertThrows(IllegalArgumentException.class, () ->
      new GroupByDistance(newItinerary(A).bus(21, T11_01, T11_02, E).build(), 0.991)
    );
  }

  @Test
  public void illegalRangeForPLowerBound() {
    assertThrows(IllegalArgumentException.class, () ->
      new GroupByDistance(newItinerary(A).bus(21, T11_01, T11_02, E).build(), 0.499)
    );
  }

  @Test
  public void testToString() {
    var itinerary = newItinerary(A, T11_00).bicycle(T11_00, T11_10, A).build();
    var subject = new GroupByDistance(itinerary, 0.5);

    assertEquals(
      "GroupByDistance{streetOnly, keySet: [StreetLeg{start: 2020-02-02T11:00:00, end: 2020-02-02T11:00:00, mode: BICYCLE}]}",
      subject.toString()
    );

    itinerary = newItinerary(A, T11_00).walk(D5m, B).bus(11, T11_10, T11_15, C).build();
    subject = new GroupByDistance(itinerary, 0.5);

    assertEquals(
      "GroupByDistance{keySet: [ScheduledTransitLeg{start: 2020-02-02T11:10:00, end: 2020-02-02T11:10:00, mode: BUS, tripId: F:11}]}",
      subject.toString()
    );
  }
}
