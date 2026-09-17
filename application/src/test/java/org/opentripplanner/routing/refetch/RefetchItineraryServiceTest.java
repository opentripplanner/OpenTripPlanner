package org.opentripplanner.routing.refetch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMultimap;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
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
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;

public class RefetchItineraryServiceTest {

  // Setup transit
  protected static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  protected static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE
  );
  protected static final Station STATION_A = ENV_BUILDER.station("StationA");
  protected static final RegularStop STOP_A = ENV_BUILDER.stopAtStation("A", "StationA");
  protected static final RegularStop STOP_B = ENV_BUILDER.stop("B");
  protected static final RegularStop STOP_C = ENV_BUILDER.stop("C");
  protected static final RegularStop STOP_D = ENV_BUILDER.stop("D");
  protected static final RegularStop STOP_E = ENV_BUILDER.stop("E");

  protected static final TransitTestEnvironmentBuilder TRANSIT_ENV = ENV_BUILDER.addTrip(
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
    .addTrip(TripInput.of("trip4").addStop(STOP_C, "08:30").addStop(STOP_D, "09:30"))
    .addTrip(
      TripInput.of("trip5")
        .withWithTripOnServiceDate("trip5")
        .addStop(STOP_D, "15:00")
        .addStop(STOP_E, "16:00")
    );

  // Setup street
  protected static final GraphBuilder G = GraphBuilder.of();

  protected static final VertexRef V1 = G.vertex();
  protected static final VertexRef VA = G.linkStop(STOP_A);
  protected static final VertexRef VB = G.linkStop(STOP_B);
  protected static final VertexRef VC = G.linkStop(STOP_C);
  protected static final VertexRef VD = G.linkStop(STOP_D);
  protected static final VertexRef V2 = G.vertex();

  static {
    V1.street(VA).meters(10);
    V2.street(VD).meters(10);

    VB.street(VC).meters(20);
  }

  protected static final Graph GRAPH = G.build();

  // Setup transfers
  protected static final RegularTransferService TRANSFER_SERVICE = createTransferService(
    List.of(makeTransfer(STOP_B, STOP_C, GRAPH))
  );

  @Test
  void refetchSimple() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1), routeRequest());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B []", itinerary.toStr());
    assertEquals(0, itinerary.legs().getFirst().boardStopPosInPattern());
    assertEquals(1, itinerary.legs().getFirst().alightStopPosInPattern());
  }

  @Test
  void refetchFromSameStation() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var from = GenericLocation.fromStopId(STATION_A.getId());

    var itinerary = refetch.refetchItinerary(from, null, List.of(leg1), routeRequest());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B []", itinerary.toStr());
    assertEquals(0, itinerary.legs().getFirst().boardStopPosInPattern());
    assertEquals(1, itinerary.legs().getFirst().alightStopPosInPattern());
  }

  @Test
  void refetchWithTransferAtSameStop() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var leg2 = legRef("trip2", STOP_B, STOP_C, transitEnv);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B ~ BUS trip2 12:00 13:00 ~ C []", itinerary.toStr());
  }

  @Test
  void refetchWithTransfer() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var leg2 = legRef("trip3", STOP_C, STOP_D, transitEnv);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D []",
      itinerary.toStr()
    );
    assertEquals("11:00", itinerary.legs().get(1).startTime().toLocalTime().toString());
    assertEquals("11:00:10", itinerary.legs().get(1).endTime().toLocalTime().toString());
  }

  /// It should be possible to fetch an itinerary that has a leg that ends after the later one starts.
  /// This is needed to be able to refetch an itinerary that has a delay on an earlier leg that makes
  /// a transfer impossible to make.
  @Test
  void refetchWithImpossibleTransfer() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var leg2 = legRef("trip4", STOP_C, STOP_D, transitEnv);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip4 8:30 9:30 ~ D []",
      itinerary.toStr()
    );
    assertEquals(Duration.ofMinutes(-30), itinerary.totalDuration());
  }

  @Test
  void refetchItineraryWithAccessEgress() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var start = GenericLocation.fromCoordinate(V1.coord());
    var end = GenericLocation.fromCoordinate(V2.coord());

    var leg1 = legRef("trip1", STOP_A, STOP_D, transitEnv);

    var itinerary = refetch.refetchItinerary(start, end, List.of(leg1), routeRequest());

    assertEquals(
      "Origin ~ Walk 5s ~ A ~ BUS trip1 10:00 12:00 ~ D ~ Walk 5s ~ Destination []",
      itinerary.toStr()
    );
  }

  @Test
  void refetchItineraryWithAccessFromStop() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var start = GenericLocation.fromStopId(STOP_B.getId());
    var leg1 = legRef("trip3", STOP_C, STOP_D, transitEnv);

    var itinerary = refetch.refetchItinerary(start, null, List.of(leg1), routeRequest());

    assertEquals("B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D []", itinerary.toStr());
  }

  @Test
  void refetchItineraryWithMultipleLegsAndAccessEgress() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var start = GenericLocation.fromCoordinate(V1.coord());
    var end = GenericLocation.fromCoordinate(V2.coord());

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var leg2 = legRef("trip3", STOP_C, STOP_D, transitEnv);

    var itinerary = refetch.refetchItinerary(start, end, List.of(leg1, leg2), routeRequest());

    assertEquals(
      "Origin ~ Walk 5s ~ A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D ~ Walk 5s ~ Destination []",
      itinerary.toStr()
    );
  }

  @Test
  void refetchItineraryWithTwoTransitLegsAndConstrainedTransfer() {
    var transitEnv = ENV_BUILDER.build();
    var cts = createConstrainedTransferService(guaranteed("trip1", 1, "trip2", 0, transitEnv));

    transitEnv.transitService().getConstrainedTransferService().addAll(cts.listAll());

    var refetch = createRefetchService(transitEnv);

    transitEnv.transitService().getConstrainedTransferService().addAll(cts.listAll());

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var leg2 = legRef("trip2", STOP_B, STOP_D, transitEnv);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    var legs = itinerary.legs();

    assertTrue(legs.getFirst().transferToNextLeg().getTransferConstraint().isGuaranteed());
    assertNull(itinerary.legs().getFirst().transferFromPrevLeg());
    assertTrue(
      Objects.requireNonNull(legs.getLast().transferFromPrevLeg())
        .getTransferConstraint()
        .isGuaranteed()
    );
    assertNull(itinerary.legs().getLast().transferToNextLeg());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B ~ BUS trip2 12:00 14:00 ~ D []", itinerary.toStr());
  }

  @Test
  void refetchItineraryWithMultipleConstrainedTransfers() {
    var transitEnv = TRANSIT_ENV.build();
    var cts = createConstrainedTransferService(
      staySeated("trip1", 1, "trip3", 0, transitEnv),
      guaranteed("trip3", 1, "trip5", 0, transitEnv)
    );
    transitEnv.transitService().getConstrainedTransferService().addAll(cts.listAll());
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var leg2 = legRef("trip3", STOP_C, STOP_D, transitEnv);
    var leg3 = legRef("trip5", STOP_D, STOP_E, transitEnv);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2, leg3), routeRequest());

    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D ~ BUS trip5 15:00 16:00 ~ E []",
      itinerary.toStr()
    );

    var legs = itinerary.legs();
    assertNull(legs.get(0).transferFromPrevLeg());
    assertTrue(legs.get(0).transferToNextLeg().getTransferConstraint().isStaySeated());
    assertTrue(legs.get(1).isWalkingLeg());
    assertTrue(legs.get(2).transferFromPrevLeg().getTransferConstraint().isStaySeated());
    assertTrue(legs.get(2).transferToNextLeg().getTransferConstraint().isGuaranteed());
    assertTrue(legs.get(3).transferFromPrevLeg().getTransferConstraint().isGuaranteed());
    assertNull(legs.get(3).transferToNextLeg());
  }

  @Test
  void refetchWithFailedLinking() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var start = GenericLocation.fromCoordinate(V1.coord().moveNorthMeters(10000));
    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);

    var e = assertThrows(RefetchItineraryException.class, () ->
      refetch.refetchItinerary(start, null, List.of(leg1), routeRequest())
    );
    assertEquals("Could not calculate access", e.getMessage());
  }

  @Test
  void refetchWithFailedTransfer() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip3", STOP_C, STOP_D, transitEnv);
    var leg2 = legRef("trip1", STOP_A, STOP_B, transitEnv);

    var e = assertThrows(RefetchItineraryException.class, () ->
      refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest())
    );
    assertEquals("Could not transfer from F:D to F:A", e.getMessage());
  }

  @Test
  void refetchEmptyLegs() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    assertThrows(IllegalArgumentException.class, () ->
      refetch.refetchItinerary(null, null, List.of(), routeRequest())
    );
  }

  @Test
  void refetchWithBoardAlightSlack() {
    var transitEnv = TRANSIT_ENV.build();
    var refetch = createRefetchService(transitEnv);

    var leg1 = legRef("trip1", STOP_A, STOP_B, transitEnv);
    var leg2 = legRef("trip3", STOP_C, STOP_D, transitEnv);
    var start = GenericLocation.fromCoordinate(V1.coord());
    var end = GenericLocation.fromCoordinate(V2.coord());

    var request = routeRequest()
      .copyOf()
      .withPreferences(p ->
        p.withTransit(transit ->
          transit.withDefaultBoardSlackSec(2 * 60).withDefaultAlightSlackSec(3 * 60)
        )
      )
      .buildRequest();

    var itinerary = refetch.refetchItinerary(start, end, List.of(leg1, leg2), request);

    assertEquals(
      "Origin ~ Walk 5s ~ A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D ~ Walk 5s ~ Destination []",
      itinerary.toStr()
    );
    // Access should have two min board slack
    assertEquals("09:58", itinerary.legs().getFirst().endTime().toLocalTime().toString());
    // Transfer should have three min alight slack
    assertEquals("11:03", itinerary.legs().get(2).startTime().toLocalTime().toString());
    // Egress should have three min alight slack
    assertEquals("13:33", itinerary.legs().getLast().startTime().toLocalTime().toString());
  }

  private ScheduledTransitLegReference legRef(
    String tripId,
    RegularStop boardStop,
    RegularStop alightStop,
    TransitTestEnvironment transitEnv
  ) {
    var tripData = transitEnv.tripData(tripId);
    var stops = tripData.tripPattern().getStops();
    var boardPos = stops.indexOf(boardStop);
    var alightPos = stops.indexOf(alightStop);
    assertNotEquals(-1, boardPos);
    assertNotEquals(-1, alightPos);
    return new ScheduledTransitLegReference(
      transitEnv.tripData(tripId).trip().getId(),
      SERVICE_DATE,
      boardPos,
      alightPos,
      boardStop.getId(),
      alightStop.getId(),
      null
    );
  }

  private RefetchItineraryService createRefetchService(TransitTestEnvironment transitEnv) {
    StreetDetailsService streetDetailsService = null;
    VertexCreationService vertexCreationService = new VertexCreationService(
      new VertexLinker(
        GRAPH,
        GeofencingZoneService.EMPTY,
        VisibilityMode.TRAVERSE_AREA_EDGES,
        10,
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
      new TransitAlertServiceImpl(),
      TRANSFER_SERVICE,
      streetDetailsService,
      linkingContextFactory,
      streetLimitationParametersService
    );
  }

  public static RegularTransferService createTransferService(List<PathTransfer> transfers) {
    var transferRepo = TransferServiceTestFactory.defaultTransferRepository();
    ImmutableMultimap.Builder<StopLocation, PathTransfer> builder = ImmutableMultimap.builder();
    transfers.forEach(transfer -> builder.put(transfer.from, transfer));
    transferRepo.addAllTransfersByStops(builder.build());
    return TransferServiceTestFactory.transferService(transferRepo);
  }

  public static ConstrainedTransferService createConstrainedTransferService(
    ConstrainedTransfer... constrainedTransfers
  ) {
    DefaultConstrainedTransferService service = new DefaultConstrainedTransferService();

    service.addAll(Arrays.asList(constrainedTransfers));

    return service;
  }

  public static ConstrainedTransfer staySeated(
    String fromTrip,
    int fromPos,
    String toTrip,
    int toPos,
    TransitTestEnvironment transitEnv
  ) {
    return constrained(
      fromTrip,
      fromPos,
      toTrip,
      toPos,
      TransferConstraint.of().staySeated().build(),
      transitEnv
    );
  }

  public static ConstrainedTransfer guaranteed(
    String fromTrip,
    int fromPos,
    String toTrip,
    int toPos,
    TransitTestEnvironment transitEnv
  ) {
    return constrained(
      fromTrip,
      fromPos,
      toTrip,
      toPos,
      TransferConstraint.of().guaranteed().build(),
      transitEnv
    );
  }

  public static ConstrainedTransfer constrained(
    String fromTrip,
    int fromPos,
    String toTrip,
    int toPos,
    TransferConstraint constraint,
    TransitTestEnvironment transitEnv
  ) {
    var p1 = new TripTransferPoint(transitEnv.tripData(fromTrip).trip(), fromPos);

    var p2 = new TripTransferPoint(transitEnv.tripData(toTrip).trip(), toPos);

    return new ConstrainedTransfer(null, p1, p2, constraint);
  }

  public static PathTransfer makeTransfer(RegularStop from, RegularStop to, Graph graph) {
    var edges = findPath(from, to, graph);
    var length = edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
    return new PathTransfer(STOP_B, STOP_C, length, edges, EnumSet.of(StreetMode.WALK));
  }

  public static List<Edge> findPath(RegularStop from, RegularStop to, Graph graph) {
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

  private static RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1))
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(2)))
      .buildRequest();
  }

  public static class VertexRef {

    public final StreetVertex vertex;
    public final GraphBuilder graphBuilder;

    public VertexRef(GraphBuilder graphBuilder, StreetVertex vertex) {
      this.graphBuilder = graphBuilder;
      this.vertex = vertex;
    }

    public StreetVertex vertex() {
      return vertex;
    }

    public GraphBuilder graphBuilder() {
      return graphBuilder;
    }

    public EdgeRef street(VertexRef to) {
      return graphBuilder.street(this, to);
    }

    public WgsCoordinate coord() {
      return vertex.toWgsCoordinate();
    }
  }

  public static class GraphBuilder {

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

    private EdgeRef street(VertexRef from, VertexRef to) {
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

  public static class EdgeRef {

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
