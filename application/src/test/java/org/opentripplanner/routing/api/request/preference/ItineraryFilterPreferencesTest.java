package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

class ItineraryFilterPreferencesTest {

  private static final boolean ACCESSIBILITY_SCORE = true;
  private static final double BIKE_RENTAL_DISTANCE_RATIO = 0.37;
  private static final ItineraryFilterDebugProfile DEBUG = ItineraryFilterDebugProfile.LIST_ALL;
  private static final boolean FILTER_ITINERARIES_WITH_SAME_FIRST_OR_LAST_TRIP = true;
  private static final double GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER = 4.0;
  private static final double GROUP_SIMILARITY_KEEP_ONE = 0.8;
  private static final double GROUP_SIMILARITY_KEEP_THREE = 0.5;
  private static final double MIN_BIKE_PARKING_DISTANCE = 2000.0;
  private static final CostLinearFunction NON_TRANSIT_GENERALIZED_COST_LIMIT =
    CostLinearFunction.of(Duration.ofSeconds(4), 5.0);
  private static final double PARK_AND_RIDE_DURATION_RATIO = 0.44;
  private static final TransitGeneralizedCostFilterParams TRANSIT_GENERALIZED_COST_LIMIT =
    new TransitGeneralizedCostFilterParams(CostLinearFunction.of(Duration.ofSeconds(4), 5.0), 3.0);
  private static final CostLinearFunction TRANSIT_BEST_STREET_COST_LIMIT = CostLinearFunction.of(
    Duration.ofSeconds(30),
    1.3
  );

  private final ItineraryFilterPreferences subject = ItineraryFilterPreferences.of()
    .withAccessibilityScore(ACCESSIBILITY_SCORE)
    .withBikeRentalDistanceRatio(BIKE_RENTAL_DISTANCE_RATIO)
    .withDebug(DEBUG)
    .withFilterItinerariesWithSameFirstOrLastTrip(FILTER_ITINERARIES_WITH_SAME_FIRST_OR_LAST_TRIP)
    .withGroupedOtherThanSameLegsMaxCostMultiplier(GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER)
    .withGroupSimilarityKeepOne(GROUP_SIMILARITY_KEEP_ONE)
    .withGroupSimilarityKeepThree(GROUP_SIMILARITY_KEEP_THREE)
    .withMinBikeParkingDistance(MIN_BIKE_PARKING_DISTANCE)
    .withNonTransitGeneralizedCostLimit(NON_TRANSIT_GENERALIZED_COST_LIMIT)
    .withParkAndRideDurationRatio(PARK_AND_RIDE_DURATION_RATIO)
    .withTransitGeneralizedCostLimit(TRANSIT_GENERALIZED_COST_LIMIT)
    .withRemoveTransitWithHigherCostThanBestOnStreetOnly(TRANSIT_BEST_STREET_COST_LIMIT)
    .build();

  @Test
  void accessibilityScore() {
    assertEquals(ACCESSIBILITY_SCORE, subject.useAccessibilityScore());
  }

  @Test
  void bikeRentalDistanceRatio() {
    assertEquals(BIKE_RENTAL_DISTANCE_RATIO, subject.bikeRentalDistanceRatio());
  }

  @Test
  void debug() {
    assertEquals(DEBUG, subject.debug());
  }

  @Test
  void filterItinerariesWithSameFirstOrLastTrip() {
    assertEquals(
      FILTER_ITINERARIES_WITH_SAME_FIRST_OR_LAST_TRIP,
      subject.filterItinerariesWithSameFirstOrLastTrip()
    );
  }

  @Test
  void groupedOtherThanSameLegsMaxCostMultiplier() {
    assertEquals(
      GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER,
      subject.groupedOtherThanSameLegsMaxCostMultiplier()
    );
  }

  @Test
  void groupSimilarityKeepOne() {
    assertEquals(GROUP_SIMILARITY_KEEP_ONE, subject.groupSimilarityKeepOne());
  }

  @Test
  void groupSimilarityKeepThree() {
    assertEquals(GROUP_SIMILARITY_KEEP_THREE, subject.groupSimilarityKeepThree());
  }

  @Test
  void minBikeParkingDistance() {
    assertEquals(MIN_BIKE_PARKING_DISTANCE, subject.minBikeParkingDistance());
  }

  @Test
  void nonTransitGeneralizedCostLimit() {
    assertEquals(NON_TRANSIT_GENERALIZED_COST_LIMIT, subject.nonTransitGeneralizedCostLimit());
  }

  @Test
  void parkAndRideDurationRatio() {
    assertEquals(PARK_AND_RIDE_DURATION_RATIO, subject.parkAndRideDurationRatio());
  }

  @Test
  void transitGeneralizedCostLimit() {
    assertEquals(TRANSIT_GENERALIZED_COST_LIMIT, subject.transitGeneralizedCostLimit());
  }

  @Test
  void removeTransitWithHigherCostThanBestOnStreetOnly() {
    assertEquals(
      TRANSIT_BEST_STREET_COST_LIMIT,
      subject.removeTransitWithHigherCostThanBestOnStreetOnly()
    );
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(StreetPreferences.DEFAULT, StreetPreferences.of().build());
    assertEquals(subject, subject.copyOf().build());
    assertSame(subject, subject.copyOf().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withGroupSimilarityKeepOne(0.95).build();
    var same = other
      .copyOf()
      .withGroupSimilarityKeepOne(GROUP_SIMILARITY_KEEP_ONE)
      .withFilterDirectFlexBySearchWindow(true)
      .build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals(
      "ItineraryFilterPreferences{filterDirectFlexBySearchWindow}",
      ItineraryFilterPreferences.DEFAULT.toString()
    );
    assertEquals(
      "ItineraryFilterPreferences{" +
      "accessibilityScore, " +
      "bikeRentalDistanceRatio: 0.37, " +
      "debug: LIST_ALL, " +
      "filterItinerariesWithSameFirstOrLastTrip, " +
      "groupedOtherThanSameLegsMaxCostMultiplier: 4.0, " +
      "groupSimilarityKeepOne: 0.8, " +
      "groupSimilarityKeepThree: 0.5, " +
      "minBikeParkingDistance: 2,000.0, " +
      "nonTransitGeneralizedCostLimit: 4s + 5.0 t, " +
      "parkAndRideDurationRatio: 0.44, " +
      "transitGeneralizedCostLimit: TransitGeneralizedCostFilterParams[costLimitFunction=4s + 5.0 t, intervalRelaxFactor=3.0], " +
      "removeTransitWithHigherCostThanBestOnStreetOnly: 30s + 1.30 t, " +
      "filterDirectFlexBySearchWindow" +
      "}",
      subject.toString()
    );
  }
}
