package org.opentripplanner.ext.stopconsolidation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.ext.fares.model.FareModelForTest.ANY_FARE_OFFER;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.TripOnDateDataFetcher;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

class ConsolidatedStopLegBuilderTest implements PlanTestConstants {

  private static final ZonedDateTime TIME = ZonedDateTime.parse("2025-06-25T08:33:36+02:00");
  private static final Set<TransitAlert> ALERTS = Set.of(
    TransitAlert.of(id("alert")).withDescriptionText(I18NString.of("alert")).build()
  );
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private static final TransitTestEnvironment ENV = ENV_BUILDER.addTrip(
    TripInput.of("test-trip")
      .addStop(ENV_BUILDER.stop("S0", b -> b.withCoordinate(60.0, 10.0)), "08:00", "08:00")
      .addStop(ENV_BUILDER.stop("S1", b -> b.withCoordinate(60.0, 10.01)), "08:05", "08:05")
      .addStop(ENV_BUILDER.stop("S2", b -> b.withCoordinate(60.0, 10.02)), "08:10", "08:10")
  ).build();
  private static final TripOnDateDataFetcher TRIP_DATA = ENV.tripData("test-trip");
  private static final TripPattern PATTERN = TRIP_DATA.tripPattern();
  private static final TripTimes TRIP_TIMES = TRIP_DATA.scheduledTripTimes();
  private static final ScheduledTransitLeg SCHEDULED_TRANSIT_LEG =
    new ScheduledTransitLegBuilder<>()
      .withTripTimes(TRIP_TIMES)
      .withTripPattern(PATTERN)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(1)
      .withStartTime(TIME)
      .withEndTime(TIME)
      .withServiceDate(TIME.toLocalDate())
      .withZoneId(ZoneIds.BERLIN)
      .build();
  private static final List<FareOffer> FARES = List.of(ANY_FARE_OFFER);

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
    assertEquals(FARES, copy.fareOffers());
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
    assertEquals(FARES, copy.fareOffers());
    assertEquals(ZoneIds.BERLIN, copy.zoneId());
  }
}
