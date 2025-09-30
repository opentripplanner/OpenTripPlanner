package org.opentripplanner.routing.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.linking.TestVertexLinker;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

public class TemporaryVerticesContainerTest {

  private final GeometryFactory gf = GeometryUtils.getGeometryFactory();
  // Given:
  // - a graph with 4 intersections/vertexes and a stop
  private final Graph g = new Graph(new Deduplicator());

  private final StreetVertex a = StreetModelForTest.intersectionVertex("A", 1.0, 1.0);
  private final StreetVertex b = StreetModelForTest.intersectionVertex("B", 1.0, 0.0);
  private final StreetVertex c = StreetModelForTest.intersectionVertex("C", 0.0, 1.0);
  private final StreetVertex d = StreetModelForTest.intersectionVertex("D", 0.0, 2.0);
  private final FeedScopedId stopId = new FeedScopedId("test", "1");
  private final TransitStopVertex s = TransitStopVertex.of()
    .withStop(RegularStop.of(stopId, () -> 1).withCoordinate(WgsCoordinate.GREENWICH).build())
    .build();
  private final List<Vertex> permanentVertexes = Arrays.asList(a, b, c, d, s);
  // - And travel *origin* is 0,4 degrees on the road from B to A
  private final GenericLocation from = GenericLocation.fromCoordinate(1.0, 0.4);
  // - and *destination* is slightly off 0.7 degrees on road from C to A
  private final GenericLocation to = GenericLocation.fromCoordinate(0.701, 1.001);
  private final List<VisitViaLocation> viaLocations = List.of(
    new VisitViaLocation("Via1", null, List.of(), List.of(new WgsCoordinate(0.0, 1.5))),
    new VisitViaLocation("Via2", null, List.of(FeedScopedId.parse("F:1")), List.of()),
    new VisitViaLocation(
      "Via3",
      null,
      List.of(FeedScopedId.parse("F:2")),
      List.of(new WgsCoordinate(0.1, 1.9))
    )
  );

  // - and some roads
  @BeforeEach
  void setup() {
    permanentVertexes.forEach(g::addVertex);
    createStreetEdge(a, b, "a -> b WALK");
    createStreetEdge(a, b, "a -> b CAR", StreetTraversalPermission.CAR);
    createStreetEdge(b, a, "b -> a WALK");
    createStreetEdge(b, a, "b -> a CAR", StreetTraversalPermission.CAR);
    createStreetEdge(a, c, "a -> c");
    createStreetEdge(c, d, "c -> d");
    createStreetEdge(d, c, "c -> d");
    createStreetEdge(a, d, "a -> d");
    g.index();
    g.calculateConvexHull();
  }

  @Test
  void createFromToViaVertexRequest() {
    var fromWithStops = new GenericLocation("From with stops", stopId, 1.0, 0.4);
    var subject = TemporaryVerticesContainer.of(g, TestVertexLinker.of(g), Set::of)
      .withFrom(fromWithStops, StreetMode.WALK)
      .withTo(to, StreetMode.WALK)
      .withVia(viaLocations, EnumSet.of(StreetMode.WALK))
      .build();
    var request = subject.createFromToViaVertexRequest();
    var fromVertices = request.findVertices(fromWithStops);
    assertThat(fromVertices).isNotEmpty();
    assertEquals(subject.fromVertices(), fromVertices);
    var toVertices = request.findVertices(to);
    assertThat(toVertices).isNotEmpty();
    assertEquals(subject.toVertices(), toVertices);
    assertThat(request.fromStops()).isNotEmpty();
    assertEquals(subject.fromStopVertices(), request.fromStops());
    assertThat(request.toStops()).isEmpty();
    assertEquals(subject.toStopVertices(), request.toStops());
    var firstViaVertices = request.findVertices(viaLocations.get(0).coordinateLocation());
    assertThat(firstViaVertices).isNotEmpty();
    assertEquals(
      subject.verticesByLocation().get(viaLocations.get(0).coordinateLocation()),
      firstViaVertices
    );
    var thirdViaVertices = request.findVertices(viaLocations.get(2).coordinateLocation());
    assertThat(thirdViaVertices).isNotEmpty();
    assertEquals(
      subject.verticesByLocation().get(viaLocations.get(2).coordinateLocation()),
      thirdViaVertices
    );
  }

