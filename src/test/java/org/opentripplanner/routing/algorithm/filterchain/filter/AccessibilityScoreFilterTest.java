package org.opentripplanner.routing.algorithm.filterchain.filter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class AccessibilityScoreFilterTest implements PlanTestConstants {

  @Test
  public void shouldAddAccessibilityScore() {
    final int ID = 1;

    Itinerary i1 = newItinerary(A).bus(ID, 0, 50, B).bus(ID, 52, 100, C).build();
    Itinerary i2 = newItinerary(A).bus(ID, 0, 50, B).bus(ID, 52, 100, C).build();
    Itinerary i3 = newItinerary(A).bus(ID, 0, 50, B).bus(ID, 52, 100, C).build();

    List<Itinerary> input = List.of(i1, i2, i3);

    input.forEach(i -> assertNull(i.accessibilityScore));

    var filter = new AccessibilityScoreFilter();
    var filtered = filter.filter(input);

    filtered.forEach(i -> {
      assertNotNull(i.accessibilityScore);
      i.legs.forEach(l -> {
        assertNotNull(l.accessibilityScore());
      });
    });
  }
}
