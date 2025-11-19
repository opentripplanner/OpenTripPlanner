package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.ImmutableMultimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetStationCentroidLink;
import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;

class LinkingContextFactoryTest {

  private static final WgsCoordinate CENTER = new WgsCoordinate(0, 0);
  private static final WgsCoordinate RIGHT = new WgsCoordinate(0, 1);
  private static final WgsCoordinate TOP = new WgsCoordinate(1, 0);
  private static final WgsCoordinate DISTANT = new WgsCoordinate(70, 70);
  private static final int DISTANCE = 20;

  private static final FeedScopedId ALPHA_ID = id("alpha");
  private static final FeedScopedId OMEGA_ID = id("omega");

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private final Station stationAlpha = testModel
    .station("alpha")
    .withId(ALPHA_ID)
    .withCoordinate(CENTER)
    .withShouldRouteToCentroid(true)
    .build();

  private final RegularStop stopA = testModel
    .stop("A")
    .withCoordinate(CENTER.moveEastMeters(DISTANCE))
    .build();
  private final RegularStop stopB = testModel
    .stop("B")
    .withCoordinate(CENTER.moveSouthMeters(DISTANCE))
    .build();
  private final RegularStop stopC = testModel
    .stop("C")
    .withCoordinate(CENTER.moveWestMeters(DISTANCE))
    .build();
  private final RegularStop stopD = testModel
    .stop("D")
    .withCoordinate(CENTER.moveNorthMeters(DISTANCE))
    .build();
  private final Graph graph = buildGraph(stationAlpha, stopA, stopB, stopC, stopD);
  private final VertexCreationService vertexCreationService = new VertexCreationService(
    VertexLinkerTestFactory.of(graph)
  );
  private final LinkingContextFactory linkingContextFactory = new LinkingContextFactory(
    graph,
    vertexCreationService
  );

  private final SiteRepository siteRepository = testModel
    .siteRepositoryBuilder()
    .withRegularStops(List.of(stopA, stopB, stopC, stopD))
    .build();

  @Test
  void coordinates() {
    var container = new TemporaryVerticesContainer();
    var from = GenericLocation.fromCoordinate(stopA.getLat(), stopA.getLon());
    var to = GenericLocation.fromCoordinate(stopD.getLat(), stopD.getLon());
    var request = LinkingContextRequest.of()
      .withFrom(from)
      .withTo(to)
      .withDirectMode(StreetMode.WALK)
      .build();
    var linkingContext = linkingContextFactory.create(container, request);
    assertThat(linkingContext.findVertices(from)).hasSize(1);
    assertThat(linkingContext.findVertices(to)).hasSize(1);
  }

  @Test
  void stopId() {
    var stopLinkingContextFactory = new LinkingContextFactory(
      graph,
      new VertexCreationService(VertexLinkerTestFactory.of(graph)),
      Set::of
    );
    var container = new TemporaryVerticesContainer();
    var from = stopToLocation(stopA);
    var to = stopToLocation(stopB);
    var request = LinkingContextRequest.of()
      .withFrom(from)
      .withTo(to)
      .withDirectMode(StreetMode.WALK)
      .build();
    var linkingContext = stopLinkingContextFactory.create(container, request);
    assertEquals(stopA, toStop(linkingContext.findVertices(from)));
    assertEquals(stopB, toStop(linkingContext.findVertices(to)));
  }

  @Test
  void stationId() {
    var mapping = ImmutableMultimap.<FeedScopedId, FeedScopedId>builder()
      .putAll(OMEGA_ID, stopC.getId(), stopD.getId())
      .build();
    var stopLinkingContextFactory = new LinkingContextFactory(
      graph,
      new VertexCreationService(VertexLinkerTestFactory.of(graph)),
      mapping::get
    );
    var container = new TemporaryVerticesContainer();
    var from = GenericLocation.fromStopId("station", OMEGA_ID.getFeedId(), OMEGA_ID.getId());
    var request = LinkingContextRequest.of()
      .withFrom(from)
      .withTo(stopToLocation(stopB))
      .withDirectMode(StreetMode.WALK)
      .build();
    var linkingContext = stopLinkingContextFactory.create(container, request);
    assertThat(toStops(linkingContext.findVertices(from))).containsExactly(stopC, stopD);
  }

