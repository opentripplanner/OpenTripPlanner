package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.GenericLocation;
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
 * This test checks the flex works when spanning multiple service/running days.
 */
public class MidnightFlexIntegrationTest {

  public static final GenericLocation STOP_A = GenericLocation.fromStopId(
    "A",
    "flex-midnight",
    "A"
  );
  public static final GenericLocation STOP_B = GenericLocation.fromStopId(
    "A",
    "flex-midnight",
    "B"
  );
  static Instant noon = ZonedDateTime.parse("2026-02-10T12:00:00+01:00[Europe/Rome]").toInstant();
  static Instant beforeMidnight = ZonedDateTime.parse(
    "2026-02-10T23:59:00+01:00[Europe/Rome]"
  ).toInstant();
  static Instant afterMidnight = ZonedDateTime.parse(
    "2026-02-11T00:01:00+01:00[Europe/Rome]"
  ).toInstant();

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
    var itineraries = getItineraries(noon);

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
  void flexDirectBeforeMidnight() {
    var itineraries = getItineraries(beforeMidnight);

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-10", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-10T23:59+01:00[Europe/Rome]",
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
  void flexDirectAfterMidnight() {
    var itineraries = getItineraries(afterMidnight);

    assertEquals(2, itineraries.size());
    assertEquals(1, itineraries.get(0).legs().size());
    assertTrue(itineraries.get(0).transitLeg(0).isFlexibleTrip());
    assertEquals("2026-02-10", itineraries.get(0).transitLeg(0).serviceDate().toString());
    assertEquals(
      "2026-02-11T00:01+01:00[Europe/Rome]",
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

  @AfterAll
  static void teardown() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }

  private static List<Itinerary> getItineraries(Instant dateTime) {
    RouteRequest request = RouteRequest.of()
      .withDateTime(dateTime)
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
