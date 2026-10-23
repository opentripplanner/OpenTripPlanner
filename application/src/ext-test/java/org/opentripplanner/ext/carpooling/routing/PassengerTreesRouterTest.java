package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A straight road where every street takes three minutes to drive, with one trip from a0 to a1 and
 * the passenger halfway:
 * <pre>
 *   beforeStart — · — a0 — · — passenger — · — a1 — · — afterEnd
 * </pre>
 * A search checks its limit when it starts down a street, so a limit cuts the road only at
 * an intersection.
 */
class PassengerTreesRouterTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final double MAX_SPEED = 40.0;
  private static final Duration AN_HOUR = Duration.ofHours(1);

  private Vertex beforeStart;
  private Vertex a0;
  private Vertex passenger;
  private Vertex a1;
  private Vertex afterEnd;
  private List<RoutableCarpoolTrip> trips;

  @BeforeEach
  void buildRoad() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          var road = new ArrayList<IntersectionVertex>();
          for (int i = 0; i <= 8; i++) {
            road.add(intersection("v" + i, ORIGIN.moveEastMeters(i * 1_800)));
          }
          for (int i = 0; i < 8; i++) {
            // 1.8 km at 10 m/s: three minutes.
            street(road.get(i), road.get(i + 1), 1_800, StreetTraversalPermission.ALL, 10);
            street(road.get(i + 1), road.get(i), 1_800, StreetTraversalPermission.ALL, 10);
          }
          beforeStart = road.get(0);
          a0 = road.get(2);
          passenger = road.get(4);
          a1 = road.get(6);
          afterEnd = road.get(8);
        }
      }
    );
    trips = List.of(
      new RoutableCarpoolTrip(
        createSimpleTrip(a0.toWgsCoordinate(), a1.toWgsCoordinate()),
        List.of(a0, a1),
        new CarpoolCorridor(
          List.of(Duration.ofMinutes(12)),
          List.of(Duration.ofMinutes(30)),
          List.of()
        )
      )
    );
  }

  @Test
  void passengerTreesAreCapped() {
    // a0 and a1 are two streets from the passenger; capped at two minutes, each tree drives one.
    var capped = PassengerTrees.router(
      passenger,
      trips,
      true,
      MAX_SPEED,
      Duration.ofMinutes(2),
      AN_HOUR
    );
    assertNull(capped.route(passenger, a1));
    assertNull(capped.route(a0, passenger));

    var uncapped = PassengerTrees.router(passenger, trips, true, MAX_SPEED, AN_HOUR, AN_HOUR);
    assertNotNull(uncapped.route(passenger, a1));
    assertNotNull(uncapped.route(a0, passenger));
  }

  @Test
  void onlyTheDriversSideTreeStopsAtTheTripsWaypoints() {
    var access = PassengerTrees.router(passenger, trips, true, MAX_SPEED, AN_HOUR, AN_HOUR);
    assertNotNull(access.route(a0, passenger), "the driver's side reaches a0");
    assertNull(access.route(beforeStart, passenger), "the driver's side stops at a0");
    assertNotNull(access.route(passenger, afterEnd), "the ride goes on past a1");

    var egress = PassengerTrees.router(passenger, trips, false, MAX_SPEED, AN_HOUR, AN_HOUR);
    assertNotNull(egress.route(passenger, a1), "the driver's side reaches a1");
    assertNull(egress.route(passenger, afterEnd), "the driver's side stops at a1");
    assertNotNull(egress.route(beforeStart, passenger), "the ride comes from before a0");
  }

  @Test
  void onlyTheRideSideTreeStopsAtTheRequestsMaximumLeg() {
    var maxLeg = Duration.ofMinutes(2);

    var access = PassengerTrees.router(passenger, trips, true, MAX_SPEED, AN_HOUR, maxLeg);
    assertNotNull(access.route(a0, passenger), "the driver's side reaches a0, six minutes away");
    assertNull(access.route(passenger, a1), "the ride stops at two minutes");

    var egress = PassengerTrees.router(passenger, trips, false, MAX_SPEED, AN_HOUR, maxLeg);
    assertNotNull(egress.route(passenger, a1), "the driver's side reaches a1, six minutes away");
    assertNull(egress.route(a0, passenger), "the ride stops at two minutes");
  }
}