  @Test
  void centroid() {
    var container = new TemporaryVerticesContainer();
    var from = GenericLocation.fromStopId("station", ALPHA_ID.getFeedId(), ALPHA_ID.getId());
    var request = LinkingContextRequest.of()
      .withFrom(from)
      .withTo(stopToLocation(stopB))
      .withDirectMode(StreetMode.WALK)
      .build();
    var linkingContext = linkingContextFactory.create(container, request);
    var fromVertices = List.copyOf(linkingContext.findVertices(from));
    assertThat(fromVertices).hasSize(1);

    var station = ((StationCentroidVertex) fromVertices.getFirst()).getId();
    assertEquals(station, this.stationAlpha.getId());
  }

  @Test
  void locationsShouldBeRoutableWithTheGivenModes() {
    try (var container = new TemporaryVerticesContainer();) {
      var from = GenericLocation.fromCoordinate(0.5, 0.5);
      var to = GenericLocation.fromCoordinate(0.6, 0.4);
      var via = GenericLocation.fromCoordinate(0.4, 0.6);
      var request = LinkingContextRequest.of()
        .withFrom(from)
        .withTo(to)
        .withViaLocationsWithCoordinates(List.of(via))
        .withDirectMode(StreetMode.WALK)
        .withAccessMode(StreetMode.CAR)
        .withTransferMode(StreetMode.CAR)
        .withEgressMode(StreetMode.CAR)
        .build();
      var subject = linkingContextFactory.create(container, request);

      var fromVertices = subject.findVertices(from);
      assertThat(fromVertices).hasSize(1);
      var fromVertex = fromVertices.iterator().next();
      var fromOutgoing = fromVertex.getOutgoing();
      var fromIncoming = fromVertex.getIncoming();
      assertThat(fromOutgoing).hasSize(2);
      assertThat(fromIncoming).hasSize(0);

      // Both direct and access mode should be possible to use from the origin vertex
      assertTrue(outgoingEdgeIsTraversableWith(fromOutgoing, TraverseMode.WALK));
      assertTrue(outgoingEdgeIsTraversableWith(fromOutgoing, TraverseMode.CAR));
      assertFalse(outgoingEdgeIsTraversableWith(fromOutgoing, TraverseMode.BICYCLE));

      var toVertices = subject.findVertices(to);
      assertThat(toVertices).hasSize(1);
      var toVertex = toVertices.iterator().next();
      var toIncoming = toVertex.getIncoming();
      var toOutgoing = toVertex.getOutgoing();
      assertThat(toIncoming).hasSize(2);
      assertThat(toOutgoing).hasSize(0);

      // Both direct and egress mode should be possible to use to reach the destination vertex
      assertTrue(incomingEdgeIsTraversableWith(toIncoming, TraverseMode.WALK));
      assertTrue(incomingEdgeIsTraversableWith(toIncoming, TraverseMode.CAR));
      assertFalse(incomingEdgeIsTraversableWith(toIncoming, TraverseMode.BICYCLE));

      var viaVertices = subject.findVertices(via);
      assertThat(viaVertices).hasSize(1);
      var viaVertex = viaVertices.iterator().next();
      var viaIncoming = viaVertex.getIncoming();
      var viaOutgoing = viaVertex.getOutgoing();
      assertThat(viaIncoming).hasSize(2);
      assertThat(viaOutgoing).hasSize(2);

      // Both direct and transfer mode should be possible to use from and to the via vertex
      assertTrue(incomingEdgeIsTraversableWith(viaIncoming, TraverseMode.WALK));
      assertTrue(incomingEdgeIsTraversableWith(viaIncoming, TraverseMode.CAR));
      assertFalse(incomingEdgeIsTraversableWith(viaIncoming, TraverseMode.BICYCLE));
      assertTrue(outgoingEdgeIsTraversableWith(viaOutgoing, TraverseMode.WALK));
      assertTrue(outgoingEdgeIsTraversableWith(viaOutgoing, TraverseMode.CAR));
      assertFalse(outgoingEdgeIsTraversableWith(viaOutgoing, TraverseMode.BICYCLE));
    }
  }

