package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;

class RemoveItinerariesWithShortStreetLegTest implements PlanTestConstants {

  private final RemoveItinerariesWithShortStreetLeg subject =
    new RemoveItinerariesWithShortStreetLeg(500, BICYCLE);

  @Test
  void noBikeDoesNothing() {
    var regularTransit = newItinerary(A).bus(30, T11_16, T11_20, C).build();

    assertFalse(subject.shouldBeFlaggedForRemoval().test(regularTransit), regularTransit.toStr());
  }

  @Test
  void justBikeDoesNothing() {
    var itin = newItinerary(A).bicycle(T11_05, T11_06, B).build();
    assertEquals(300, itin.getLegs().get(0).getDistanceMeters());

    assertFalse(subject.shouldBeFlaggedForRemoval().test(itin), itin.toStr());
  }

  @Test
  void zeroMinDoesNothing() {
    var filter = new RemoveItinerariesWithShortStreetLeg(0, BICYCLE);
    var itin = newItinerary(A).bicycle(T11_05, T11_06, B).rail(30, T11_16, T11_20, C).build();
    assertEquals(300, itin.getLegs().get(0).getDistanceMeters());

    assertFalse(filter.shouldBeFlaggedForRemoval().test(itin), itin.toStr());
  }

  @Test
  void shortBike() {
    var itin = newItinerary(A).bicycle(T11_05, T11_06, B).rail(30, T11_16, T11_20, C).build();
    assertEquals(300, itin.getLegs().get(0).getDistanceMeters());

    assertTrue(subject.shouldBeFlaggedForRemoval().test(itin), itin.toStr());
  }

  @Test
  void longBike() {
    var itin = newItinerary(A).bicycle(T11_05, T11_30, B).rail(30, T11_33, T11_50, C).build();
    assertEquals(7500, itin.getLegs().get(0).getDistanceMeters());

    assertFalse(subject.shouldBeFlaggedForRemoval().test(itin), itin.toStr());
  }
}
