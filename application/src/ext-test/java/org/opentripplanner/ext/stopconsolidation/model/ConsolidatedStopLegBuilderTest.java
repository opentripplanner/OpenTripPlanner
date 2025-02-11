package org.opentripplanner.ext.stopconsolidation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.FARE_PRODUCT_USE;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;

class ConsolidatedStopLegBuilderTest implements PlanTestConstants {

  private static final Set<TransitAlert> ALERTS = Set.of(
    TransitAlert.of(id("alert")).withDescriptionText(I18NString.of("alert")).build()
  );
  private static final TripPattern PATTERN = TimetableRepositoryForTest
    .of()
    .pattern(TransitMode.BUS)
    .build();
  private static final ScheduledTransitLeg SCHEDULED_TRANSIT_LEG = new ScheduledTransitLegBuilder<>()
    .withZoneId(ZoneIds.BERLIN)
    .withServiceDate(LocalDate.of(2025, 1, 15))
    .withTripPattern(PATTERN)
    .withBoardStopIndexInPattern(0)
    .withDistanceMeters(1000)
    .withAlightStopIndexInPattern(1)
    .build();
  private static final List<FareProductUse> FARES = List.of(FARE_PRODUCT_USE);

  @Test
  void build() {
    var leg = new ConsolidatedStopLegBuilder(SCHEDULED_TRANSIT_LEG)
      .withFrom(E.stop)
      .withTo(F.stop)
      .build();
    assertEquals(E.stop, leg.getFrom().stop);
    assertEquals(F.stop, leg.getTo().stop);
  }

  @Test
  void copyAttributesFromConsolidatedStopLeg() {
    var leg = new ConsolidatedStopLegBuilder(SCHEDULED_TRANSIT_LEG)
      .withFrom(E.stop)
      .withTo(F.stop)
      .build();

    var copy = leg
      .copy()
      .withAccessibilityScore(4f)
      .withFareProducts(FARES)
      .withAlerts(Set.of(ALERTS))
      .build();

    assertEquals(leg.getFrom().stop, copy.getFrom().stop);
    assertEquals(leg.getTo().stop, copy.getTo().stop);
    assertEquals(Set.of(ALERTS), copy.getTransitAlerts());
    assertEquals(FARES, copy.fareProducts());
    assertEquals(ZoneIds.BERLIN, copy.getZoneId());
  }

  @Test
  void copyConsolidatedLeg() {
    var leg = new ConsolidatedStopLegBuilder(SCHEDULED_TRANSIT_LEG)
      .withFrom(E.stop)
      .withTo(F.stop)
      .withAlerts(ALERTS)
      .build();

    var copy = leg.copy().build();

    assertEquals(E.stop, copy.getFrom().stop);
    assertEquals(F.stop, copy.getTo().stop);
    assertEquals(ALERTS, copy.getTransitAlerts());
  }

  @Test
  void copyAttributesFromScheduledLeg() {
    var leg = SCHEDULED_TRANSIT_LEG
      .copy()
      .withFareProducts(FARES)
      .withAlerts(Set.of(ALERTS))
      .build();

    var copy = new ConsolidatedStopLegBuilder(leg).withFrom(C.stop).withTo(G.stop).build();

    assertEquals(C.stop, copy.getFrom().stop);
    assertEquals(G.stop, copy.getTo().stop);
    assertEquals(Set.of(ALERTS), copy.getTransitAlerts());
    assertEquals(FARES, copy.fareProducts());
    assertEquals(ZoneIds.BERLIN, copy.getZoneId());
  }
}
