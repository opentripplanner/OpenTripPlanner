package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.flex.MidnightFlexIntegrationTest.STOP_A;
import static org.opentripplanner.ext.flex.MidnightFlexIntegrationTest.STOP_B;
import static org.opentripplanner.ext.flex.MidnightFlexIntegrationTest.afterMidnight;
import static org.opentripplanner.ext.flex.MidnightFlexIntegrationTest.beforeMidnight;
import static org.opentripplanner.ext.flex.MidnightFlexIntegrationTest.noon;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.request.request.TransitRequestBuilder;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * This test checks the flex works when spanning multiple service/running days and takes into account
 * the booking time and rules.
 */
public class FlexBookingIntegrationTest {

  static Graph graph;

  static TimetableRepository timetableRepository;

  static TransferRepository transferRepository;

  static RoutingService service;

  @BeforeAll
  static void setup() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
    TestOtpModel model = FlexIntegrationTestData.midnightFlexGtfs();
    graph = model.graph();
    timetableRepository = model.timetableRepository();
    transferRepository = model.transferRepository();
    service = TestServerContext.createServerContext(
      graph,
      timetableRepository,
      transferRepository,
      model.fareServiceFactory().makeFareService()
    ).routingService();
  }

  @Test
  void flexDirectSameDay() {
    var itineraries = getItineraries(noon, noon);

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-10", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-10T12:30+01:00[Europe/Rome]",
      itineraries.get(0).transitLeg(0).startTime().toString()
    );

    assertEquals(1, itineraries.get(1).legs().size());
    assertTrue(itineraries.get(1).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-11", itineraries.get(1).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T06:00+01:00[Europe/Rome]",
      itineraries.get(1).transitLeg(0).startTime().toString()
    );
  }

  @Test
  void flexDirectSameDayBooking() {
    var itineraries = getItineraries(noon, noon.plus(Duration.ofHours(2)));

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-10", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-10T14:30+01:00[Europe/Rome]",
      itineraries.get(0).transitLeg(0).startTime().toString()
    );

    assertEquals(1, itineraries.get(1).legs().size());
    assertTrue(itineraries.get(1).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-11", itineraries.get(1).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T06:00+01:00[Europe/Rome]",
      itineraries.get(1).transitLeg(0).startTime().toString()
    );
  }

  @Test
  void flexDirectPreviousDayBooking() {
    var itineraries = getItineraries(noon, noon.minus(Duration.ofHours(18)));

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-10", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-10T12:00+01:00[Europe/Rome]",
      itineraries.get(0).transitLeg(0).startTime().toString()
    );

    assertEquals(1, itineraries.get(1).legs().size());
    assertTrue(itineraries.get(1).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-11", itineraries.get(1).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T06:00+01:00[Europe/Rome]",
      itineraries.get(1).transitLeg(0).startTime().toString()
    );
  }

  @Test
  void flexDirectPreviousPreviousDayBooking() {
    var itineraries = getItineraries(noon, noon.minus(Duration.ofHours(32)));

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-10", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-10T12:00+01:00[Europe/Rome]",
      itineraries.get(0).transitLeg(0).startTime().toString()
    );

    assertEquals(1, itineraries.get(1).legs().size());
    assertTrue(itineraries.get(1).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-11", itineraries.get(1).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T06:00+01:00[Europe/Rome]",
      itineraries.get(1).transitLeg(0).startTime().toString()
    );
  }

  @Test
  void flexDirectBeforeMidnightBooking() {
    var itineraries = getItineraries(beforeMidnight, beforeMidnight.plus(Duration.ofMinutes(30)));

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-10", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T00:59+01:00[Europe/Rome]",
      itineraries.get(0).transitLeg(0).startTime().toString()
    );

    assertEquals(1, itineraries.get(1).legs().size());
    assertTrue(itineraries.get(1).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-11", itineraries.get(1).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T06:00+01:00[Europe/Rome]",
      itineraries.get(1).transitLeg(0).startTime().toString()
    );
  }

  @Test
  void flexDirectAfterMidnightBooking() {
    var itineraries = getItineraries(afterMidnight, afterMidnight.plus(Duration.ofMinutes(30)));

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-11", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T06:00+01:00[Europe/Rome]",
      itineraries.get(0).transitLeg(0).startTime().toString()
    );

    assertEquals(1, itineraries.get(1).legs().size());
    assertTrue(itineraries.get(1).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-12", itineraries.get(1).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-12T06:00+01:00[Europe/Rome]",
      itineraries.get(1).transitLeg(0).startTime().toString()
    );
  }

  @AfterAll
  static void teardown() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }

  private static List<Itinerary> getItineraries(Instant dateTime, Instant bookingTime) {
    RouteRequest request = RouteRequest.of()
      .withDateTime(dateTime)
      .withBookingTime(bookingTime)
      .withFrom(STOP_A)
      .withTo(STOP_B)
      .withNumItineraries(2)
      .withSearchWindow(Duration.ofHours(6))
      .withPreferences(p ->
        p.withStreet(s ->
          s.withAccessEgress(ae ->
            ae.withPenalty(Map.of(StreetMode.FLEXIBLE, TimeAndCostPenalty.ZERO))
          )
        )
      )
      .withJourney(journeyBuilder -> {
        journeyBuilder.withTransit(TransitRequestBuilder::disable);
        journeyBuilder.withModes(
          JourneyRequest.DEFAULT.modes()
            .copyOf()
            .withDirectMode(StreetMode.FLEXIBLE)
            .withAccessMode(StreetMode.WALK)
            .withEgressMode(StreetMode.WALK)
            .build()
        );
      })
      .buildRequest();

    var result = service.route(request);
    var itineraries = result.getTripPlan().itineraries;

    assertFalse(itineraries.isEmpty());
    return itineraries;
  }
}
