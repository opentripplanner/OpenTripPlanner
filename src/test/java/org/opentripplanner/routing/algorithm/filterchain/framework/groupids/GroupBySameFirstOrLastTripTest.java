package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class GroupBySameFirstOrLastTripTest implements PlanTestConstants {

  @Test
  public void nonTransitShouldHaveAEmptyKey() {
    GroupBySameFirstOrLastTrip group = new GroupBySameFirstOrLastTrip(
      newItinerary(A, T11_00).walk(D10m, A).build()
    );

    assertEquals(0, group.getKeySet().size());
  }

  @Test
  public void mergeBasedOnKeySetSize() {
    GroupBySameFirstOrLastTrip oneLeg = new GroupBySameFirstOrLastTrip(
      newItinerary(A).bus(11, T11_00, T11_30, D).build()
    );

    GroupBySameFirstOrLastTrip twoLeg = new GroupBySameFirstOrLastTrip(
      newItinerary(A).bus(11, T11_00, T11_10, B).bus(21, T11_20, T11_30, D).build()
    );

    // Make sure both legs is part of the key-set
    assertEquals(2, twoLeg.getKeySet().size());

    // Expect merge to return initial leg every time
    assertSame(oneLeg, oneLeg.merge(twoLeg));
    assertSame(twoLeg, twoLeg.merge(oneLeg));
    assertSame(oneLeg, oneLeg.merge(oneLeg));
  }

  @Test
  public void transitDoesNotMatchEmptyKeySet() {
    GroupBySameFirstOrLastTrip transit = new GroupBySameFirstOrLastTrip(
      newItinerary(A).bus(11, T11_00, T11_05, B).build()
    );
    GroupBySameFirstOrLastTrip nonTransit = new GroupBySameFirstOrLastTrip(
      newItinerary(A, T11_00).walk(D5m, B).build()
    );
    // Make sure transit have 1 leg in the key-set
    assertEquals(1, transit.getKeySet().size());

    assertFalse(transit.match(nonTransit));
    assertFalse(nonTransit.match(transit));
  }

  @Test
  public void twoNonTransitKeySetShouldNotMatch() {
    GroupBySameFirstOrLastTrip nonTransitA = new GroupBySameFirstOrLastTrip(
      newItinerary(A, T11_00).walk(D5m, B).build()
    );
    GroupBySameFirstOrLastTrip nonTransitB = new GroupBySameFirstOrLastTrip(
      newItinerary(A, T11_00).walk(D5m, B).build()
    );
    assertFalse(nonTransitA.match(nonTransitB));
    assertFalse(nonTransitB.match(nonTransitA));
    assertTrue(nonTransitA.match(nonTransitA));
  }

  @Test
  public void testDifferentMatchScenarions() {
    final int ID_1 = 1;
    final int ID_2 = 2;
    final int ID_3 = 3;
    final int ID_4 = 4;

    // Should match if both itineraries are identical
    assertMatch(
      newItinerary(A).bus(ID_1, T11_00, T11_05, B).build(),
      newItinerary(A).bus(ID_1, T11_00, T11_05, B).build(),
      true
    );

    // Should match if both itineraries start with same trip
    assertMatch(
      newItinerary(A).bus(ID_1, T11_00, T11_05, C).build(),
      newItinerary(A).bus(ID_1, T11_00, T11_05, B).bus(ID_2, T11_10, T11_15, C).build(),
      true
    );

    // Should match if both itineraries end with same trip
    assertMatch(
      newItinerary(A).bus(ID_1, T11_00, T11_05, C).build(),
      newItinerary(A).bus(ID_2, T11_00, T11_05, B).bus(ID_1, T11_10, T11_15, C).build(),
      true
    );

    // Should filter out non transit legs during comparison
    assertMatch(
      newItinerary(A).bus(ID_1, T11_00, T11_05, D).build(),
      newItinerary(A, T11_00).walk(D5m, B).bus(ID_1, T11_10, T11_15, C).walk(D5m, D).build(),
      true
    );

    // Should not match if legs neither start nor end with same trip
    assertMatch(
      newItinerary(A).bus(ID_1, T11_00, T11_05, B).bus(ID_2, T11_10, T11_15, C).build(),
      newItinerary(A).bus(ID_3, T11_00, T11_05, B).bus(ID_4, T11_10, T11_15, C).build(),
      false
    );

    // Should not match if legs have different service days
    final LocalDate TODAY = LocalDate.now();
    final LocalDate TOMORROW = TODAY.plusDays(1);
    assertMatch(
      newItinerary(A).bus(ID_1, T11_00, T11_05, B, TODAY).build(),
      newItinerary(A).bus(ID_1, T11_00, T11_05, B, TOMORROW).build(),
      false
    );
  }

  public void assertMatch(Itinerary i1, Itinerary i2, boolean desiredResult) {
    GroupBySameFirstOrLastTrip transitA = new GroupBySameFirstOrLastTrip(i1);
    GroupBySameFirstOrLastTrip transitB = new GroupBySameFirstOrLastTrip(i2);

    assertEquals(desiredResult, transitA.match(transitB));
    assertEquals(desiredResult, transitB.match(transitA));
  }
}
