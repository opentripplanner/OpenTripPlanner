package org.opentripplanner.ext.realtimeresolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.refetch.RefetchItineraryServiceTest.createTransferService;
import static org.opentripplanner.routing.refetch.RefetchItineraryServiceTest.makeTransfer;
import static org.opentripplanner.routing.refetch.RefetchItineraryServiceTest.staySeated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.refetch.RefetchItineraryService;
import org.opentripplanner.routing.refetch.RefetchItineraryServiceTest;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.TripOnDateDataFetcher;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;
import org.opentripplanner.utils.time.TimeUtils;

class RealtimeResolverTest {

  // Setup transit
  private static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE
  );
  private static final RegularStop STOP_A = ENV_BUILDER.stopAtStation("A", "StationA");
  private static final RegularStop STOP_B = ENV_BUILDER.stop("B");
  private static final RegularStop STOP_C = ENV_BUILDER.stop("C");
  private static final RegularStop STOP_D = ENV_BUILDER.stop("D");
  private static final RegularStop STOP_E = ENV_BUILDER.stop("E");

  private TransitTestEnvironment transitEnv;

  @BeforeEach
  void setUp() {
    TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);

    envBuilder.stopAtStation("A", "StationA");
    envBuilder.stop("B");
    envBuilder.stop("C");
    envBuilder.stop("D");
    envBuilder.stop("E");

    transitEnv = envBuilder
      .addTrip(
        TripInput.of("trip1")
          .withWithTripOnServiceDate("trip1")
          .addStop(STOP_A, "10:00")
          .addStop(STOP_B, "11:00")
          .addStop(STOP_D, "12:00")
      )
      .addTrip(
        TripInput.of("trip2")
          .withWithTripOnServiceDate("trip2")
          .addStop(STOP_B, "12:00")
          .addStop(STOP_C, "13:00")
          .addStop(STOP_D, "14:00")
      )
      .addTrip(TripInput.of("trip3").addStop(STOP_C, "12:30").addStop(STOP_D, "13:30"))
      .addTrip(
        TripInput.of("trip4")
          .withWithTripOnServiceDate("trip4")
          .addStop(STOP_C, "08:30")
          .addStop(STOP_D, "09:30")
      )
      .addTrip(
        TripInput.of("trip5")
          .withWithTripOnServiceDate("trip5")
          .addStop(STOP_A, "11:05")
          .addStop(STOP_B, "11:20")
      )
      .addTrip(
        TripInput.of("trip6")
          .withWithTripOnServiceDate("trip6")
          .addStop(STOP_B, "11:20")
          .addStop(STOP_C, "11:40")
      )
      .addTrip(
        TripInput.of("trip7")
          .withWithTripOnServiceDate("trip7")
          .addStop(STOP_D, "15:00")
          .addStop(STOP_E, "16:00")
      )
      .build();
  }

  // Setup street
  private static final RefetchItineraryServiceTest.GraphBuilder G =
    RefetchItineraryServiceTest.GraphBuilder.of();

  private static final RefetchItineraryServiceTest.VertexRef V1 = G.vertex();
  private static final RefetchItineraryServiceTest.VertexRef VA = G.linkStop(STOP_A);
  private static final RefetchItineraryServiceTest.VertexRef VB = G.linkStop(STOP_B);
  private static final RefetchItineraryServiceTest.VertexRef VC = G.linkStop(STOP_C);
  private static final RefetchItineraryServiceTest.VertexRef VD = G.linkStop(STOP_D);
  private static final RefetchItineraryServiceTest.VertexRef V2 = G.vertex();

  static {
    V1.street(VA).meters(10);
    V2.street(VD).meters(10);

    VB.street(VC).meters(20);
  }

  private static final Graph GRAPH = G.build();

  // Setup transfers
  private static final RegularTransferService TRANSFER_SERVICE = createTransferService(
    List.of(makeTransfer(STOP_B, STOP_C, GRAPH))
  );

  @Test
  void populateItineraryLegsWithNoRealTime() {
    var refetchService = createRefetchService(new TransitAlertServiceImpl());

    TripOnDateDataFetcher trip1 = transitEnv.tripData("trip1");
    TripOnDateDataFetcher trip2 = transitEnv.tripData("trip4");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);

    var from = Place.forStop(STOP_B);
    var to = Place.forStop(STOP_C);

    var walkLeg = walkLeg(
      from,
      to,
      busLeg.endTime().plusMinutes(1),
      busLeg.endTime().plusMinutes(10)
    );

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg, trainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitEnv.transitService(),
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    Leg walkingLeg = legs.stream().filter(Leg::isWalkingLeg).findFirst().orElse(null);
    assertEquals(3, legs.size());
    assertEquals(
      "2020-03-03T11:01+01:00[Europe/Paris]",
      Objects.requireNonNull(walkingLeg).startTime().toString()
    );
    assertEquals(
      "2020-03-03T11:10+01:00[Europe/Paris]",
      Objects.requireNonNull(walkingLeg).endTime().toString()
    );
  }

  @Test
  void populateItineraryLegsWithRealTimeAndChangeQuay() {
    var refetchService = createRefetchService(new TransitAlertServiceImpl());

    TripOnDateDataFetcher trip1 = transitEnv.tripData("trip1");
    TripOnDateDataFetcher trip2 = transitEnv.tripData("trip4");

    var siri = SiriTestHelper.of(transitEnv);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef("trip1")
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("10:00", "10:10")
          .call(STOP_B)
          .arriveAimedExpected("11:00", "11:12")
          .departAimedExpected("11:00", "11:12")
          .call(STOP_D)
          .arriveAimedExpected("12:00", "12:15")
      )
      .buildEstimatedTimetableDeliveries();
    UpdateResult updateResult = siri.applyEstimatedTimetable(updates);
    assertEquals(1, updateResult.successful());

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);

    var from = Place.forStop(STOP_B);
    var to = Place.forStop(STOP_D);

    var walkLeg = walkLeg(
      from,
      to,
      busLeg.startTime().plusMinutes(1),
      busLeg.endTime().plusMinutes(10)
    );

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg, trainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitEnv.transitService(),
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    assertEquals(3, legs.size());

    //Realtime on first leg
    assertEquals("2020-03-03T10:10+01:00[Europe/Paris]", legs.getFirst().startTime().toString());
    assertEquals("2020-03-03T11:12+01:00[Europe/Paris]", legs.getFirst().endTime().toString());

    //Assert that refetch has updated leg data with realtime data
    assertEquals(
      "2020-03-03T11:12+01:00[Europe/Paris]",
      Objects.requireNonNull(legs.get(1)).startTime().toString()
    );
    assertEquals(
      "2020-03-03T11:12:10+01:00[Europe/Paris]",
      Objects.requireNonNull(legs.get(1)).endTime().toString()
    );

    assertEquals(Place.forStop(STOP_B).toString(), legs.get(1).from().toString());
    assertEquals(Place.forStop(STOP_C).toString(), legs.get(1).to().toString());

    //No realtime on last leg
    assertEquals("2020-03-03T08:30+01:00[Europe/Paris]", legs.getLast().startTime().toString());
    assertEquals("2020-03-03T09:30+01:00[Europe/Paris]", legs.getLast().endTime().toString());
  }

  @Test
  void populateTransitLegsWithRealtime() {
    TripOnDateDataFetcher trip5 = transitEnv.tripData("trip5");

    TripOnDateDataFetcher trip6 = transitEnv.tripData("trip6");

    ScheduledTransitLeg busOneLeg = buildScheduledTransitLeg(trip5, 0, 1);
    ScheduledTransitLeg busTwoLeg = buildScheduledTransitLeg(trip6, 0, 1);

    var itinerary = Itinerary.ofScheduledTransit(List.of(busOneLeg, busTwoLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var siri = SiriTestHelper.of(transitEnv);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef("trip5")
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("11:05", "11:07:03")
          .call(STOP_B)
          .arriveAimedExpected("11:20", "11:22:03")
      )
      .buildEstimatedTimetableDeliveries();
    UpdateResult updateResult = siri.applyEstimatedTimetable(updates);
    assertEquals(1, updateResult.successful());

    // Put an alert on stopA
    var transitAlertService = new TransitAlertServiceImpl();
    var alert = TransitAlert.of(STOP_A.getId())
      .addEntity(new EntitySelector.Stop(STOP_A.getId()))
      .withCalendar(AlertCalendar.ofAlwaysActive())
      .build();
    transitAlertService.setAlerts(List.of(alert));

    var itinerariesWithRealtime = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      createRefetchService(transitAlertService),
      transitEnv.transitService(),
      transitAlertService,
      routeRequest()
    );

    assertFalse(itinerariesWithRealtime.isEmpty());

    var legs = itinerariesWithRealtime.getFirst().legs();
    var leg1ArrivalDelay = legs.getFirst().asScheduledTransitLeg().endTime();

    assertEquals("2020-03-03T11:22:03+01:00[Europe/Paris]", leg1ArrivalDelay.toString());
    assertEquals(1, legs.get(0).listTransitAlerts().size());
    assertEquals(0, legs.get(1).listTransitAlerts().size());
    assertEquals(1, itinerariesWithRealtime.size());
  }

  @Test
  void populateLegsWithRealtimeNonTransit() {
    // Test walk leg and transit leg that can't be found in the transit service
    TripOnDateDataFetcher trip1 = transitEnv.tripData("trip1");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    Place from = Place.forStop(STOP_B);
    Place to = Place.forStop(STOP_C);

    var walkLeg = walkLeg(
      from,
      to,
      busLeg.endTime().plusMinutes(1),
      busLeg.endTime().plusMinutes(10)
    );

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var model = new TransitRepository();
    model.index();
    var transitService = new DefaultTransitService(model);

    var itineraries = List.of(itinerary);
    itineraries = RealtimeResolver.populateLegsWithRealtime(
      itineraries,
      null,
      transitService,
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    assertEquals(1, itineraries.size());

    var legs = itinerary.legs();
    assertEquals(2, legs.size());
    assertTrue(legs.get(1).isWalkingLeg());
    assertTrue(legs.get(0).isTransitLeg());
  }

  @Test
  void populateTransitLegsKeepStaySeated() {
    var refetchService = createRefetchService(new TransitAlertServiceImpl());
    TripOnDateDataFetcher trip1 = transitEnv.tripData("trip1");
    TripOnDateDataFetcher trip2 = transitEnv.tripData("trip2");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    var transfer = staySeated("trip1", 1, "trip2", 0, transitEnv);
    ScheduledTransitLeg updatedBusLeg = busLeg.copyOf().withTransferToNextLeg(transfer).build();

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);
    ScheduledTransitLeg updatedTrainLeg = trainLeg
      .copyOf()
      .withTransferFromPreviousLeg(transfer)
      .build();
    var itinerary = Itinerary.ofScheduledTransit(List.of(updatedBusLeg, updatedTrainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitEnv.transitService(),
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    assertEquals(2, legs.size());
    assertTrue(legs.getFirst().transferToNextLeg().getTransferConstraint().isStaySeated());
    assertNull(legs.getFirst().transferFromPrevLeg());
    assertTrue(legs.get(1).transferFromPrevLeg().getTransferConstraint().isStaySeated());
    assertNull(legs.get(1).transferToNextLeg());
    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ BUS trip2 12:00 13:00 ~ C []",
      itineraries.getFirst().toStr()
    );
  }

  private StreetLeg walkLeg(Place from, Place to, ZonedDateTime startTime, ZonedDateTime endTime) {
    return StreetLeg.of()
      .withFrom(from)
      .withMode(TraverseMode.WALK)
      .withTo(to)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .withDistanceMeters(500)
      .build();
  }

  private static RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1))
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(2)))
      .buildRequest();
  }

  private ScheduledTransitLeg buildScheduledTransitLeg(
    TripOnDateDataFetcher trip,
    int boardPos,
    int alightPos
  ) {
    ZonedDateTime startTime = TimeUtils.zonedDateTime(
      SERVICE_DATE,
      trip.scheduledTripTimes().getDepartureTime(boardPos),
      transitEnv.timeZone()
    );

    ZonedDateTime endTime = TimeUtils.zonedDateTime(
      SERVICE_DATE,
      trip.scheduledTripTimes().getDepartureTime(alightPos),
      transitEnv.timeZone()
    );

    return ScheduledTransitLeg.of()
      .withTripTimes(trip.scheduledTripTimes())
      .withTripPattern(trip.tripPattern())
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withServiceDate(SERVICE_DATE)
      .withZoneId(transitEnv.timeZone())
      .withBoardStopIndexInPattern(boardPos)
      .withAlightStopIndexInPattern(alightPos)
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .build();
  }

  private RefetchItineraryService createRefetchService(TransitAlertService transitAlertService) {
    StreetDetailsService streetDetailsService = null;

    VertexCreationService vertexCreationService = new VertexCreationService(
      new VertexLinker(
        GRAPH,
        GeofencingZoneService.EMPTY,
        VisibilityMode.TRAVERSE_AREA_EDGES,
        100,
        false
      )
    );

    LinkingContextFactory linkingContextFactory = new LinkingContextFactory(
      GRAPH,
      vertexCreationService
    );

    var streetLimitationParametersService = new StreetLimitationParametersService() {
      @Override
      public float maxCarSpeed() {
        return 100f;
      }

      @Override
      public int maxAreaNodes() {
        return 0;
      }

      @Override
      public float getBestWalkSafety() {
        return 0;
      }

      @Override
      public float getBestBikeSafety() {
        return 0;
      }
    };

    return new RefetchItineraryService(
      GRAPH,
      transitEnv.transitService(),
      transitAlertService,
      TRANSFER_SERVICE,
      streetDetailsService,
      linkingContextFactory,
      streetLimitationParametersService
    );
  }
}
