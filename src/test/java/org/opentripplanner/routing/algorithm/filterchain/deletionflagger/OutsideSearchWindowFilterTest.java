package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.PlanTestConstants;

public class OutsideSearchWindowFilterTest implements PlanTestConstants {

  @Test
  public void filterOnLatestDepartureTime() {
    var it = newItinerary(A).bus(32, 0, 60, E).build();
    var input = List.of(it);
    var time = it.firstLeg().getStartTime().toInstant();

    // Verify itinerary is removed if limit is exceeded
    var subject = new OutsideSearchWindowFilter(time.minusSeconds(1));
    var result = subject.flagForRemoval(List.of(it));

    assertEquals(input, result);

    // Verify itinerary is NOT removed if time is within limit
    subject = new OutsideSearchWindowFilter(time);
    result = subject.flagForRemoval(input);

    assertEquals(List.of(), result);
  }

  @Test
  public void testTaggedBy() {
    var it = newItinerary(A).bus(32, 0, 60, E).build();
    assertFalse(OutsideSearchWindowFilter.taggedBy(it));

    it.flagForDeletion(new SystemNotice(OutsideSearchWindowFilter.TAG, "Text"));
    assertTrue(OutsideSearchWindowFilter.taggedBy(it));
  }
}
