package org.opentripplanner.ext.realtimeresolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMultimap;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.I18NString;
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
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.constrained.ConstrainedTransferService;
import org.opentripplanner.transfer.constrained.internal.DefaultConstrainedTransferService;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;
import org.opentripplanner.transfer.constrained.model.TransferConstraint;
import org.opentripplanner.transfer.constrained.model.TripTransferPoint;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.TripOnDateDataFetcher;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;
import org.opentripplanner.utils.time.TimeUtils;

class RealtimeResolverTest {

  // Setup transit
  static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(SERVICE_DATE);
  static final RegularStop STOP_A = ENV_BUILDER.stopAtStation("A", "StationA");
  static final RegularStop STOP_B = ENV_BUILDER.stop("B");
  static final RegularStop STOP_C = ENV_BUILDER.stop("C");
  static final RegularStop STOP_D = ENV_BUILDER.stop("D");

  static final TransitTestEnvironment TRANSIT_ENV = ENV_BUILDER.addTrip(
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
    )
    .addTrip(TripInput.of("trip3").addStop(STOP_C, "12:30").addStop(STOP_D, "13:30"))
    .addTrip(TripInput.of("trip4").addStop(STOP_C, "08:30").addStop(STOP_D, "09:30"))
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
    .build();

  // Setup street
  static final GraphBuilder G = GraphBuilder.of();
  static final VertexRef V1 = G.vertex();
  static final VertexRef VA = G.linkStop(STOP_A);
  static final VertexRef VB = G.linkStop(STOP_B);
  static final VertexRef VC = G.linkStop(STOP_C);
  static final VertexRef VD = G.linkStop(STOP_D);
  static final VertexRef V2 = G.vertex();

  static {
    // Connect street vertices to stops
    V1.street(VA).meters(10);
    V2.street(VD).meters(10);
    // Create transfer path
    VB.street(VC).meters(20);
  }

  static final Graph GRAPH = G.build();

  // Setup transfers
  static final RegularTransferService TRANSFER_SERVICE = createTransferService(
    List.of(makeTransfer(STOP_B, STOP_C, GRAPH))
  );

  @Test
  void populateItineraryLegsWithNoRealTime() {
    var refetchService = createRefetchService(
      new TransitAlertServiceImpl(),
      new DefaultConstrainedTransferService()
    );
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");
    TripOnDateDataFetcher trip2 = TRANSIT_ENV.tripData("trip4");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);

    StopLocation fromStop = RegularStop.of(
      trip1.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex.toWgsCoordinate())
      .withId(busLeg.to().stop.getId())
      .build();

    StopLocation toStop = RegularStop.of(
      trip2.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex.toWgsCoordinate())
      .withId(trainLeg.from().stop.getId())
      .build();

    var from = Place.forStop(fromStop);
    var to = Place.forStop(toStop);

