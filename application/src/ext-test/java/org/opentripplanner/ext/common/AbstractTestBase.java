package org.opentripplanner.ext.common;

import com.google.common.collect.ImmutableMultimap;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
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
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.TimeUtils;

public abstract class AbstractTestBase {

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

  protected static final TransitTestEnvironment TRANSIT_ENV = ENV_BUILDER.addTrip(
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

  // Common methods
  protected ScheduledTransitLeg buildScheduledTransitLeg(
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

  private static RegularTransferService createTransferService(List<PathTransfer> transfers) {
    var transferRepo = TransferServiceTestFactory.defaultTransferRepository();
    ImmutableMultimap.Builder<StopLocation, PathTransfer> builder = ImmutableMultimap.builder();
    transfers.forEach(transfer -> builder.put(transfer.from, transfer));
    transferRepo.addAllTransfersByStops(builder.build());
    return TransferServiceTestFactory.transferService(transferRepo);
  }

  protected ConstrainedTransferService createConstrainedTransferService(
    ConstrainedTransfer... constrainedTransfers
  ) {
    DefaultConstrainedTransferService service = new DefaultConstrainedTransferService();

    service.addAll(Arrays.asList(constrainedTransfers));

    return service;
  }

  protected ConstrainedTransfer staySeated(String fromTrip, int fromPos, String toTrip, int toPos) {
    return constrained(
      fromTrip,
      fromPos,
      toTrip,
      toPos,
      TransferConstraint.of().staySeated().build()
    );
  }

  protected ConstrainedTransfer guaranteed(String fromTrip, int fromPos, String toTrip, int toPos) {
    return constrained(
      fromTrip,
      fromPos,
      toTrip,
      toPos,
      TransferConstraint.of().guaranteed().build()
    );
  }

  protected ConstrainedTransfer constrained(
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

  private static PathTransfer makeTransfer(RegularStop from, RegularStop to, Graph graph) {
    var edges = findPath(from, to, graph);
    var length = edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
    return new PathTransfer(STOP_B, STOP_C, length, edges, EnumSet.of(StreetMode.WALK));
  }

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

  protected RefetchItineraryService createRefetchService(
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

  protected RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1))
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(2)))
      .buildRequest();
  }

  protected static class VertexRef {

    protected final StreetVertex vertex;
    protected final GraphBuilder graphBuilder;

    protected VertexRef(GraphBuilder graphBuilder, StreetVertex vertex) {
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

  protected static class GraphBuilder {

    protected final List<EdgeRef> edges = new ArrayList<>();
    protected final Graph graph = new Graph();

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

    public StreetVertex createVertex() {
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

    public void createEdge(StreetVertex v1, StreetVertex v2, int meters) {
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

    public WgsCoordinate nextCoord() {
      return WgsCoordinate.GREENWICH.moveEastMeters(graph.countVertices());
    }

    public String nextLabel() {
      return "X" + graph.countVertices();
    }
  }

  protected static class EdgeRef {

    protected final StreetVertex from;
    protected final StreetVertex to;
    protected int meters = 100;

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
