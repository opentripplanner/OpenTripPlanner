package org.opentripplanner.street.model.edge;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class TemporaryFreeEdgeTest {

  private static final Vertex FROM = intersectionVertex("from", 1.0, 2.0);
  private static final Vertex TO = new TemporaryStreetLocation(
    new Coordinate(0, 0),
    I18NString.of("to")
  );

  @Test
  void traverse() {
    var edge = new TemporaryFreeEdge(FROM, TO, StreetTraversalPermission.ALL);
    StreetSearchRequest options = StreetSearchRequest.of().build();
    State s0 = new State(FROM, options);
    var states = edge.traverse(s0);
    assertThat(states).hasLength(1);
    var state = states[0];
    assertEquals(TO, state.getVertex());
    assertEquals(TraverseMode.WALK, state.currentMode());
    assertEquals(1, state.weight, 0.0001);
  }

  @Test
  void traverseRestrictedAndBlocked() {
    var edge = new TemporaryFreeEdge(FROM, TO, StreetTraversalPermission.PEDESTRIAN);
    StreetSearchRequest options = StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    State s0 = new State(FROM, options);
    var states = edge.traverse(s0);
    assertThat(states).isEmpty();
  }

  @Test
  void traverseRestrictedButAllowed() {
    var edge = new TemporaryFreeEdge(FROM, TO, StreetTraversalPermission.PEDESTRIAN);
    StreetSearchRequest options = StreetSearchRequest.of().withMode(StreetMode.WALK).build();
    State s0 = new State(FROM, options);
    var states = edge.traverse(s0);
    assertThat(states).hasLength(1);
  }

  @Test
  void traverseBiking() {
    var edge = new TemporaryFreeEdge(FROM, TO, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
    StreetSearchRequest options = StreetSearchRequest.of().withMode(StreetMode.BIKE).build();
    State s0 = new State(FROM, options);
    var states = edge.traverse(s0);
    assertThat(states).hasLength(1);
    var state = states[0];
    assertEquals(TraverseMode.BICYCLE, state.currentMode());
    assertFalse(state.isBackWalkingBike());
    assertEquals(1, state.weight, 0.0001);
  }

  @Test
  void traverseBikeWalking() {
    var edge = new TemporaryFreeEdge(FROM, TO, StreetTraversalPermission.PEDESTRIAN);
    StreetSearchRequest options = StreetSearchRequest.of().withMode(StreetMode.BIKE).build();
    State s0 = new State(FROM, options);
    var states = edge.traverse(s0);
    assertThat(states).hasLength(1);
    var state = states[0];
    assertEquals(TraverseMode.BICYCLE, state.currentMode());
    assertTrue(state.isBackWalkingBike());
    // Bike walking related cost won't be applied on the first edge
    assertEquals(1, state.weight, 0.0001);
  }
}
