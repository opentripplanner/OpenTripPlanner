package org.opentripplanner.graph_builder.module.nearbystops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;

class NearbyStopFinderVisitorTest {

  private static final TransitTestEnvironmentBuilder ENV = TransitTestEnvironment.of();
  static final RegularStop STOP = ENV.stop("stop-1");
  static final AreaStop AREA_STOP = ENV.areaStop("area-1");

  private static final StopResolver STOP_RESOLVER = new StopResolver() {
    @Override
    public RegularStop getRegularStop(FeedScopedId id) {
      return STOP;
    }

    @Override
    public AreaStop getAreaStop(FeedScopedId id) {
      return AREA_STOP;
    }
  };

  @BeforeAll
  static void setup() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
  }

  @AfterAll
  static void teardown() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }

  @Test
  void collectsTransitStops() {
    var visitor = new NearbyStopFinderVisitor(STOP_RESOLVER, Set.of(), Set.of(), false);

    var state = TestStateBuilder.ofWalking().streetEdge().stop(STOP).build();
    visitor.visitVertex(state);

    var expected = NearbyStop.nearbyStopForState(state, STOP);
    assertEquals(List.of(expected), visitor.transitStopsFound());
  }

  @Test
  void skipsOriginVertices() {
    var state = TestStateBuilder.ofWalking().streetEdge().stop(STOP).build();
    var originVertices = Set.of(state.getVertex());
    var visitor = new NearbyStopFinderVisitor(STOP_RESOLVER, originVertices, Set.of(), false);

    visitor.visitVertex(state);

    assertTrue(visitor.transitStopsFound().isEmpty());
  }

  @Test
  void skipsIgnoreVertices() {
    var state = TestStateBuilder.ofWalking().streetEdge().stop(STOP).build();
    var ignoreVertices = Set.of(state.getVertex());
    var visitor = new NearbyStopFinderVisitor(STOP_RESOLVER, Set.of(), ignoreVertices, false);

    visitor.visitVertex(state);

    assertTrue(visitor.transitStopsFound().isEmpty());
  }

  @Test
  void skipsNonTransitVertices() {
    var visitor = new NearbyStopFinderVisitor(STOP_RESOLVER, Set.of(), Set.of(), false);

    // State at a regular intersection, not a transit stop
    var state = TestStateBuilder.ofWalking().streetEdge().build();
    visitor.visitVertex(state);

    assertTrue(visitor.transitStopsFound().isEmpty());
  }

  @Test
  void collectsAreaStopsWhenFlexEnabled() {
    var vertex = StreetModelForTest.intersectionVertex(10, 10);
    var other = StreetModelForTest.intersectionVertex(10.1, 10.1);
    StreetModelForTest.streetEdge(vertex, other, StreetTraversalPermission.CAR);
    vertex.addAreaStops(Set.of(AREA_STOP.getId()));

    var state = new State(vertex, StreetSearchRequest.of().withStartTime(Instant.EPOCH).build());
    var visitor = new NearbyStopFinderVisitor(STOP_RESOLVER, Set.of(), Set.of(), false);
    visitor.visitVertex(state);

    assertEquals(1, visitor.areaStopStates().size());
    assertTrue(visitor.areaStopStates().containsKey(AREA_STOP));
    assertEquals(state, visitor.areaStopStates().get(AREA_STOP).iterator().next());
  }

  @Test
  void doesNotCollectAreaStopsWithoutCarPermission() {
    var vertex = StreetModelForTest.intersectionVertex(20, 20);
    var other = StreetModelForTest.intersectionVertex(20.1, 20.1);
    StreetModelForTest.streetEdge(vertex, other, StreetTraversalPermission.PEDESTRIAN);
    vertex.addAreaStops(Set.of(AREA_STOP.getId()));

    var state = new State(vertex, StreetSearchRequest.of().withStartTime(Instant.EPOCH).build());
    var visitor = new NearbyStopFinderVisitor(STOP_RESOLVER, Set.of(), Set.of(), false);
    visitor.visitVertex(state);

    assertTrue(visitor.areaStopStates().isEmpty());
  }

  @Test
  void collectsAreaStopsInReverseDirection() {
    var vertex = StreetModelForTest.intersectionVertex(30, 30);
    var other = StreetModelForTest.intersectionVertex(30.1, 30.1);
    // Create incoming CAR edge: other -> vertex
    StreetModelForTest.streetEdge(other, vertex, StreetTraversalPermission.CAR);
    vertex.addAreaStops(Set.of(AREA_STOP.getId()));

    var state = new State(vertex, StreetSearchRequest.of().withStartTime(Instant.EPOCH).build());
    var visitor = new NearbyStopFinderVisitor(STOP_RESOLVER, Set.of(), Set.of(), true);
    visitor.visitVertex(state);

    assertEquals(1, visitor.areaStopStates().size());
    assertTrue(visitor.areaStopStates().containsKey(AREA_STOP));
  }
}