    var walkLeg = StreetLeg.of()
      .withFrom(from)
      .withMode(TraverseMode.WALK)
      .withTo(to)
      .withStartTime(busLeg.startTime().plusMinutes(1))
      .withEndTime(busLeg.endTime().plusMinutes(10))
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .withDistanceMeters(500)
      .build();

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg, trainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();
    var model = new TransitRepository();
    model.index();
    var transitService = new DefaultTransitService(model);
    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitService,
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    Leg walkingLeg = legs.stream().filter(Leg::isWalkingLeg).findFirst().orElse(null);
    assertEquals(3, legs.size());
    assertEquals(
      "2020-03-03T10:01+01:00[Europe/Paris]",
      Objects.requireNonNull(walkingLeg).startTime().toString()
    );
    assertEquals(
      "2020-03-03T11:10+01:00[Europe/Paris]",
      Objects.requireNonNull(walkingLeg).endTime().toString()
    );
  }

  @Test
  void populateItineraryLegsWithRealTime() {
    var refetchService = createRefetchService(
      new TransitAlertServiceImpl(),
      new DefaultConstrainedTransferService()
    );
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");
    TripOnDateDataFetcher trip2 = TRANSIT_ENV.tripData("trip4");

    var siri = SiriTestHelper.of(TRANSIT_ENV);

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

    StopLocation fromStop = RegularStop.of(
      trip1.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex.toWgsCoordinate())
      .withId(busLeg.to().stop.getId())
      .build();

    StopLocation toStop = RegularStop.of(
      trip2.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex.toWgsCoordinate())
      //Sets wrong id that doesnt match previous leg, in order for stops to not match and force a refetch
      .withId(STOP_D.getId())
      .build();

    var from = Place.forStop(fromStop);
    var to = Place.forStop(toStop);

    var walkLeg = StreetLeg.of()
      .withFrom(from)
      .withMode(TraverseMode.WALK)
      .withTo(to)
      .withStartTime(busLeg.startTime().plusMinutes(1))
      .withEndTime(busLeg.endTime().plusMinutes(10))
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .withDistanceMeters(500)
      .build();

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg, trainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();
    var model = new TransitRepository();
    model.index();
    var transitService = new DefaultTransitService(model);
    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitService,
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    Leg refetchedWalkingLeg = legs.stream().filter(Leg::isWalkingLeg).findFirst().orElse(null);
    assertEquals(3, legs.size());
    //Assert that refetch has updated leg data with realtime data
    assertEquals(
      "2020-03-03T11:12+01:00[Europe/Paris]",
      Objects.requireNonNull(refetchedWalkingLeg).startTime().toString()
    );
    assertEquals(
      "2020-03-03T11:12:10+01:00[Europe/Paris]",
      Objects.requireNonNull(refetchedWalkingLeg).endTime().toString()
    );
  }

  @Test
  void testPopulateLegsWithRealtime() {
    TripOnDateDataFetcher trip5 = TRANSIT_ENV.tripData("trip5");
    TripOnDateDataFetcher trip6 = TRANSIT_ENV.tripData("trip6");

    ScheduledTransitLeg busOneLeg = buildScheduledTransitLeg(trip5, 0, 1);
    ScheduledTransitLeg busTwoLeg = buildScheduledTransitLeg(trip6, 0, 1);

    var itinerary = Itinerary.ofScheduledTransit(List.of(busOneLeg, busTwoLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var siri = SiriTestHelper.of(TRANSIT_ENV);

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
      createRefetchService(transitAlertService, new DefaultConstrainedTransferService()),
      TRANSIT_ENV.transitService(),
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
  void testPopulateLegsWithRealtimeNonTransit() {
    // Test walk leg and transit leg that doesn't have a corresponding realtime leg
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    Place from = Place.normal(VB.vertex, VB.vertex.getName());
    Place to = Place.normal(VC.vertex, VC.vertex.getName());

    StreetLeg walkLeg = StreetLeg.of()
      .withFrom(from)
      .withMode(TraverseMode.WALK)
      .withTo(to)
      .withDistanceMeters(300)
      .withStartTime(busLeg.endTime().plusMinutes(1))
      .withEndTime(busLeg.endTime().plusMinutes(10))
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .withDistanceMeters(500)
      .build();

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var model = new TransitRepository();
    model.index();
    var transitService = new DefaultTransitService(model);

    var itineraries = List.of(itinerary);
    itineraries = RealtimeResolver.populateLegsWithRealtime(
      itineraries,
      createRefetchService(new TransitAlertServiceImpl(), new DefaultConstrainedTransferService()),
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
  void testPopulateLegsKeepStaySeated() {
    var cts = createConstrainedTransferService(staySeated("trip1", 1, "trip2", 0));
    var refetchService = createRefetchService(new TransitAlertServiceImpl(), cts);
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");
    TripOnDateDataFetcher trip2 = TRANSIT_ENV.tripData("trip2");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);
    ConstrainedTransfer transfer = cts.findTransfer(
      TRANSIT_ENV.tripData("trip1").trip(),
      1,
      STOP_B,
      TRANSIT_ENV.tripData("trip2").trip(),
      0,
      STOP_C
    );
    ScheduledTransitLeg updatedBusLeg = busLeg.copyOf().withTransferToNextLeg(transfer).build();

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);
    ScheduledTransitLeg updatedTrainLeg = trainLeg
      .copyOf()
      .withTransferFromPreviousLeg(transfer)
      .build();
    var itinerary = Itinerary.ofScheduledTransit(List.of(updatedBusLeg, updatedTrainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var model = new TransitRepository();
    model.index();

    var transitService = new DefaultTransitService(model);
    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitService,
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

  private ScheduledTransitLeg buildScheduledTransitLeg(
    TripOnDateDataFetcher trip,
    int boardPos,
    int alightPos
  ) {
    ZonedDateTime startTime = TimeUtils.zonedDateTime(
      SERVICE_DATE,
      trip.scheduledTripTimes().getDepartureTime(boardPos),
      TRANSIT_ENV.timeZone()
    );

    ZonedDateTime endTime = TimeUtils.zonedDateTime(
      SERVICE_DATE,
      trip.scheduledTripTimes().getDepartureTime(alightPos),
      TRANSIT_ENV.timeZone()
    );
    return ScheduledTransitLeg.of()
      .withTripTimes(trip.scheduledTripTimes())
      .withTripPattern(trip.tripPattern())
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withServiceDate(SERVICE_DATE)
      .withZoneId(TRANSIT_ENV.timeZone())
      .withBoardStopIndexInPattern(boardPos)
      .withAlightStopIndexInPattern(alightPos)
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .build();
  }

  private static PathTransfer makeTransfer(RegularStop from, RegularStop to, Graph graph) {
    var edges = findPath(from, to, graph);
    var length = edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
    return new PathTransfer(STOP_B, STOP_C, length, edges, EnumSet.of(StreetMode.WALK));
  }

  private static RegularTransferService createTransferService(List<PathTransfer> transfers) {
    var transferRepo = TransferServiceTestFactory.defaultTransferRepository();
    ImmutableMultimap.Builder<StopLocation, PathTransfer> builder = ImmutableMultimap.builder();
    transfers.forEach(transfer -> builder.put(transfer.from, transfer));
    transferRepo.addAllTransfersByStops(builder.build());
    return TransferServiceTestFactory.transferService(transferRepo);
  }

  /// Find the edges that correspond to a transfer
  private static List<Edge> findPath(RegularStop from, RegularStop to, Graph graph) {
    var vFrom = graph.getStopVertex(from.getId());
    var linkFrom = vFrom.getOutgoing().stream().findFirst().orElseThrow();
    var vTo = graph.getStopVertex(to.getId());
    var linkTo = vTo.getIncoming().stream().findFirst().orElseThrow();
    var edge = linkFrom
      .getToVertex()
      .getOutgoingStreetEdges()
      .stream()
      .filter(e -> e.getToVertex().equals(linkTo.getFromVertex()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Could not find edge"));
    return List.of(linkFrom, edge, linkTo);
  }

  private RouteRequest routeRequest() {
    // From and To doesn't have any effect for RefetchItineraryService
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1))
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(2)))
      .buildRequest();
  }

  private ConstrainedTransferService createConstrainedTransferService(
    ConstrainedTransfer... constrainedTransfers
  ) {
    DefaultConstrainedTransferService service = new DefaultConstrainedTransferService();
    service.addAll(Arrays.asList(constrainedTransfers));
    return service;
  }

  private ConstrainedTransfer staySeated(String fromTrip, int fromPos, String toTrip, int toPos) {
    return constrained(
      fromTrip,
      fromPos,
      toTrip,
      toPos,
      TransferConstraint.of().staySeated().build()
    );
  }

  private ConstrainedTransfer constrained(
    String fromTrip,
    int fromPos,
    String toTrip,
    int toPos,
    TransferConstraint constraint
  ) {
    var p1 = new TripTransferPoint(TRANSIT_ENV.tripData(fromTrip).trip(), fromPos);
    var p2 = new TripTransferPoint(TRANSIT_ENV.tripData(toTrip).trip(), toPos);
    return new ConstrainedTransfer(null, p1, p2, constraint);
  }

  private RefetchItineraryService createRefetchService(
    TransitAlertService transitAlertService,
    ConstrainedTransferService constrainedTransferService
  ) {
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
      TRANSIT_ENV.transitService(),
      transitAlertService,
      TRANSFER_SERVICE,
      streetDetailsService,
      constrainedTransferService,
      linkingContextFactory,
      streetLimitationParametersService
    );
  }

  private static class GraphBuilder {

    private final List<EdgeRef> edges = new ArrayList<>();
    private final Graph graph = new Graph();

    public static GraphBuilder of() {
      return new GraphBuilder();
    }

    public VertexRef vertex() {
      return new VertexRef(this, createVertex());
    }

    public VertexRef linkStop(RegularStop stop) {
      var stopV = TransitStopVertex.of()
        .withId(stop.getId())
        .withCoordinate(stop.getCoordinate())
        .build();
      var streetVertex = createVertex();
      BoardingLocationToStopLink.createBoardingLocationToStopLink(stopV, streetVertex);
      BoardingLocationToStopLink.createBoardingLocationToStopLink(streetVertex, stopV);
      var v = new VertexRef(this, streetVertex);
      graph.addVertex(stopV);
      return v;
    }

    public EdgeRef street(VertexRef from, VertexRef to) {
      var e = new EdgeRef(from.vertex, to.vertex);
      this.edges.add(e);
      return e;
    }

    public Graph build() {
      for (var e : edges) {
        createEdge(e.from, e.to, e.meters);
        createEdge(e.to, e.from, e.meters);
      }
      graph.hasStreets = true;
      graph.index();
      return graph;
    }

    private StreetVertex createVertex() {
      var coord = nextCoord();
      var v = new LabelledIntersectionVertex(
        nextLabel(),
        coord.longitude(),
        coord.latitude(),
        false,
        false
      );
      graph.addVertex(v);
      return v;
    }

    private void createEdge(StreetVertex v1, StreetVertex v2, int meters) {
      var geom = GeometryUtils.makeLineString(v1.toWgsCoordinate(), v2.toWgsCoordinate());
      new StreetEdgeBuilder<>()
        .withFromVertex(v1)
        .withToVertex(v2)
        .withGeometry(geom)
        .withName("TestEdge")
        .withMeterLength(meters)
        .withPermission(StreetTraversalPermission.ALL)
        .withBack(false)
        .buildAndConnect();
    }

    private WgsCoordinate nextCoord() {
      return WgsCoordinate.GREENWICH.moveEastMeters(graph.countVertices());
    }

    private String nextLabel() {
      return "X" + graph.countVertices();
    }
  }

  private static class VertexRef {

    private final StreetVertex vertex;
    private final GraphBuilder graphBuilder;

    public VertexRef(GraphBuilder graphBuilder, StreetVertex vertex) {
      this.graphBuilder = graphBuilder;
      this.vertex = vertex;
    }

    public EdgeRef street(VertexRef to) {
      return graphBuilder.street(this, to);
    }

    public WgsCoordinate coord() {
      return vertex.toWgsCoordinate();
    }
  }

  private static class EdgeRef {

    private final StreetVertex from;
    private final StreetVertex to;
    private int meters = 100;

    public EdgeRef(StreetVertex from, StreetVertex to) {
      this.from = from;
      this.to = to;
    }

    public EdgeRef meters(int meters) {
      this.meters = meters;
      return this;
    }
  }
}
