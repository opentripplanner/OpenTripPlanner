package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class OutsideSearchWindowFilterTest implements PlanTestConstants {

  @Test
  public void filterOnLatestDepartureTime() {
    // Given:
    Itinerary it = newItinerary(A).bus(32, 0, 60, E).build();
    Instant time = it.firstLeg().getStartTime().toInstant();

    // When:
    assertTrue(
      DeletionFlaggerTestHelper
        .process(List.of(it), new OutsideSearchWindowFilter(time.minusSeconds(1)))
        .isEmpty()
    );
    assertFalse(
      DeletionFlaggerTestHelper.process(List.of(it), new OutsideSearchWindowFilter(time)).isEmpty()
    );
  }
}
