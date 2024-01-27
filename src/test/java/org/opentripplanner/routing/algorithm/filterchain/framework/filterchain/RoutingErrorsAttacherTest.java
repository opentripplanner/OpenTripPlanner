package org.opentripplanner.routing.algorithm.filterchain.framework.filterchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter;
import org.opentripplanner.routing.api.response.RoutingErrorCode;

class RoutingErrorsAttacherTest implements PlanTestConstants {

  @Test
  void walkingReturnsWalkingBetterThanTransitError() {
    var walk = newItinerary(A, T11_06).walk(D12m, E).build();
    var bus = newItinerary(A).bus(21, T11_06, T11_28, E).build();
    var itins = flagAll(List.of(walk, bus));
    var errors = RoutingErrorsAttacher.computeErrors(itins, itins);
    assertEquals(1, errors.size());

    var error = errors.get(0);
    assertEquals(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, error.code);
  }

  @Test
  void cyclingDoesntReturnWalkingBetterThanTransit() {
    var bike = newItinerary(A, T11_06).bicycle(T11_05, T11_55, E).build();
    var bus = newItinerary(A).bus(21, T11_06, T11_28, E).build();
    var itins = flagAll(List.of(bike, bus));
    var errors = RoutingErrorsAttacher.computeErrors(itins, itins);
    assertEquals(0, errors.size());
  }

  public static List<Itinerary> flagAll(List<Itinerary> itineraries) {
    itineraries.forEach(i ->
      i.flagForDeletion(
        new SystemNotice(
          RemoveTransitIfStreetOnlyIsBetter.TAG,
          "This itinerary is marked as deleted."
        )
      )
    );
    return itineraries;
  }
}
