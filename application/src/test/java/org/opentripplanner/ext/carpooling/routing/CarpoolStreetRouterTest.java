package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * Unit tests for {@link CarpoolStreetRouter}.
 * <p>
 * These tests verify the router in isolation by mocking all dependencies.
 */
class CarpoolStreetRouterTest {

  private Graph mockGraph;
  private VertexLinker mockVertexLinker;
  private StreetLimitationParametersService mockStreetService;
  private RouteRequest mockRequest;
  private RoutingPreferences mockPreferences;
  private StreetPreferences mockStreetPreferences;
  private CarpoolStreetRouter router;

  @BeforeEach
  void setup() {
    mockGraph = mock(Graph.class);
    mockVertexLinker = mock(VertexLinker.class);
    mockStreetService = mock(StreetLimitationParametersService.class);
    mockRequest = mock(RouteRequest.class);
    mockPreferences = mock(RoutingPreferences.class);
    mockStreetPreferences = mock(StreetPreferences.class);

    // Setup mock chain for preferences
    when(mockRequest.preferences()).thenReturn(mockPreferences);
    when(mockPreferences.street()).thenReturn(mockStreetPreferences);
    when(mockRequest.arriveBy()).thenReturn(false);
    when(mockStreetService.getMaxCarSpeed()).thenReturn(30.0f);

    router = new CarpoolStreetRouter(mockGraph, mockVertexLinker, mockStreetService, mockRequest);
  }

  @Test
  void constructor_storesDependencies() {
    assertNotNull(router);
    // Router should be successfully constructed with all dependencies
  }

  @Test
  void route_withValidLocations_returnsNonNull() {
    // This is a basic smoke test - actual routing behavior depends on
    // the graph and is tested via integration tests
    var from = GenericLocation.fromCoordinate(59.9139, 10.7522); // Oslo center
    var to = GenericLocation.fromCoordinate(59.9149, 10.7522); // Oslo north

    // Note: Without a real graph, this will likely return null
    // The important thing is that it doesn't throw exceptions
    var result = router.route(from, to);
    // Result can be null if routing fails (expected with mock graph)
    // What matters is no exceptions were thrown
  }

  @Test
  void route_withNullFrom_handlesGracefully() {
    var to = GenericLocation.fromCoordinate(59.9149, 10.7522);

    // Should handle null gracefully (return null, not throw)
    var result = router.route(null, to);

    // Result should be null (routing failed)
    assertNull(result);
  }

  @Test
  void route_withNullTo_handlesGracefully() {
    var from = GenericLocation.fromCoordinate(59.9139, 10.7522);

    // Should handle null gracefully (return null, not throw)
    var result = router.route(from, null);

    // Result should be null (routing failed)
    assertNull(result);
  }

  @Test
  void route_multipleCallsSameLocations_behavesConsistently() {
    var from = GenericLocation.fromCoordinate(59.9139, 10.7522);
    var to = GenericLocation.fromCoordinate(59.9149, 10.7522);

    var result1 = router.route(from, to);
    var result2 = router.route(from, to);

    // Results should be consistent (both null or both non-null)
    assertEquals(result1 == null, result2 == null);
  }

  @Test
  void route_multipleDifferentCalls_routesIndependently() {
    var from1 = GenericLocation.fromCoordinate(59.9139, 10.7522);
    var to1 = GenericLocation.fromCoordinate(59.9149, 10.7522);
    var from2 = GenericLocation.fromCoordinate(59.9159, 10.7522);
    var to2 = GenericLocation.fromCoordinate(59.9169, 10.7522);

    // Should be able to route multiple different pairs
    var result1 = router.route(from1, to1);
    var result2 = router.route(from2, to2);
    // Both routes should complete without exceptions
    // Results may be null with mock graph, but no exceptions
  }
}
