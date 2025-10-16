package org.opentripplanner.street.search;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.ImmutableMultimap;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetStationCentroidLink;
import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;

class TemporaryVerticesContainerTest {

  private static final WgsCoordinate CENTER = new WgsCoordinate(0, 0);
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

  private final SiteRepository siteRepository = testModel
    .siteRepositoryBuilder()
    .withRegularStops(List.of(stopA, stopB, stopC, stopD))
    .build();

  @Test
  void coordinates() {
    var container = new TemporaryVerticesContainer(
      graph,
      VertexLinkerTestFactory.of(graph),
      Set::of,
      GenericLocation.fromCoordinate(stopA.getLat(), stopA.getLon()),
      GenericLocation.fromCoordinate(stopD.getLat(), stopD.getLon()),
      WALK,
      WALK
    );
    assertThat(container.getFromVertices()).hasSize(1);
    assertThat(container.getToVertices()).hasSize(1);

    assertThat(container.getFromStopVertices()).isEmpty();
    assertThat(container.getToStopVertices()).isEmpty();
  }

  @Test
  void stopId() {
    var container = new TemporaryVerticesContainer(
      graph,
      VertexLinkerTestFactory.of(graph),
      Set::of,
      stopToLocation(stopA),
      stopToLocation(stopB),
      WALK,
      WALK
    );
    assertEquals(stopA, toStop(container.getFromVertices()));
    assertEquals(stopB, toStop(container.getToVertices()));

    assertEquals(stopA, toStop(container.getFromStopVertices()));
    assertEquals(stopB, toStop(container.getToVertices()));
  }

  @Test
  void stationId() {
    var mapping = ImmutableMultimap.<FeedScopedId, FeedScopedId>builder()
      .putAll(OMEGA_ID, stopC.getId(), stopD.getId())
      .build();
    var container = new TemporaryVerticesContainer(
      graph,
      VertexLinkerTestFactory.of(graph),
      mapping::get,
      GenericLocation.fromStopId("station", OMEGA_ID.getFeedId(), OMEGA_ID.getId()),
      stopToLocation(stopB),
      WALK,
      WALK
    );
    assertThat(toStops(container.getFromVertices())).containsExactly(stopC, stopD);
    assertThat(toStops(container.getFromStopVertices())).containsExactly(stopC, stopD);
  }

  @Test
  void centroid() {
    var container = new TemporaryVerticesContainer(
      graph,
      VertexLinkerTestFactory.of(graph),
      Set::of,
      GenericLocation.fromStopId("station", ALPHA_ID.getFeedId(), ALPHA_ID.getId()),
      stopToLocation(stopB),
      WALK,
      WALK
    );
    var fromVertices = List.copyOf(container.getFromVertices());
    assertThat(fromVertices).hasSize(1);

    var station = ((StationCentroidVertex) fromVertices.getFirst()).getId();
    assertEquals(station, this.stationAlpha.getId());
  }

  private static Graph buildGraph(Station station, RegularStop... stops) {
    var graph = new Graph();
    var center = StreetModelForTest.intersectionVertex(CENTER.asJtsCoordinate());
    graph.addVertex(center);
    var centroidVertex = new StationCentroidVertex(station.getId(), station.getCoordinate());
    graph.addVertex(centroidVertex);
    StreetStationCentroidLink.createStreetStationLink(centroidVertex, center);
    Arrays.stream(stops).forEach(s -> {
      graph.addVertex(TransitStopVertex.of().withId(s.getId()).withPoint(s.getGeometry()).build());
      var vertex = StreetModelForTest.intersectionVertex(s.getCoordinate().asJtsCoordinate());
      StreetModelForTest.streetEdge(vertex, center);
      graph.addVertex(vertex);
    });
    graph.index();
    graph.calculateConvexHull();
    return graph;
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
}
