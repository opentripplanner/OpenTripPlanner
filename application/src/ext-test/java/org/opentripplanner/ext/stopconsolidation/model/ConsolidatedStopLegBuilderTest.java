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
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;

class ConsolidatedStopLegBuilderTest implements PlanTestConstants {

  private static final Set<TransitAlert> ALERTS = Set.of(
    TransitAlert.of(id("alert")).withDescriptionText(I18NString.of("alert")).build()
  );
  private static final TripPattern PATTERN = TimetableRepositoryForTest.of()
    .pattern(TransitMode.BUS)
    .build();
  private static final ScheduledTransitLeg SCHEDULED_TRANSIT_LEG =
    new ScheduledTransitLegBuilder<>()
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
    assertEquals(E.stop, leg.from().stop);
    assertEquals(F.stop, leg.to().stop);
  }

  @Test
  void copyAttributesFromConsolidatedStopLeg() {
    var leg = new ConsolidatedStopLegBuilder(SCHEDULED_TRANSIT_LEG)
      .withFrom(E.stop)
      .withTo(F.stop)
      .build();

    var copy = leg
      .copyOf()
      .withAccessibilityScore(4f)
      .withFareProducts(FARES)
      .withAlerts(Set.of(ALERTS))
      .build();

    assertEquals(leg.from().stop, copy.from().stop);
    assertEquals(leg.to().stop, copy.to().stop);
    assertEquals(Set.of(ALERTS), copy.listTransitAlerts());
    assertEquals(FARES, copy.fareProducts());
    assertEquals(ZoneIds.BERLIN, copy.zoneId());
  }

  @Test
  void copyConsolidatedLeg() {
    var leg = new ConsolidatedStopLegBuilder(SCHEDULED_TRANSIT_LEG)
      .withFrom(E.stop)
      .withTo(F.stop)
      .withAlerts(ALERTS)
      .build();

    var copy = leg.copyOf().build();

    assertEquals(E.stop, copy.from().stop);
    assertEquals(F.stop, copy.to().stop);
    assertEquals(ALERTS, copy.listTransitAlerts());
  }

  @Test
  void copyAttributesFromScheduledLeg() {
    var leg = SCHEDULED_TRANSIT_LEG.copyOf()
      .withFareProducts(FARES)
      .withAlerts(Set.of(ALERTS))
      .build();

    var copy = new ConsolidatedStopLegBuilder(leg).withFrom(C.stop).withTo(G.stop).build();

    assertEquals(C.stop, copy.from().stop);
    assertEquals(G.stop, copy.to().stop);
    assertEquals(Set.of(ALERTS), copy.listTransitAlerts());
    assertEquals(FARES, copy.fareProducts());
    assertEquals(ZoneIds.BERLIN, copy.zoneId());
  }
}
