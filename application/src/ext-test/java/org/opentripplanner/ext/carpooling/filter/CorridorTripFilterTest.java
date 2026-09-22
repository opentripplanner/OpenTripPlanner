package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterEgress;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * One leg, 20 km east, allowed to take 30 minutes: at 40 m/s that reaches 72 km of driving, so a
 * point 10 km along the leg and 5 km off it is in reach and a point 60 km off it is not.
 */
class CorridorTripFilterTest {

  private static final WgsCoordinate A = new WgsCoordinate(63.43, 10.39);
  private static final WgsCoordinate B = A.moveEastMeters(20_000);
  private static final WgsCoordinate NEAR = A.moveEastMeters(10_000).moveNorthMeters(5_000);
  private static final WgsCoordinate FAR = A.moveEastMeters(10_000).moveNorthMeters(60_000);
  private static final Instant TIME = Instant.parse("2024-01-15T09:00:00Z");

  private final CorridorTripFilter filter = new CorridorTripFilter(40.0);

  @Test
  void directNeedsBothThePickupAndTheDropoffInReach() {
    var request = departAfterDirect(TIME);

    assertTrue(filter.isCandidateTrip(trip(), new SnappedPassenger(request, at(NEAR), at(B))));
    assertFalse(filter.isCandidateTrip(trip(), new SnappedPassenger(request, at(NEAR), at(FAR))));
    assertFalse(filter.isCandidateTrip(trip(), new SnappedPassenger(request, at(FAR), at(NEAR))));
  }

  @Test
  void accessAndEgressNeedOnlyThePassengersOwnEnd() {
    assertTrue(
      filter.isCandidateTrip(trip(), new SnappedPassenger(departAfterAccess(TIME), at(NEAR), null))
    );
    assertFalse(
      filter.isCandidateTrip(trip(), new SnappedPassenger(departAfterAccess(TIME), at(FAR), null))
    );
    assertTrue(
      filter.isCandidateTrip(trip(), new SnappedPassenger(departAfterEgress(TIME), null, at(NEAR)))
    );
    assertFalse(
      filter.isCandidateTrip(trip(), new SnappedPassenger(departAfterEgress(TIME), null, at(FAR)))
    );
  }

  private static RoutableCarpoolTrip trip() {
    var corridor = new CarpoolCorridor(
      List.of(Duration.ofMinutes(15)),
      List.of(Duration.ofMinutes(30)),
      List.of()
    );
    return new RoutableCarpoolTrip(
      createSimpleTrip(A, B),
      List.of(vertex("a", A), vertex("b", B)),
      corridor
    );
  }

  private static SnapResult at(WgsCoordinate coordinate) {
    return new SnapResult(vertex("snap", coordinate), null);
  }

  private static Vertex vertex(String label, WgsCoordinate coordinate) {
    return new SimpleVertex(label, coordinate.latitude(), coordinate.longitude());
  }
}
