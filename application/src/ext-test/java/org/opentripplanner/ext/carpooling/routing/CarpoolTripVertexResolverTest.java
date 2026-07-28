package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

class CarpoolTripVertexResolverTest extends GraphRoutingTest {

  /** Escape distance matched to the ~100 m test streets: 50 m escapes; a ~35 m island cannot. */
  private static final CarReachableVertexSnapper SNAPPER = new CarReachableVertexSnapper(50);

  /**
   * A mid-edge route point resolves to the nearer edge endpoint, one vertex per point in order.
   * Graph: two-way all-modes {@code A --(100 m)-- B}; boards ~30 % along (nearer A), alights ~80 %
   * (nearer B).
   */
  @Test
  void midEdgeRoutePointsResolveToNearerEndpoints() {
    var v = new IntersectionVertex[2];
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("A", 60.0000, 10.0000);
          v[1] = intersection("B", 60.0000, 10.0018);
          street(v[0], v[1], 100, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        }
      }
    );

    var trip = CarpoolTripTestData.createSimpleTrip(
      new WgsCoordinate(60.0000, 10.0005),
      new WgsCoordinate(60.0000, 10.0014)
    );
    var resolved = resolverFor(model).resolve(trip);

    assertNotNull(resolved);
    assertEquals(v[0], resolved.vertices().get(0));
    assertEquals(v[1], resolved.vertices().get(1));
  }

  /**
   * A trip with any unresolvable route point resolves to {@code null}: the boarding point sits on a
   * car-only island the walk fallback cannot leave.
   */
  @Test
  void tripWithUnresolvableRoutePointResolvesToNull() {
    var v = new IntersectionVertex[4];
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("I1", 60.0000, 10.0000);
          v[1] = intersection("I2", 60.0000, 10.0006);
          v[2] = intersection("M1", 60.0100, 10.0000);
          v[3] = intersection("M2", 60.0100, 10.0018);
          street(v[0], v[1], 35, StreetTraversalPermission.CAR, StreetTraversalPermission.CAR);
          street(v[2], v[3], 100, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        }
      }
    );

    var trip = CarpoolTripTestData.createSimpleTrip(
      new WgsCoordinate(60.0000, 10.0003),
      new WgsCoordinate(60.0100, 10.0009)
    );

    assertNull(resolverFor(model).resolve(trip));
  }

  /**
   * A route point whose own linking offers no car-reachable vertex falls back to the walk search and
   * relocates onto the drivable network. Both ends of a one-way corridor are car-permitting but
   * neither can be both arrived at and departed from, so both points walk ~56 m to M, the only
   * vertex that can. Graph (one-way car forward, reverse pedestrian):
   * <pre>
   *   O --(car, ~56 m)--> M --(car, ~56 m)--> D
   * </pre>
   */
  @Test
  void routePointWalksOutWhenItsLinkingIsNotCarReachable() {
    var v = new IntersectionVertex[3];
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("O", 60.0000, 10.0000);
          v[1] = intersection("M", 60.0000, 10.0010);
          v[2] = intersection("D", 60.0000, 10.0020);
          street(
            v[0],
            v[1],
            56,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(
            v[1],
            v[2],
            56,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.PEDESTRIAN
          );
        }
      }
    );

    var trip = CarpoolTripTestData.createSimpleTrip(v[0].toWgsCoordinate(), v[2].toWgsCoordinate());
    var resolved = resolverFor(model).resolve(trip);

    assertNotNull(resolved);
    assertEquals(
      v[1],
      resolved.vertices().get(0),
      "The origin O cannot be arrived at, so it must walk out to M"
    );
    assertEquals(
      v[1],
      resolved.vertices().get(1),
      "The destination D cannot be departed from, so it must walk back to M"
    );
  }

  private static CarpoolTripVertexResolver resolverFor(TestOtpModel model) {
    return new CarpoolTripVertexResolver(
      new VertexCreationService(VertexLinkerTestFactory.of(model.graph())),
      SNAPPER
    );
  }
}
