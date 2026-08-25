package org.opentripplanner.ext.carpooling.routing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;

class CarpoolStreetRouterTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);

  private static final CarpoolStreetRouter ROUTER = new CarpoolStreetRouter(
    StreetLimitationParametersService.DEFAULT
  );

  private IntersectionVertex vertexA;
  private IntersectionVertex vertexC;
  private IntersectionVertex vertexDisconnected;

  @BeforeEach
  void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          var B = intersection("B", ORIGIN.moveEastMeters(500));
          var C = intersection("C", ORIGIN.moveEastMeters(1000));
          var Z = intersection("Z", ORIGIN.moveNorthMeters(500));

          biStreet(A, B, 500);
          biStreet(B, C, 500);
          // Z has no edges — disconnected from the rest of the graph

          vertexA = A;
          vertexC = C;
          vertexDisconnected = Z;
        }
      }
    );
  }

  /**
   * Clears the interrupt flag so that a cancellation raised by one test cannot surface as a
   * spurious {@link OTPRequestTimeoutException} in an unrelated test sharing the same thread.
   */
  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  @Test
  void routeBetweenConnectedVertices() {
    assertThat(ROUTER.route(vertexA, vertexC)).isNotNull();
  }

  @Test
  void returnNullWhenNoPathExists() {
    assertThat(ROUTER.route(vertexA, vertexDisconnected)).isNull();
  }

  /**
   * A cancelled search has no verdict on whether the leg is routable, so it must not be reported as
   * a routing failure: the caller memoizes a null return as "unroutable" for every later request.
   * The router carries no state between calls, so the pair routes for real once the cancellation
   * is over.
   */
  @Test
  void propagateCancellationInsteadOfReturningNull() {
    Thread.currentThread().interrupt();
    assertThrows(OTPRequestTimeoutException.class, () -> ROUTER.route(vertexA, vertexC));
    Thread.interrupted();

    assertThat(ROUTER.route(vertexA, vertexC)).isNotNull();
  }
}
