package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class RemoveTransitIfWalkingIsBetterTest implements PlanTestConstants {

  @Test
  public void filterAwayNothingIfNoWalking() {
    // Given:
    Itinerary i1 = newItinerary(A).bus(21, 6, 7, E).build();
    Itinerary i2 = newItinerary(A).rail(110, 6, 9, E).build();

    // When:
    List<Itinerary> result = new RemoveTransitIfWalkingIsBetter()
      .removeMatchesForTest(List.of(i1, i2));

    // Then:
    assertEquals(toStr(List.of(i1, i2)), toStr(result));
  }

  @Test
  public void filterAwayTransitWithLongerWalk() {
    // a walk itinerary
    Itinerary walk = newItinerary(A, 6).walk(D2m, E).build();

    // a bicycle itinerary will not be filtered
    Itinerary bicycle = newItinerary(A).bicycle(6, 9, E).build();

    // transit which has more walking as plain walk should be dropped
    Itinerary i1 = newItinerary(A, 6).walk(D3m, D).bus(1, 9, 10, E).build();

    // transit which has less walking than plain walk should be kept
    Itinerary i2 = newItinerary(A, 6).walk(D1m, B).bus(2, 7, 10, E).build();

    List<Itinerary> result = new RemoveTransitIfWalkingIsBetter()
      .removeMatchesForTest(List.of(i1, i2, bicycle, walk));

    assertEquals(toStr(List.of(i2, bicycle, walk)), toStr(result));
  }
}