  @Test
  void temporaryChangesRemovedOnClose() {
    // When - the container is created
    var subject = TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
      .withFrom(from, StreetMode.WALK)
      .withTo(to, StreetMode.WALK)
      .build();

    // Then:
    originAndDestinationInsertedCorrect(subject, false);
    cleanUpAndValidate(subject);
  }

  @Test
  void multipleModes() {
    try (
      var subject = TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
        .withFrom(from, EnumSet.of(StreetMode.WALK, StreetMode.CAR_TO_PARK))
        .withTo(to, StreetMode.WALK)
        .build()
    ) {
      // When - the container is created

      // Then:
      assertThat(subject.fromVertices()).hasSize(1);
      var fromOutgoing = subject.fromVertices().iterator().next().getOutgoing();
      assertThat(fromOutgoing).hasSize(4);
      assertTrue(outgoingEdgeIsTraversableWith(fromOutgoing, TraverseMode.WALK));
      assertTrue(outgoingEdgeIsTraversableWith(fromOutgoing, TraverseMode.CAR));
    }
  }

  @Test
  void temporaryChangesRemovedOnCloseWithVia() {
    // When - the container is created
    var subject = TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
      .withFrom(from, StreetMode.WALK)
      .withTo(to, StreetMode.WALK)
      .withVia(viaLocations, EnumSet.of(StreetMode.WALK))
      .build();

    // Then:
    locationsInsertedCorrect(subject);
    cleanUpAndValidate(subject);
  }

  @Test
  void locationNotFoundException() {
    // Stops not found
    var fromException = assertThrows(RoutingValidationException.class, () ->
      TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
        .withFrom(
          new GenericLocation("Stop not found", new FeedScopedId("F", "stop1"), null, null),
          StreetMode.WALK
        )
        .build()
        .close()
    );
    assertThat(fromException.getRoutingErrors()).hasSize(1);
    var fromError = fromException.getRoutingErrors().getFirst();
    assertEquals(InputField.FROM_PLACE, fromError.inputField);
    assertEquals(RoutingErrorCode.LOCATION_NOT_FOUND, fromError.code);

    var toException = assertThrows(RoutingValidationException.class, () ->
      TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
        .withFrom(from, StreetMode.WALK)
        .withTo(
          new GenericLocation("Stop not found", new FeedScopedId("F", "stop1"), null, null),
          StreetMode.WALK
        )
        .build()
        .close()
    );

    assertThat(toException.getRoutingErrors()).hasSize(1);
    var toError = toException.getRoutingErrors().getFirst();
    assertEquals(InputField.TO_PLACE, toError.inputField);
    assertEquals(RoutingErrorCode.LOCATION_NOT_FOUND, toError.code);

    // Coordinate not found (but in bounds)
    var viaException = assertThrows(RoutingValidationException.class, () ->
      TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
        .withFrom(from, StreetMode.WALK)
        .withTo(to, StreetMode.WALK)
        .withVia(
          List.of(new VisitViaLocation("Via1", null, List.of(), List.of(new WgsCoordinate(10, 0)))),
          EnumSet.of(StreetMode.WALK)
        )
        .build()
        .close()
    );
    assertThat(viaException.getRoutingErrors()).hasSize(1);
    var viaError = viaException.getRoutingErrors().getFirst();
    assertEquals(InputField.INTERMEDIATE_PLACE, viaError.inputField);
    assertEquals(RoutingErrorCode.LOCATION_NOT_FOUND, viaError.code);
  }

  @Test
  void locationOutsideBoundsException() {
    var exception = assertThrows(RoutingValidationException.class, () ->
      TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
        .withFrom(GenericLocation.fromCoordinate(0, 0.02), StreetMode.WALK)
        .withTo(GenericLocation.fromCoordinate(0, 0.01), StreetMode.WALK)
        .withVia(
          List.of(new VisitViaLocation("Via1", null, List.of(), List.of(new WgsCoordinate(78, 0)))),
          EnumSet.of(StreetMode.WALK)
        )
        .build()
        .close()
    );

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
    var exception = assertThrows(RoutingValidationException.class, () ->
      TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
        .withFrom(from, StreetMode.WALK)
        .withTo(from, StreetMode.WALK)
        .build()
        .close()
    );

    assertThat(exception.getRoutingErrors()).hasSize(1);
    var fromError = exception.getRoutingErrors().getFirst();
    assertNull(fromError.inputField);
    assertEquals(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, fromError.code);
  }

