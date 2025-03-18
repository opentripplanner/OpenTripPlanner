package org.opentripplanner.street.integration;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;

public class BicycleParkAndRideTest extends ParkAndRideTest {

  /**
   * B+C Park                          Car Park |    ALL        WALK        WALK    |   ALL A
   * <-------> B --------> C --------> D ---------> E | Bike Park
   */

  private StreetVertex A, B, C, D, E;

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

          street(A, B, 87, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(B, C, 87, StreetTraversalPermission.PEDESTRIAN);
          street(C, D, 87, StreetTraversalPermission.PEDESTRIAN);
          street(D, E, 87, StreetTraversalPermission.ALL);

          vehicleParking(
            "AllPark",
            47.500,
            19.001,
            true,
            true,
            List.of(vehicleParkingEntrance(A, "All Park Entrance", true, true))
          );

          vehicleParking(
            "BikePark",
            47.520,
            19.001,
            true,
            false,
            List.of(vehicleParkingEntrance(C, "BikePark Entrance", false, true))
          );

          vehicleParking(
            "CarPark",
            47.530,
            19.001,
            false,
            true,
            List.of(vehicleParkingEntrance(D, "CarPark Entrance", true, true))
          );
        }
      }
    );
    graph = otpModel.graph();
  }

  // Verify that it is not possible to park at a car-only park
  @Test
  public void bicycleParkingToCarParkingPlaceTest() {
    assertEmptyPath(D, D, StreetMode.BIKE_TO_PARK);
  }

  @Test
  public void bicycleParkingToBicycleParkingPlaceTest() {
    assertPath(
      B,
      D,
      StreetMode.BIKE_TO_PARK,
      "null - null (0.00, 0)",
      "WALK - BC street (327.07, 66)",
      "null - BikePark Entrance (328.07, 66)",
      "null (parked) - BikePark Entrance (448.07, 126)",
      "null (parked) - BikePark Entrance (449.07, 126)",
      "WALK (parked) - CD street (579.89, 191)"
    );
  }

  @Test
  public void bicycleParkingToCarAndBicycleParkingPlaceTest() {
    assertPath(
      B,
      B,
      StreetMode.BIKE_TO_PARK,
      "null - null (0.00, 0)",
      "BICYCLE - AB street (34.80, 18)",
      "null - All Park Entrance (35.80, 18)",
      "null (parked) - All Park Entrance (155.80, 78)",
      "null (parked) - All Park Entrance (156.80, 78)",
      "WALK (parked) - AB street (287.63, 143)"
    );
  }
}
