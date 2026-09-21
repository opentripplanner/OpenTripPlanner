package org.opentripplanner.ext.fares.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.transit.model.basic.Money;

class FareProductTest {

  static ZonedDateTime ZDT = OffsetDateTime.parse("2023-03-27T10:44:54+02:00").toZonedDateTime();

  static RiderCategory CATEGORY = RiderCategory.of(new FeedScopedId("1", "pensioners"))
    .withName("Pensioners")
    .build();

  static FareMedium MEDIUM = new FareMedium(new FeedScopedId("1", "app"), "App");

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(fareProduct(null, null), ZDT, "b18a083d-ee82-3c83-af07-2b8bb11bff9e"),
      Arguments.of(
        fareProduct(null, null),
        ZDT.plusHours(1),
        "2a60adcf-3e56-338a-ab7d-8407a3bc529b"
      ),
      Arguments.of(fareProduct(CATEGORY, null), ZDT, "59e781a8-ee72-3454-a7c3-960feadf85dd"),
      Arguments.of(fareProduct(CATEGORY, MEDIUM), ZDT, "17de57df-da5c-3cf3-8fee-c158559ea560")
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void instanceId(FareProduct fareProduct, ZonedDateTime startTime, String expectedInstanceId) {
    var instanceId = fareProduct.uniqueInstanceId(startTime);

    assertEquals(expectedInstanceId, instanceId);
  }

  @Test
  void equalEligibilityBothNullCategoryAndMedium() {
    var a = fareProduct(null, null);
    var b = fareProduct(null, null);
    assertTrue(a.equalEligibility(b));
  }

  @Test
  void equalEligibilitySameCategoryAndMedium() {
    var a = fareProduct(CATEGORY, MEDIUM);
    var b = fareProduct(CATEGORY, MEDIUM);
    assertTrue(a.equalEligibility(b));
  }

  @Test
  void equalEligibilitySameCategoryNullMedium() {
    var a = fareProduct(CATEGORY, null);
    var b = fareProduct(CATEGORY, null);
    assertTrue(a.equalEligibility(b));
  }

  @Test
  void equalEligibilityDifferentCategory() {
    var otherCategory = RiderCategory.of(id("students")).withName("Students").build();
    var a = fareProduct(CATEGORY, MEDIUM);
    var b = fareProduct(otherCategory, MEDIUM);
    assertFalse(a.equalEligibility(b));
  }

  @Test
  void equalEligibilityDifferentMedium() {
    var otherMedium = new FareMedium(id("card"), "Card");
    var a = fareProduct(CATEGORY, MEDIUM);
    var b = fareProduct(CATEGORY, otherMedium);
    assertFalse(a.equalEligibility(b));
  }

  @Test
  void equalEligibilityNullCategoryVsNonNull() {
    var a = fareProduct(null, MEDIUM);
    var b = fareProduct(CATEGORY, MEDIUM);
    assertFalse(a.equalEligibility(b));
  }

  @Test
  void equalEligibilityNullMediumVsNonNull() {
    var a = fareProduct(CATEGORY, null);
    var b = fareProduct(CATEGORY, MEDIUM);
    assertFalse(a.equalEligibility(b));
  }

  private static FareProduct fareProduct(RiderCategory cat, FareMedium medium) {
    return FareProduct.of(new FeedScopedId("fares", "daypass"), "day pass", Money.euros(10))
      .withCategory(cat)
      .withMedium(medium)
      .build();
  }
}