  @Test
  void locationNotFoundException() {
    // Coordinate not found (but in bounds)
    var container = new TemporaryVerticesContainer();
    var from = GenericLocation.fromCoordinate(65, 65);
    var to = GenericLocation.fromCoordinate(0.0, 1.0);
    var viaRequest = LinkingContextRequest.of()
      .withFrom(from)
      .withTo(to)
      .withDirectMode(StreetMode.WALK)
      .build();
    var exception = assertThrows(RoutingValidationException.class, () ->
      linkingContextFactory.create(container, viaRequest)
    );
    container.close();
    assertThat(exception.getRoutingErrors()).hasSize(1);
    var viaError = exception.getRoutingErrors().getFirst();
    assertEquals(InputField.FROM_PLACE, viaError.inputField);
    assertEquals(RoutingErrorCode.LOCATION_NOT_FOUND, viaError.code);
  }

  @Test
  void locationOutsideBoundsException() {
    var container = new TemporaryVerticesContainer();
    var request = LinkingContextRequest.of()
      .withFrom(GenericLocation.fromCoordinate(80, 80))
      .withTo(GenericLocation.fromCoordinate(85, 85))
      .withViaLocationsWithCoordinates(List.of(new GenericLocation("Via1", null, 87.0, 87.0)))
      .withDirectMode(StreetMode.WALK)
      .build();
    var exception = assertThrows(RoutingValidationException.class, () ->
      linkingContextFactory.create(container, request)
    );
    container.close();

    assertThat(exception.getRoutingErrors()).hasSize(3);
    var fromError = exception.getRoutingErrors().get(0);
    assertEquals(InputField.FROM_PLACE, fromError.inputField);
    assertEquals(RoutingErrorCode.OUTSIDE_BOUNDS, fromError.code);
    var toError = exception.getRoutingErrors().get(1);
    assertEquals(InputField.TO_PLACE, toError.inputField);
    assertEquals(RoutingErrorCode.OUTSIDE_BOUNDS, toError.code);
    var viaError = exception.getRoutingErrors().get(2);
    assertEquals(InputField.INTERMEDIATE_PLACE, viaError.inputField);
    assertEquals(RoutingErrorCode.OUTSIDE_BOUNDS, viaError.code);
  }

  @Test
  void walkingBetterThanTransitException() {
    var container = new TemporaryVerticesContainer();
    var sameLocation = GenericLocation.fromCoordinate(0.0, 0.0);
    var request = LinkingContextRequest.of()
      .withFrom(sameLocation)
      .withTo(sameLocation)
      .withDirectMode(StreetMode.WALK)
      .build();
    var exception = assertThrows(RoutingValidationException.class, () ->
      linkingContextFactory.create(container, request)
    );
    container.close();

    assertThat(exception.getRoutingErrors()).hasSize(1);
    var fromError = exception.getRoutingErrors().getFirst();
    assertNull(fromError.inputField);
    assertEquals(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, fromError.code);
  }

  @Test
  void nonExistingPlaceIdWithCoordinatesShouldFallbackToCoordinates() {
    var container = new TemporaryVerticesContainer();
    var nonExistingStopId = new FeedScopedId("F", "NonExistingStop");

    // Create locations with both a non-existing stop ID and valid coordinates
    var from = new GenericLocation("From", nonExistingStopId, stopA.getLat(), stopA.getLon());
    var to = new GenericLocation(
      "To",
      new FeedScopedId("F", "AnotherNonExisting"),
      stopD.getLat(),
      stopD.getLon()
    );

    var request = LinkingContextRequest.of()
      .withFrom(from)
      .withTo(to)
      .withDirectMode(StreetMode.WALK)
      .build();

    // This should NOT throw an exception - it should fall back to using coordinates
    var linkingContext = linkingContextFactory.create(container, request);

    // Verify that vertices were created from the coordinates
    var fromVertices = linkingContext.findVertices(from);
    assertThat(fromVertices).hasSize(1);
    assertTemporaryVertexOnStop(fromVertices.stream().findFirst().get(), stopA);

    var toVertices = linkingContext.findVertices(to);
    assertThat(toVertices).hasSize(1);
    assertTemporaryVertexOnStop(toVertices.stream().findFirst().get(), stopD);

    container.close();
  }

