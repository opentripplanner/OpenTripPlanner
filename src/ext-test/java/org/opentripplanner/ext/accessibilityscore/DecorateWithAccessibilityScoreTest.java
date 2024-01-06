package org.opentripplanner.ext.accessibilityscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

public class DecorateWithAccessibilityScoreTest implements PlanTestConstants {

  private static final int ID = 1;

  static List<Arguments> accessibilityScoreTestCase() {
    return List.of(
      Arguments.of(
        newItinerary(A, 0)
          .walk(20, Place.forStop(TEST_MODEL.stop("1:stop", 1d, 1d).build()))
          .bus(ID, 0, 50, B)
          .bus(ID, 52, 100, C)
          .build(),
        0.5f
      ),
      Arguments.of(
        newItinerary(A, 0)
          .walk(20, Place.forStop(TEST_MODEL.stop("1:stop", 1d, 1d).build()))
          .bus(ID, 0, 50, B)
          .bus(ID, 52, 100, C)
          .build(),
        0.5f
      ),
      Arguments.of(
        newItinerary(A, 0)
          .walk(20, Place.forStop(TEST_MODEL.stop("1:stop", 1d, 1d).build()))
          .bus(ID, 0, 50, B)
          .bus(ID, 52, 100, C)
          .build(),
        0.5f
      )
    );
  }

  @ParameterizedTest
  @MethodSource("accessibilityScoreTestCase")
  public void accessibilityScoreTest(Itinerary itinerary, float expectedAccessibilityScore) {
    var filter = new DecorateWithAccessibilityScore(WheelchairPreferences.DEFAULT.maxSlope());

    filter.decorate(itinerary);

    assertEquals(expectedAccessibilityScore, itinerary.getAccessibilityScore());

    itinerary
      .getLegs()
      .forEach(l -> {
        assertNotNull(l.accessibilityScore());
      });
  }
}
