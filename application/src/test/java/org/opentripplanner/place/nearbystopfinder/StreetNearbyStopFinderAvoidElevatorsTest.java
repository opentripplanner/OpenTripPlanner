package org.opentripplanner.place.nearbystopfinder;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.streetadapter.VertexFactory;

/**
 * Three stops in a row. The elevator {@code e1} connects A and B, and there is a longer street
 * between them. The elevator {@code e2} is the only connection between B and C.
 */
class StreetNearbyStopFinderAvoidElevatorsTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(0.0, 0.0);
  private TransitStopVertex stopA;
  private TransitStopVertex stopB;
  private TransitStopVertex stopC;

  @BeforeEach
  void setUp() throws Exception {
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var a = intersection("A", ORIGIN);
          var b = intersection("B", ORIGIN.moveEastMeters(100));
          var c = intersection("C", ORIGIN.moveEastMeters(200));
          var detour = intersection("Detour", ORIGIN.moveNorthMeters(500));

          biStreet(a, detour, 500);
          biStreet(detour, b, 500);
          elevator("e1", a, b);
          elevator("e2", b, c);

          stopA = stop("StopA", a.toWgsCoordinate());
          stopB = stop("StopB", b.toWgsCoordinate());
          stopC = stop("StopC", c.toWgsCoordinate());
          biLink(a, stopA);
          biLink(b, stopB);
          biLink(c, stopC);
        }

        private void elevator(String id, StreetVertex from, StreetVertex to) {
          var factory = new VertexFactory(graph());
          var fromHop = factory.elevator(from, from.getLabelString() + "_" + id);
          var toHop = factory.elevator(to, to.getLabelString() + "_" + id);
          ElevatorBoardEdge.createElevatorBoardEdge(from, fromHop);
          ElevatorAlightEdge.createElevatorAlightEdge(fromHop, from);
          ElevatorBoardEdge.createElevatorBoardEdge(to, toHop);
          ElevatorAlightEdge.createElevatorAlightEdge(toHop, to);
          ElevatorHopEdge.bidirectional(
            fromHop,
            toHop,
            StreetTraversalPermission.ALL,
            Accessibility.POSSIBLE,
            1,
            -1,
            id
          );
        }
      }
    );
  }

  @Test
  void withoutAvoidedElevatorsThePathsUseTheElevators() {
    var nearby = find(Set.of());

    assertThat(elevatorIdsTo(nearby, stopB)).containsExactly("e1");
    assertThat(elevatorIdsTo(nearby, stopC)).containsExactly("e1", "e2");
  }

  @Test
  void anAvoidedElevatorIsNotUsedAndTheDetourIsFound() {
    var nearby = find(Set.of("e1"));

    assertThat(elevatorIdsTo(nearby, stopB)).isEmpty();
    assertThat(elevatorIdsTo(nearby, stopC)).containsExactly("e2");
  }

  @Test
  void aStopThatCanOnlyBeReachedWithAnAvoidedElevatorIsNotFound() {
    var nearby = find(Set.of("e2"));

    assertThat(elevatorIdsTo(nearby, stopB)).containsExactly("e1");
    assertThat(nearby.stream().map(NearbyStop::stopId)).doesNotContain(stopC.getId());
  }

  @Test
  void anElevatorWithAnotherIdIsNotAvoided() {
    var nearby = find(Set.of("other"));

    assertThat(elevatorIdsTo(nearby, stopC)).containsExactly("e1", "e2");
  }

  private Collection<NearbyStop> find(Set<String> avoidElevatorIds) {
    return StreetNearbyStopFinder.of(null)
      .build()
      .findNearbyStops(
        stopA,
        RouteRequest.defaultValue(),
        StreetMode.WALK,
        false,
        Duration.ofHours(1),
        0,
        avoidElevatorIds
      );
  }

  private static Set<String> elevatorIdsTo(Collection<NearbyStop> nearby, TransitStopVertex stop) {
    FeedScopedId stopId = stop.getId();
    var path = nearby
      .stream()
      .filter(it -> it.stopId().equals(stopId))
      .findFirst()
      .orElseThrow();
    return path
      .edges()
      .stream()
      .filter(ElevatorHopEdge.class::isInstance)
      .map(edge -> ((ElevatorHopEdge) edge).id().orElseThrow())
      .collect(Collectors.toSet());
  }
}