  @Test
  void cleanUpWhenExceptionThrown() {
    assertThrows(RoutingValidationException.class, () ->
      TemporaryVerticesContainer.of(g, TestVertexLinker.of(g))
        .withFrom(from, StreetMode.WALK)
        .withTo(from, StreetMode.WALK)
        .withVia(viaLocations, EnumSet.of(StreetMode.WALK))
        .build()
        .close()
    );

    validateCleanup();
  }

  private static <T extends Collection<String>> T findAllReachableVertexes(
    Vertex vertex,
    boolean forward,
    T list
  ) {
    if (list.contains(vertex.getDefaultName())) {
      return list;
    }

    list.add(vertex.getDefaultName());
    if (forward) {
      vertex.getOutgoing().forEach(it -> findAllReachableVertexes(it.getToVertex(), forward, list));
    } else {
      vertex
        .getIncoming()
        .forEach(it -> findAllReachableVertexes(it.getFromVertex(), forward, list));
    }
    return list;
  }

  private void originAndDestinationInsertedCorrect(
    TemporaryVerticesContainer subject,
    boolean hasViaLocations
  ) {
    // Then - the origin and destination is
    var originVertices = subject.fromVertices();
    assertThat(originVertices).hasSize(1);
    assertEquals("Origin", originVertices.iterator().next().getDefaultName());
    var destinationVertices = subject.toVertices();
    assertThat(destinationVertices).hasSize(1);
    assertEquals("Destination", destinationVertices.iterator().next().getDefaultName());

    // And - from the origin
    Collection<String> vertexesReachableFromOrigin = findAllReachableVertexes(
      subject.fromVertices().iterator().next(),
      true,
      new ArrayList<>()
    );
    String msg = "All reachable vertexes from origin: " + vertexesReachableFromOrigin;

    // it is possible to reach the A, B, C and the Destination Vertex
    assertTrue(vertexesReachableFromOrigin.contains("A"), msg);
    assertTrue(vertexesReachableFromOrigin.contains("B"), msg);
    assertTrue(vertexesReachableFromOrigin.contains("C"), msg);
    assertTrue(vertexesReachableFromOrigin.contains("D"), msg);
    assertTrue(vertexesReachableFromOrigin.contains("Destination"), msg);

    if (hasViaLocations) {
      assertTrue(vertexesReachableFromOrigin.contains("Via1"), msg);
      assertTrue(vertexesReachableFromOrigin.contains("Via3"), msg);
    }

    // And - from the destination we can backtrack
    Collection<String> vertexesReachableFromDestination = findAllReachableVertexes(
      subject.toVertices().iterator().next(),
      false,
      new ArrayList<>()
    );
    msg = "All reachable vertexes back from destination: " + vertexesReachableFromDestination;

    // and reach the A, B and the Origin Vertex
    assertTrue(vertexesReachableFromDestination.contains("A"), msg);
    assertTrue(vertexesReachableFromDestination.contains("B"), msg);
    assertTrue(vertexesReachableFromDestination.contains("Origin"), msg);

    // But - not the C Vertex
    assertFalse(vertexesReachableFromDestination.contains("C"), msg);
  }

