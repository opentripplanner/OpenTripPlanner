package org.opentripplanner.street.integration;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;

public class CarParkAndRideTest extends ParkAndRideTest {

  /**
   * B+C Park             Bike Park ALL        WALK    |  WALK       WALK    |   ALL A --------> B
   * --------> C -------> D -------> E --------> F \         /                       | Car Park
   * Car Park
   */

  private StreetVertex A, B, C, D, E, F;

  @BeforeEach
  public void setUp() throws Exception {
    var otpModel = modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 47.500, 19.000);
          B = intersection("B", 47.510, 19.000);
          C = intersection("C", 47.520, 19.000);
          D = intersection("D", 47.530, 19.000);
          E = intersection("E", 47.540, 19.000);
          F = intersection("F", 47.550, 19.000);

          street(A, B, 87, StreetTraversalPermission.ALL);
          street(B, C, 87, StreetTraversalPermission.PEDESTRIAN);
          street(C, D, 87, StreetTraversalPermission.PEDESTRIAN);
          street(D, E, 87, StreetTraversalPermission.PEDESTRIAN);
          street(E, F, 87, StreetTraversalPermission.ALL);

          vehicleParking(
            "CarPark #1",
            47.505,
            19.001,
            false,
            true,
            List.of(
              vehicleParkingEntrance(A, "CarPark #1 Entrance A", false, true),
              vehicleParkingEntrance(B, "CarPark #1 Entrance B", true, false)
            ),
            "tag1",
            "tag2",
            "tag3"
          );

          vehicleParking(
            "AllPark",
            47.520,
            19.001,
            true,
            true,
            List.of(vehicleParkingEntrance(C, "AllPark Entrance", true, true))
          );

          vehicleParking(
            "CarPark #2",
            47.530,
            19.001,
            false,
            true,
            true,
            List.of(vehicleParkingEntrance(D, "CarPark #2 Entrance", true, true))
          );

          vehicleParking(
            "BikePark",
            47.540,
            19.000,
            true,
            false,
            List.of(vehicleParkingEntrance(E, "BikePark Entrance", false, true))
          );
        }
      }
    );
    graph = otpModel.graph();
  }

  @Test
  public void carParkingToBicycleParkingPlaceTest() {
    assertEmptyPath(E, F, StreetMode.CAR_TO_PARK);
  }

  @Test
  public void carParkingToCarParkingPlaceTest() {
    assertPath(
      D,
      F,
      StreetMode.CAR_TO_PARK,
      "null - null (0.00, 0)",
      "null - CarPark #2 Entrance (1.00, 0)",
      "null (parked) - CarPark #2 Entrance (241.00, 180)",
      "null (parked) - CarPark #2 Entrance (242.00, 180)",
      "WALK (parked) - DE street (372.83, 246)",
      "WALK (parked) - EF street (503.65, 311)"
    );
  }

  @Test
  public void carParkingToCarAndBicycleParkingPlaceTest() {
    assertPath(
      C,
      F,
      StreetMode.CAR_TO_PARK,
      "null - null (0.00, 0)",
      "null - AllPark Entrance (1.00, 0)",
      "null (parked) - AllPark Entrance (241.00, 180)",
      "null (parked) - AllPark Entrance (242.00, 180)",
      "WALK (parked) - CD street (372.83, 246)",
      "WALK (parked) - DE street (503.65, 311)",
      "WALK (parked) - EF street (634.48, 377)"
    );
  }

  @Test
  public void exitOnWalkableEntranceCarParkingTest() {
    assertPath(
      A,
      B,
      StreetMode.CAR_TO_PARK,
      "null - null (0.00, 0)",
      "CAR - AB street (15.54, 8)",
      "null - CarPark #1 Entrance B (16.54, 8)",
      "null (parked) - CarPark #1 Entrance A (256.54, 188)",
      "null (parked) - CarPark #1 Entrance A (257.54, 188)",
      "WALK (parked) - AB street (388.36, 254)"
    );
  }

  @Test
  public void wheelchairPlacesNotAvailableTest() {
    assertEmptyPath(A, B, StreetMode.CAR_TO_PARK, true);
  }

  @Test
  public void wheelchairPlacesAvailable() {
    assertPath(
      D,
      F,
      StreetMode.CAR_TO_PARK,
      true,
      "null - null (0.00, 0)",
      "null - CarPark #2 Entrance (1.00, 0)",
      "null (parked) - CarPark #2 Entrance (241.00, 180)",
      "null (parked) - CarPark #2 Entrance (242.00, 180)",
      "WALK (parked) - DE street (372.83, 246)",
      "WALK (parked) - EF street (503.65, 311)"
    );
  }

  @Test
  public void noPathIfContainsBannedTags() {
    assertNoPathWithParking(A, B, StreetMode.CAR_TO_PARK, Set.of("tag1"), Set.of());
  }

  @Test
  public void pathIfNoBannedTagsPresent() {
    assertPathWithParking(A, B, StreetMode.CAR_TO_PARK, Set.of("no-such-tag"), Set.of());
  }

  @Test
  public void noPathIfContainsMissingRequiredTags() {
    assertNoPathWithParking(A, B, StreetMode.CAR_TO_PARK, Set.of(), Set.of("no-such-tag"));
  }

  @Test
  public void pathWithRequiredTags() {
    assertPathWithParking(A, B, StreetMode.CAR_TO_PARK, Set.of(), Set.of("tag2"));
  }

  @Test
  public void bannedTagsTakePrecedence() {
    assertNoPathWithParking(A, B, StreetMode.CAR_TO_PARK, Set.of("tag1"), Set.of("tag2"));
  }
}