  private static Graph buildGraph(Station station, RegularStop... stops) {
    var graph = new Graph();
    var left = StreetModelForTest.intersectionVertex(CENTER.asJtsCoordinate());
    var right = StreetModelForTest.intersectionVertex(RIGHT.asJtsCoordinate());
    var top = StreetModelForTest.intersectionVertex(TOP.asJtsCoordinate());
    // This is so we can test location not found exception
    var distant = StreetModelForTest.intersectionVertex(DISTANT.asJtsCoordinate());
    StreetModelForTest.streetEdge(left, right, StreetTraversalPermission.PEDESTRIAN);
    StreetModelForTest.streetEdge(left, right, StreetTraversalPermission.CAR);
    StreetModelForTest.streetEdge(left, right, StreetTraversalPermission.BICYCLE);
    StreetModelForTest.streetEdge(right, top, StreetTraversalPermission.PEDESTRIAN);
    StreetModelForTest.streetEdge(right, top, StreetTraversalPermission.CAR);
    StreetModelForTest.streetEdge(right, top, StreetTraversalPermission.BICYCLE);
    graph.addVertex(left);
    graph.addVertex(right);
    graph.addVertex(top);
    graph.addVertex(distant);
    var centroidVertex = new StationCentroidVertex(
      station.getId(),
      station.getName(),
      station.getCoordinate()
    );
    graph.addVertex(centroidVertex);
    StreetStationCentroidLink.createStreetStationLink(centroidVertex, left);
    Arrays.stream(stops).forEach(s -> {
      graph.addVertex(TransitStopVertex.of().withId(s.getId()).withPoint(s.getGeometry()).build());
      var vertex = StreetModelForTest.intersectionVertex(s.getCoordinate().asJtsCoordinate());
      StreetModelForTest.streetEdge(vertex, left);
      graph.addVertex(vertex);
    });
    graph.index();
    graph.calculateConvexHull();
    return graph;
  }

  private void assertTemporaryVertexOnStop(Vertex vertex, RegularStop stop) {
    assertThat(vertex).isInstanceOf(TemporaryStreetLocation.class);
    assertEquals(stop.getLat(), vertex.getLat(), 0.0001);
    assertEquals(stop.getLon(), vertex.getLon(), 0.0001);
  }

  private RegularStop toStop(Set<? extends Vertex> fromVertices) {
    assertThat(fromVertices).hasSize(1);
    var id = ((TransitStopVertex) List.copyOf(fromVertices).getFirst()).getId();
    return siteRepository.getRegularStop(id);
  }

  private Set<RegularStop> toStops(Set<? extends Vertex> fromVertices) {
    return fromVertices
      .stream()
      .map(v -> ((TransitStopVertex) v).getId())
      .map(siteRepository::getRegularStop)
      .collect(Collectors.toUnmodifiableSet());
  }

  private GenericLocation stopToLocation(RegularStop s) {
    return GenericLocation.fromStopId(
      s.getName().toString(),
      s.getId().getFeedId(),
      s.getId().getId()
    );
  }

  private boolean outgoingEdgeIsTraversableWith(Collection<Edge> edges, TraverseMode mode) {
    return edges
      .stream()
      .anyMatch(outgoing ->
        outgoing
          .getToVertex()
          .getOutgoingStreetEdges()
          .stream()
          .anyMatch(edge -> edge.canTraverse(mode))
      );
  }

  private boolean incomingEdgeIsTraversableWith(Collection<Edge> edges, TraverseMode mode) {
    return edges
      .stream()
      .anyMatch(incoming ->
        incoming
          .getFromVertex()
          .getIncomingStreetEdges()
          .stream()
          .anyMatch(edge -> edge.canTraverse(mode))
      );
  }
}
