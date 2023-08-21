package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

public class RemoveTransitIfStreetOnlyIsBetterFilterTest implements PlanTestConstants {

  @Test
  public void filterAwayNothingIfNoWalking() {
    // Given:
    Itinerary i1 = newItinerary(A).bus(21, 6, 7, E).build();
    Itinerary i2 = newItinerary(A).rail(110, 6, 9, E).build();

    // When:
    List<Itinerary> result = DeletionFlaggerTestHelper.process(
      List.of(i1, i2),
      new RemoveTransitIfStreetOnlyIsBetterFilter(
        CostLinearFunction.of(Duration.ofSeconds(200), 1.2)
      )
    );

    // Then:
    assertEquals(toStr(List.of(i1, i2)), toStr(result));
  }

  @Test
  public void filterAwayLongTravelTimeWithoutWaitTime() {
    // Given: a walk itinerary with high cost - do not have any effect on filtering
    Itinerary walk = newItinerary(A, 6).walk(1, E).build();
    walk.setGeneralizedCost(300);

    // Given: a bicycle itinerary with low cost - transit with clearly higher cost are removed
    Itinerary bicycle = newItinerary(A).bicycle(6, 8, E).build();
    bicycle.setGeneralizedCost(200);

    // transit with almost equal cost should not be dropped
    Itinerary i1 = newItinerary(A).bus(21, 6, 8, E).build();
    i1.setGeneralizedCost(220);

    // transit with considerably higher cost will be dropped
    Itinerary i2 = newItinerary(A).bus(31, 6, 8, E).build();
    i2.setGeneralizedCost(360);

    // When:
    List<Itinerary> result = DeletionFlaggerTestHelper.process(
      List.of(i2, bicycle, walk, i1),
      new RemoveTransitIfStreetOnlyIsBetterFilter(
        CostLinearFunction.of(Duration.ofSeconds(60), 1.2)
      )
    );

    // Then:
    assertEquals(toStr(List.of(bicycle, walk, i1)), toStr(result));
  }
}
