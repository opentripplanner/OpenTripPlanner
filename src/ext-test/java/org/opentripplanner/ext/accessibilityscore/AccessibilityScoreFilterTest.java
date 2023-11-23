package org.opentripplanner.ext.accessibilityscore;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

public class AccessibilityScoreFilterTest implements PlanTestConstants {

  @Test
  public void shouldAddAccessibilityScore() {
    final int ID = 1;

    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(TEST_MODEL.stop("1:stop", 1d, 1d).build()))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();
    Itinerary i2 = newItinerary(A, 0)
      .walk(20, Place.forStop(TEST_MODEL.stop("1:stop", 1d, 1d).build()))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();
    Itinerary i3 = newItinerary(A, 0)
      .walk(20, Place.forStop(TEST_MODEL.stop("1:stop", 1d, 1d).build()))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();

    List<Itinerary> input = List.of(i1, i2, i3);

    input.forEach(i -> assertNull(i.getAccessibilityScore()));

    var filter = new AccessibilityScoreFilter(WheelchairPreferences.DEFAULT.maxSlope());
    var filtered = filter.filter(input);

    filtered.forEach(i -> {
      assertNotNull(i.getAccessibilityScore());
      i
        .getLegs()
        .forEach(l -> {
          assertNotNull(l.accessibilityScore());
        });
    });
  }
}