  private void locationsInsertedCorrect(TemporaryVerticesContainer subject) {
    originAndDestinationInsertedCorrect(subject, true);
    // Then - only via locations with coordinates are included
    var verticesByLocation = subject.verticesByLocation();
    assertThat(verticesByLocation).hasSize(4);
    var firstViaVertices = verticesByLocation.get(viaLocations.get(0).coordinateLocation());
    assertThat(firstViaVertices).hasSize(1);
    assertEquals("Via1", firstViaVertices.iterator().next().getDefaultName());
    var thirdViaVertices = verticesByLocation.get(viaLocations.get(2).coordinateLocation());
    assertThat(thirdViaVertices).hasSize(1);
    assertEquals("Via3", thirdViaVertices.iterator().next().getDefaultName());

    // And - from the first via location
    Collection<String> vertexesReachableFromFirstViaForward = findAllReachableVertexes(
      firstViaVertices.iterator().next(),
      true,
      new ArrayList<>()
    );
    Collection<String> vertexesReachableFromFirstViaBackward = findAllReachableVertexes(
      firstViaVertices.iterator().next(),
      false,
      new ArrayList<>()
    );
    String firstForwardMsg =
      "Forward reachable vertexes from the first via location: " +
      vertexesReachableFromFirstViaForward;
    String firstBackwardMsg =
      "Backward reachable vertexes from the first via location: " +
      vertexesReachableFromFirstViaBackward;

    assertTrue(vertexesReachableFromFirstViaForward.contains("C"), firstForwardMsg);
    assertTrue(vertexesReachableFromFirstViaForward.contains("D"), firstForwardMsg);
    assertTrue(vertexesReachableFromFirstViaForward.contains("Via1"), firstForwardMsg);

    assertTrue(vertexesReachableFromFirstViaBackward.contains("A"), firstBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("B"), firstBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("C"), firstBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("D"), firstBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("Origin"), firstBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("Via1"), firstBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("Via3"), firstBackwardMsg);

    // And - from the third via location
    Collection<String> vertexesReachableFromThirdViaForward = findAllReachableVertexes(
      thirdViaVertices.iterator().next(),
      true,
      new ArrayList<>()
    );
    Collection<String> vertexesReachableFromThirdViaBackward = findAllReachableVertexes(
      thirdViaVertices.iterator().next(),
      false,
      new ArrayList<>()
    );
    String thirdForwardMsg =
      "Forward reachable vertexes from the third via location: " +
      vertexesReachableFromThirdViaForward;
    String thirdBackwardMsg =
      "Backward reachable vertexes from the third via location: " +
      vertexesReachableFromThirdViaBackward;

    assertTrue(vertexesReachableFromFirstViaForward.contains("C"), thirdForwardMsg);
    assertTrue(vertexesReachableFromFirstViaForward.contains("D"), thirdForwardMsg);
    assertTrue(vertexesReachableFromFirstViaForward.contains("Via1"), thirdForwardMsg);

    assertTrue(vertexesReachableFromFirstViaBackward.contains("A"), thirdBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("B"), thirdBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("Origin"), thirdBackwardMsg);
    assertTrue(vertexesReachableFromFirstViaBackward.contains("Via3"), thirdBackwardMsg);
  }

  private void cleanUpAndValidate(TemporaryVerticesContainer subject) {
    // And When:
    subject.close();

    // Then - validate temporary elements are removed
    validateCleanup();
  }

  private void validateCleanup() {
    // Then - permanent vertexes
    for (Vertex v : permanentVertexes) {
      // - does not reference the any temporary nodes anymore
      for (Edge e : v.getIncoming()) {
        assertVertexEdgeIsNotReferencingTemporaryElements(v, e, e.getFromVertex());
      }
      for (Edge e : v.getOutgoing()) {
        assertVertexEdgeIsNotReferencingTemporaryElements(v, e, e.getToVertex());
      }
    }
    assertTrue(g.getEdges().stream().noneMatch(e -> e instanceof TemporaryEdge));
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

  private void createStreetEdge(StreetVertex v0, StreetVertex v1, String name) {
    createStreetEdge(v0, v1, name, StreetTraversalPermission.PEDESTRIAN);
  }

  private void createStreetEdge(
    StreetVertex v0,
    StreetVertex v1,
    String name,
    StreetTraversalPermission permission
  ) {
    double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
    StreetModelForTest.streetEdgeBuilder(v0, v1, dist, StreetTraversalPermission.ALL)
      .withName(I18NString.of(name))
      .buildAndConnect();
  }

  private void assertVertexEdgeIsNotReferencingTemporaryElements(Vertex src, Edge e, Vertex v) {
    String sourceName = src.getDefaultName();
    assertFalse(e instanceof TemporaryEdge, sourceName + " -> " + e.getDefaultName());
    assertFalse(v instanceof TemporaryVertex, sourceName + " -> " + v.getDefaultName());
  }
}
