package org.opentripplanner.graph_builder.module.nearbystops;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinderTest.sort;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

class StreetNearbyStopFinderMultipleLinksTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(0.0, 0.0);
  private TransitStopVertex stopA;
  private TransitStopVertex stopB;
  private TransitStopVertex stopC;
  private StopResolver stopResolver;

  @BeforeEach
  protected void setUp() throws Exception {
    var model = modelOf(
      new Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          var B = intersection("B", ORIGIN.moveEastMeters(100));
          var C = intersection("C", ORIGIN.moveEastMeters(200));

          biStreet(A, B, 100);
          biStreet(B, C, 100);

          stopA = stop("StopA", A.toWgsCoordinate());
          stopB = stop("StopB", B.toWgsCoordinate());
          stopC = stop("StopC", C.toWgsCoordinate());

          biLink(A, stopA);

          // B has many links
          biLink(B, stopB);
          biLink(B, stopB);
          biLink(B, stopB);
          biLink(B, stopB);

          biLink(C, stopC);
        }
      }
    );
    this.stopResolver = new SiteRepositoryResolver(model.timetableRepository().getSiteRepository());
  }

  @Test
  void testMaxStopCountRegression() {
    // Max-stop-count should work correctly even though there are multiple links B <-> stopB
    var durationLimit = Duration.ofMinutes(10);
    var maxStopCount = 3;
    var finder = StreetNearbyStopFinder.of(stopResolver, durationLimit, maxStopCount).build();

    var sortedNearbyStops = sort(
      finder.findNearbyStops(stopA, RouteRequest.defaultValue(), StreetMode.WALK, false)
    );

    assertThat(sortedNearbyStops).hasSize(3);
    assertZeroDistanceStop(stopA, sortedNearbyStops.get(0));
    assertStopAtDistance(stopB, 100, sortedNearbyStops.get(1));
    assertStopAtDistance(stopC, 200, sortedNearbyStops.get(2));
  }

  /**
   * Verify that the nearby stop is zero distance and corresponds to the expected vertex
   */
  void assertZeroDistanceStop(TransitStopVertex expected, NearbyStop nearbyStop) {
    assertEquals(stopResolver.getRegularStop(expected.getId()), nearbyStop.stop);
    assertEquals(0, nearbyStop.distance);
    assertEquals(0, nearbyStop.edges.size());
    assertEquals(expected, nearbyStop.state.getVertex());
    assertNull(nearbyStop.state.getBackState());
  }

  /**
   * Verify that the nearby stop is at a specific distance and corresponds to the expected vertex
   */
  void assertStopAtDistance(
    TransitStopVertex expected,
    double expectedDistance,
    NearbyStop nearbyStop
  ) {
    assertEquals(stopResolver.getRegularStop(expected.getId()), nearbyStop.stop);
    assertEquals(expectedDistance, nearbyStop.distance);
    assertEquals(expected, nearbyStop.state.getVertex());
    assertFalse(nearbyStop.edges.isEmpty());
    assertNotNull(nearbyStop.state.getBackState());
  }
}
