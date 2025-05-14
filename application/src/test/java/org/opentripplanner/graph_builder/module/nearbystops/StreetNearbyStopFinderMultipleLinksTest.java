package org.opentripplanner.graph_builder.module.nearbystops;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinderTest.assertStopAtDistance;
import static org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinderTest.assertZeroDistanceStop;
import static org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinderTest.sort;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

class StreetNearbyStopFinderMultipleLinksTest extends GraphRoutingTest {

  private static final WgsCoordinate origin = new WgsCoordinate(0.0, 0.0);
  private TransitStopVertex stopA;
  private TransitStopVertex stopB;
  private TransitStopVertex stopC;

  @BeforeEach
  protected void setUp() throws Exception {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          var A = intersection("A", origin);
          var B = intersection("B", origin.moveEastMeters(100));
          var C = intersection("C", origin.moveEastMeters(200));

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
  }

  @Test
  void testMaxStopCountRegression() {
    // Max-stop-count should work correctly even though there are multiple links B <-> stopB
    var durationLimit = Duration.ofMinutes(10);
    var maxStopCount = 3;
    var finder = new StreetNearbyStopFinder(durationLimit, maxStopCount, null);

    var sortedNearbyStops = sort(
      finder.findNearbyStops(stopA, RouteRequest.defaultValue(), StreetRequest.DEFAULT, false)
    );

    assertThat(sortedNearbyStops).hasSize(3);
    assertZeroDistanceStop(stopA, sortedNearbyStops.get(0));
    assertStopAtDistance(stopB, 100, sortedNearbyStops.get(1));
    assertStopAtDistance(stopC, 200, sortedNearbyStops.get(2));
  }
}
