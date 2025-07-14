package org.opentripplanner.graph_builder.module.nearbystops;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class StreetNearbyStopFinderTest extends GraphRoutingTest {

  private static final WgsCoordinate origin = new WgsCoordinate(0.0, 0.0);
  private TransitStopVertex isolatedStop;
  private TransitStopVertex stopA;
  private TransitStopVertex stopB;
  private TransitStopVertex stopC;
  private TransitStopVertex stopD;

  @BeforeEach
  protected void setUp() throws Exception {
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var isolated = intersection("isolated", origin.moveNorthMeters(1000));

          var A = intersection("A", origin);
          var B = intersection("B", origin.moveEastMeters(100));
          var C = intersection("C", origin.moveEastMeters(200));
          var D = intersection("D", origin.moveEastMeters(300));

          biStreet(A, B, 100);
          biStreet(B, C, 100);
          biStreet(C, D, 100);

          isolatedStop = stop("IsolatedStop", isolated.toWgsCoordinate());
          stopA = stop("StopA", A.toWgsCoordinate());
          stopB = stop("StopB", B.toWgsCoordinate());
          stopC = stop("StopC", C.toWgsCoordinate());
          stopD = stop("StopD", D.toWgsCoordinate());

          biLink(A, stopA);
          biLink(B, stopB);
          biLink(C, stopC);
          biLink(D, stopD);
        }
      }
    );
  }

  @Test
  void testIsolatedStop() {
    var durationLimit = Duration.ofMinutes(10);
    var maxStopCount = 0;
    var finder = new StreetNearbyStopFinder(durationLimit, maxStopCount, null);

    var nearbyStops = finder.findNearbyStops(
      isolatedStop,
      RouteRequest.defaultValue(),
      StreetRequest.DEFAULT,
      false
    );

    assertThat(nearbyStops).hasSize(1);
    var nearbyStop = nearbyStops.stream().findFirst().get();
    assertZeroDistanceStop(isolatedStop, nearbyStop);
  }

  @Test
  void testMultipleStops() {
    var durationLimit = Duration.ofMinutes(10);
    var maxStopCount = 0;
    var finder = new StreetNearbyStopFinder(durationLimit, maxStopCount, null);

    var sortedNearbyStops = sort(
      finder.findNearbyStops(stopA, RouteRequest.defaultValue(), StreetRequest.DEFAULT, false)
    );

    assertThat(sortedNearbyStops).hasSize(4);
    assertZeroDistanceStop(stopA, sortedNearbyStops.get(0));
    assertStopAtDistance(stopB, 100, sortedNearbyStops.get(1));
    assertStopAtDistance(stopC, 200, sortedNearbyStops.get(2));
    assertStopAtDistance(stopD, 300, sortedNearbyStops.get(3));
  }

  @Test
  void testMaxStopCount() {
    var durationLimit = Duration.ofMinutes(10);
    var maxStopCount = 2;
    var finder = new StreetNearbyStopFinder(durationLimit, maxStopCount, null);

    var sortedNearbyStops = sort(
      finder.findNearbyStops(stopA, RouteRequest.defaultValue(), StreetRequest.DEFAULT, false)
    );

    assertThat(sortedNearbyStops).hasSize(2);
    assertZeroDistanceStop(stopA, sortedNearbyStops.get(0));
    assertStopAtDistance(stopB, 100, sortedNearbyStops.get(1));
  }

  @Test
  void testDurationLimit() {
    // If we only allow walk for 101 seconds and speed is 1 m/s we should only be able to reach
    // one extra stop.
    var durationLimit = Duration.ofSeconds(101);
    var maxStopCount = 0;
    var routeRequest = RouteRequest.of()
      .withPreferences(b -> b.withWalk(w -> w.withSpeed(1.0)))
      .buildDefault();

    var finder = new StreetNearbyStopFinder(durationLimit, maxStopCount, null);
    var sortedNearbyStops = sort(
      finder.findNearbyStops(stopA, routeRequest, StreetRequest.DEFAULT, false)
    );

    assertThat(sortedNearbyStops).hasSize(2);
    assertZeroDistanceStop(stopA, sortedNearbyStops.get(0));
    assertStopAtDistance(stopB, 100, sortedNearbyStops.get(1));
  }

  @Test
  void testIgnoreStops() {
    var durationLimit = Duration.ofMinutes(10);
    var maxStopCount = 0;
    Set<Vertex> ignore = Set.of(stopA, stopB);
    var finder = new StreetNearbyStopFinder(durationLimit, maxStopCount, null, ignore);

    var sortedNearbyStops = sort(
      finder.findNearbyStops(
        Set.of(stopA),
        RouteRequest.defaultValue(),
        StreetRequest.DEFAULT,
        false
      )
    );

    assertThat(sortedNearbyStops).hasSize(2);
    assertStopAtDistance(stopC, 200, sortedNearbyStops.get(0));
    assertStopAtDistance(stopD, 300, sortedNearbyStops.get(1));
  }

  @Test
  void testIgnoreStopsWithMaxStops() {
    var durationLimit = Duration.ofMinutes(10);
    var maxStopCount = 1;
    Set<Vertex> ignore = Set.of(stopA, stopB);
    var finder = new StreetNearbyStopFinder(durationLimit, maxStopCount, null, ignore);

    var sortedNearbyStops = sort(
      finder.findNearbyStops(
        Set.of(stopA),
        RouteRequest.defaultValue(),
        StreetRequest.DEFAULT,
        false
      )
    );

    assertThat(sortedNearbyStops).hasSize(1);
    assertStopAtDistance(stopC, 200, sortedNearbyStops.get(0));
  }

  static List<NearbyStop> sort(Collection<NearbyStop> stops) {
    return stops.stream().sorted(Comparator.comparing(x -> x.distance)).toList();
  }

  /**
   * Verify that the nearby stop is zero distance and corresponds to the expected vertex
   */
  static void assertZeroDistanceStop(TransitStopVertex expected, NearbyStop nearbyStop) {
    assertEquals(expected.getStop(), nearbyStop.stop);
    assertEquals(0, nearbyStop.distance);
    assertEquals(0, nearbyStop.edges.size());
    assertEquals(expected, nearbyStop.state.getVertex());
    assertNull(nearbyStop.state.getBackState());
  }

  /**
   * Verify that the nearby stop is at a specific distance and corresponds to the expected vertex
   */
  static void assertStopAtDistance(
    TransitStopVertex expected,
    double expectedDistance,
    NearbyStop nearbyStop
  ) {
    assertEquals(expected.getStop(), nearbyStop.stop);
    assertEquals(expectedDistance, nearbyStop.distance);
    assertEquals(expected, nearbyStop.state.getVertex());
    assertFalse(nearbyStop.edges.isEmpty());
    assertNotNull(nearbyStop.state.getBackState());
  }
}
