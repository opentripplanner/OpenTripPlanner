package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This creates a graph with trip patterns
 <pre>
  S0 -  V0 ------------
        |     \       |
 S11 - V11 --------> V21 - S21
        |      \      |
 S12 - V12 --------> V22 - V22
        |             |
 S13 - V13 --------> V23 - V23
 </pre>
 */
class DirectTransferGeneratorTest extends GraphRoutingTest {

  private static final Duration MAX_TRANSFER_DURATION = Duration.ofHours(1);
  private TransitStopVertex S0, S11, S12, S13, S21, S22, S23;
  private StreetVertex V0, V11, V12, V13, V21, V22, V23;

  @Test
  public void testDirectTransfersWithoutPatterns() {
    var otpModel = model(false);
    var graph = otpModel.graph();
    var transitModel = otpModel.transitModel();
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);
    graph.hasStreets = false;

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTransfers(transitModel.getAllPathTransfers());
  }

  @Test
  public void testDirectTransfersWithPatterns() {
    var otpModel = model(true);
    var graph = otpModel.graph();
    graph.hasStreets = false;
    var transitModel = otpModel.transitModel();
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTransfers(
      transitModel.getAllPathTransfers(),
      tr(S0, 556, S11),
      tr(S0, 935, S21),
      tr(S11, 751, S21),
      tr(S12, 751, S22),
      tr(S13, 2224, S12),
      tr(S13, 2347, S22),
      tr(S21, 751, S11),
      tr(S22, 751, S12),
      tr(S23, 2347, S12),
      tr(S23, 2224, S22)
    );
  }

  @Test
  public void testDirectTransfersWithRestrictedPatterns() {
    var otpModel = model(true, true);
    var graph = otpModel.graph();
    graph.hasStreets = false;
    var transitModel = otpModel.transitModel();
    var transferRequests = List.of(new RouteRequest());

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTransfers(
      transitModel.getAllPathTransfers(),
      tr(S0, 2780, S12),
      tr(S0, 935, S21),
      tr(S11, 2224, S12),
      tr(S11, 751, S21),
      tr(S12, 751, S22),
      tr(S13, 2224, S12),
      tr(S13, 2347, S22),
      tr(S21, 2347, S12),
      tr(S22, 751, S12),
      tr(S23, 2347, S12),
      tr(S23, 2224, S22)
    );
  }

  @Test
  public void testSingleRequestWithoutPatterns() {
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    var otpModel = model(false);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var transitModel = otpModel.transitModel();

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTransfers(transitModel.getAllPathTransfers());
  }

  @Test
  public void testSingleRequestWithPatterns() {
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    var otpModel = model(true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var transitModel = otpModel.transitModel();

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTransfers(
      transitModel.getAllPathTransfers(),
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 100, List.of(V11, V21), S21)
    );
  }

  @Test
  public void testMultipleRequestsWithoutPatterns() {
    var reqWalk = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.WALK);

    var reqBike = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.BIKE);

    var transferRequests = List.of(reqWalk, reqBike);

    var otpModel = model(false);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var transitModel = otpModel.transitModel();

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTransfers(transitModel.getAllPathTransfers());
  }

  @Test
  public void testMultipleRequestsWithPatterns() {
    var reqWalk = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.WALK);

    var reqBike = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.BIKE);

    var transferRequests = List.of(reqWalk, reqBike);

    TestOtpModel model = model(true);
    var graph = model.graph();
    graph.hasStreets = true;
    var transitModel = model.transitModel();

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTransfers(
      transitModel.getAllPathTransfers(),
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 100, List.of(V11, V21), S21),
      tr(S11, 110, List.of(V11, V22), S22)
    );
  }

  @Test
  public void testTransferOnIsolatedStations() {
    var otpModel = model(true, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = false;

    var transitModel = otpModel.transitModel();
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    )
      .buildGraph();

    assertTrue(transitModel.getAllPathTransfers().isEmpty());
  }

  private TestOtpModel model(boolean addPatterns) {
    return model(addPatterns, false);
  }

  private TestOtpModel model(boolean addPatterns, boolean withBoardingConstraint) {
    return model(addPatterns, withBoardingConstraint, false);
  }

  private TestOtpModel model(
    boolean addPatterns,
    boolean withBoardingConstraint,
    boolean withNoTransfersOnStations
  ) {
    return modelOf(
      new Builder() {
        @Override
        public void build() {
          S0 = stop("S0", 47.495, 19.001, withNoTransfersOnStations);
          S11 = stop("S11", 47.500, 19.001, withNoTransfersOnStations);
          S12 = stop("S12", 47.520, 19.001, withNoTransfersOnStations);
          S13 = stop("S13", 47.540, 19.001, withNoTransfersOnStations);
          S21 = stop("S21", 47.500, 19.011, withNoTransfersOnStations);
          S22 = stop("S22", 47.520, 19.011, withNoTransfersOnStations);
          S23 = stop("S23", 47.540, 19.011, withNoTransfersOnStations);

          V0 = intersection("V0", 47.495, 19.000);
          V11 = intersection("V11", 47.500, 19.000);
          V12 = intersection("V12", 47.510, 19.000);
          V13 = intersection("V13", 47.520, 19.000);
          V21 = intersection("V21", 47.500, 19.010);
          V22 = intersection("V22", 47.510, 19.010);
          V23 = intersection("V23", 47.520, 19.010);

          biLink(V0, S0);
          biLink(V11, S11);
          biLink(V12, S12);
          biLink(V13, S13);
          biLink(V21, S21);
          biLink(V22, S22);
          biLink(V23, S23);

          street(V0, V11, 100, StreetTraversalPermission.ALL);
          street(V0, V12, 200, StreetTraversalPermission.ALL);
          street(V0, V21, 100, StreetTraversalPermission.ALL);
          street(V0, V22, 200, StreetTraversalPermission.ALL);

          street(V11, V12, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V12, V13, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V21, V22, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V22, V23, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V11, V21, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V11, V22, 110, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

          if (addPatterns) {
            var agency = TransitModelForTest.agency("Agency");

            tripPattern(
              TripPattern
                .of(TransitModelForTest.id("TP1"))
                .withRoute(route("R1", TransitMode.BUS, agency))
                .withStopPattern(
                  new StopPattern(List.of(st(S11, !withBoardingConstraint, true), st(S12), st(S13)))
                )
                .build()
            );

            tripPattern(
              TripPattern
                .of(TransitModelForTest.id("TP2"))
                .withRoute(route("R2", TransitMode.BUS, agency))
                .withStopPattern(new StopPattern(List.of(st(S21), st(S22), st(S23))))
                .build()
            );
          }
        }
      }
    );
  }

  private void assertTransfers(
    Collection<PathTransfer> allPathTransfers,
    TransferDescriptor... transfers
  ) {
    var matchedTransfers = new HashSet<PathTransfer>();
    var assertions = Stream.concat(
      Arrays.stream(transfers).map(td -> td.matcher(allPathTransfers, matchedTransfers)),
      Stream.of(allTransfersMatched(allPathTransfers, matchedTransfers))
    );

    assertAll(assertions);
  }

  private Executable allTransfersMatched(
    Collection<PathTransfer> transfersByStop,
    Set<PathTransfer> matchedTransfers
  ) {
    return () -> {
      var missingTransfers = new HashSet<>(transfersByStop);
      missingTransfers.removeAll(matchedTransfers);

      assertEquals(Set.of(), missingTransfers, "All transfers matched");
    };
  }

  private TransferDescriptor tr(TransitStopVertex from, double distance, TransitStopVertex to) {
    return new TransferDescriptor(from, distance, to);
  }

  private TransferDescriptor tr(
    TransitStopVertex from,
    double distance,
    List<StreetVertex> vertices,
    TransitStopVertex to
  ) {
    return new TransferDescriptor(from, distance, vertices, to);
  }

  private static class TransferDescriptor {

    private final StopLocation from;
    private final StopLocation to;
    private final Double distanceMeters;
    private final List<StreetVertex> vertices;

    public TransferDescriptor(TransitStopVertex from, Double distanceMeters, TransitStopVertex to) {
      this.from = from.getStop();
      this.distanceMeters = distanceMeters;
      this.vertices = null;
      this.to = to.getStop();
    }

    public TransferDescriptor(
      TransitStopVertex from,
      Double distanceMeters,
      List<StreetVertex> vertices,
      TransitStopVertex to
    ) {
      this.from = from.getStop();
      this.distanceMeters = distanceMeters;
      this.vertices = vertices;
      this.to = to.getStop();
    }

    @Override
    public String toString() {
      return ToStringBuilder
        .of(getClass())
        .addObj("from", from)
        .addObj("to", to)
        .addNum("distanceMeters", distanceMeters)
        .addCol("vertices", vertices)
        .toString();
    }

    boolean matches(PathTransfer transfer) {
      if (!Objects.equals(from, transfer.from) || !Objects.equals(to, transfer.to)) {
        return false;
      }

      if (vertices == null) {
        return distanceMeters == transfer.getDistanceMeters() && transfer.getEdges() == null;
      } else {
        var transferVertices = transfer
          .getEdges()
          .stream()
          .map(Edge::getToVertex)
          .filter(StreetVertex.class::isInstance)
          .toList();

        return (
          distanceMeters == transfer.getDistanceMeters() &&
          Objects.equals(vertices, transferVertices)
        );
      }
    }

    private Executable matcher(
      Collection<PathTransfer> transfersByStop,
      Set<PathTransfer> matchedTransfers
    ) {
      return () -> {
        var matched = transfersByStop.stream().filter(this::matches).findFirst();

        if (matched.isPresent()) {
          assertTrue(true, "Found transfer for " + this);
          matchedTransfers.add(matched.get());
        } else {
          fail("Missing transfer for " + this);
        }
      };
    }
  }
}
