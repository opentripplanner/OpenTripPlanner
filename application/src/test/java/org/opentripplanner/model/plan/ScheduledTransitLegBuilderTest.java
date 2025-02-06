package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.FARE_PRODUCT_USE;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl.FareModelForTest;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;

class ScheduledTransitLegBuilderTest {

  private static final TransitAlert ALERT = TransitAlert
    .of(id("alert"))
    .withDescriptionText(I18NString.of("alert"))
    .build();
  private static final TripPattern PATTERN = TimetableRepositoryForTest
    .of()
    .pattern(TransitMode.BUS)
    .build();
  private static final LocalDate DATE = LocalDate.of(2023, 11, 15);

  @Test
  void copyZoneId() {
    var leg = completeBuilder().build();

    var newLeg = new ScheduledTransitLegBuilder<>(leg);
    assertEquals(ZoneIds.BERLIN, newLeg.zoneId());

    var withScore = newLeg.withAccessibilityScore(4f).build();

    assertEquals(ZoneIds.BERLIN, withScore.getZoneId());
  }

  @Test
  void collectionsAreInitialized() {
    var leg = completeBuilder().build();

    assertNotNull(leg.getTransitAlerts());
    assertNotNull(leg.fareProducts());
  }

  @Test
  void nullZoneId() {
    var leg = new ScheduledTransitLegBuilder<>()
      .withServiceDate(DATE)
      .withTripPattern(PATTERN)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(1);

    assertThrows(RuntimeException.class, leg::build);
  }

  @Test
  void nullCollectionsThrow() {
    assertDoesNotThrow(completeBuilder()::build);
    assertThrows(RuntimeException.class, () -> completeBuilder().withFareProducts(null).build());
    assertThrows(RuntimeException.class, () -> completeBuilder().withAlerts(null).build());
  }

  @Test
  void copyAlerts() {
    var leg = completeBuilder().withAlerts(Set.of(ALERT)).build();

    var withScore = leg.copy().withAccessibilityScore(4f).build();

    assertEquals(Set.of(ALERT), withScore.getTransitAlerts());
  }

  @Test
  void copyFareProducts() {
    var leg = completeBuilder().withFareProducts(List.of(FARE_PRODUCT_USE)).build();

    var copy = leg.copy().build();

    assertEquals(List.of(FARE_PRODUCT_USE), copy.fareProducts());
  }

  /**
   * Returns a builder where all required fields are set.
   */
  private static ScheduledTransitLegBuilder completeBuilder() {
    return new ScheduledTransitLegBuilder<>()
      .withZoneId(ZoneIds.BERLIN)
      .withServiceDate(DATE)
      .withTripPattern(PATTERN)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(1)
      .withDistanceMeters(10000);
  }
}
