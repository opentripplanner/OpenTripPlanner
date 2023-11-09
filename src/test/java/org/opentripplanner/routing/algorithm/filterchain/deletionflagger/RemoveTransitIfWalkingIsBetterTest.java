package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

public class RemoveTransitIfWalkingIsBetterTest implements PlanTestConstants {

  @Test
  public void filterAwayNothingIfNoWalking() {
    // Given:
    Itinerary i1 = newItinerary(A).bus(21, 6, 7, E).build();
    Itinerary i2 = newItinerary(A).rail(110, 6, 9, E).build();

    // When:
    List<Itinerary> result = DeletionFlaggerTestHelper.process(
      List.of(i1, i2),
      new RemoveTransitIfWalkingIsBetterFilter(CostLinearFunction.of(Cost.ZERO, 1.0))
    );

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

    List<Itinerary> result = DeletionFlaggerTestHelper.process(
      List.of(i1, i2, bicycle, walk),
      new RemoveTransitIfWalkingIsBetterFilter(CostLinearFunction.of(Cost.ZERO, 1.0))
    );

    assertEquals(toStr(List.of(i2, bicycle, walk)), toStr(result));
  }
  // TODO: 2023-11-09 Write tests for cases other than 0 + 1.0x
}
