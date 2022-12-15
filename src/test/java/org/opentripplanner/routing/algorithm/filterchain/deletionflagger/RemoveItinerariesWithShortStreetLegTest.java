package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;

class RemoveItinerariesWithShortStreetLegTest implements PlanTestConstants {

  RemoveItinerariesWithShortStreetLeg filter = new RemoveItinerariesWithShortStreetLeg(
    500,
    BICYCLE
  );

  @Test
  void noBikeDoesNothing() {
    var regularTransit = newItinerary(A).bus(30, T11_16, T11_20, C).build();

    var result = filter.filter(List.of(regularTransit, regularTransit, regularTransit));
    assertEquals(List.of(regularTransit, regularTransit, regularTransit), result);
  }

  @Test
  void justBikeDoesNothing() {
    var itin = newItinerary(A).bicycle(T11_05, T11_06, B).build();
    assertEquals(300, itin.getLegs().get(0).getDistanceMeters());
    var result = filter.filter(List.of(itin));
    assertEquals(List.of(itin), result);
  }

  @Test
  void zeroMinDoesNothing() {
    var filter = new RemoveItinerariesWithShortStreetLeg(0, BICYCLE);
    var itin = newItinerary(A).bicycle(T11_05, T11_06, B).rail(30, T11_16, T11_20, C).build();
    assertEquals(300, itin.getLegs().get(0).getDistanceMeters());
    var result = filter.filter(List.of(itin));
    assertEquals(List.of(itin), result);
  }

  @Test
  void shortBike() {
    var itin = newItinerary(A).bicycle(T11_05, T11_06, B).rail(30, T11_16, T11_20, C).build();
    assertEquals(300, itin.getLegs().get(0).getDistanceMeters());
    var result = filter.filter(List.of(itin, itin, itin));
    assertEquals(List.of(), result);
  }

  @Test
  void longBike() {
    var itin = newItinerary(A).bicycle(T11_05, T11_30, B).rail(30, T11_33, T11_50, C).build();
    assertEquals(7500, itin.getLegs().get(0).getDistanceMeters());
    var result = filter.filter(List.of(itin, itin));
    assertEquals(List.of(itin, itin), result);
  }
}
