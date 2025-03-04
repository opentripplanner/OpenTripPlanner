package org.opentripplanner.ext.accessibilityscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

class DecorateWithAccessibilityScoreTest implements PlanTestConstants {

  private static final int ID = 1;
  private static final DecorateWithAccessibilityScore DECORATOR =
    new DecorateWithAccessibilityScore(WheelchairPreferences.DEFAULT.maxSlope());

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
  void accessibilityScoreTest(Itinerary itinerary, float expectedAccessibilityScore) {
    DECORATOR.decorate(itinerary);

    assertEquals(expectedAccessibilityScore, itinerary.getAccessibilityScore());

    itinerary.getLegs().forEach(l -> assertNotNull(l.accessibilityScore()));
  }

  private static List<Function<TestItineraryBuilder, TestItineraryBuilder>> nonWalkingCases() {
    return List.of(
      b -> b.bicycle(10, 20, B),
      b -> b.drive(10, 20, B),
      b -> b.rentedBicycle(10, 20, B)
    );
  }

  @MethodSource("nonWalkingCases")
  @ParameterizedTest
  void noScoreForNonWalking(Function<TestItineraryBuilder, TestItineraryBuilder> modifier) {
    var itinerary = modifier.apply(newItinerary(A, 0)).build();
    DECORATOR.decorate(itinerary);
    assertNull(itinerary.getAccessibilityScore());
    itinerary.getLegs().forEach(l -> assertNull(l.accessibilityScore()));
  }
}
