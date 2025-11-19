package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class BikeRequestTest {

  public static final double SPEED = 2.0;
  public static final double RELUCTANCE = 1.2;
  public static final int BOARD_COST = 660;
  public static final TimeSlopeSafetyTriangle TRIANGLE = TimeSlopeSafetyTriangle.of()
    .withSlope(1)
    .build();
  public static final VehicleRoutingOptimizeType OPTIMIZE_TYPE =
    VehicleRoutingOptimizeType.TRIANGLE;
  public static final int RENTAL_PICKUP_TIME = 30;
  public static final Cost PARK_COST = Cost.costOfSeconds(30);

  private final BikeRequest subject = BikeRequest.of()
    .withSpeed(SPEED)
    .withReluctance(RELUCTANCE)
    .withBoardCost(BOARD_COST)
    .withOptimizeType(OPTIMIZE_TYPE)
    .withRental(rental -> rental.withPickupTime(Duration.ofSeconds(RENTAL_PICKUP_TIME)).build())
    .withParking(parking -> parking.withCost(PARK_COST).build())
    .withOptimizeTriangle(it -> it.withSlope(1).build())
    .build();

  @Test
  void speed() {
    assertEquals(SPEED, subject.speed());
  }

  @Test
  void reluctance() {
    assertEquals(RELUCTANCE, subject.reluctance());
  }

  @Test
  void boardCost() {
    assertEquals(BOARD_COST, subject.boardCost());
  }

  @Test
  void optimizeType() {
    assertEquals(OPTIMIZE_TYPE, subject.optimizeType());
  }

  @Test
  void optimizeTriangle() {
    assertEquals(TRIANGLE, subject.optimizeTriangle());
  }

  @Test
  void rental() {
    RentalRequest.Builder builder = RentalRequest.of();
    var vehicleRental = builder.withPickupTime(Duration.ofSeconds(RENTAL_PICKUP_TIME)).build();
    assertEquals(vehicleRental, subject.rental());
  }

  @Test
  void parking() {
    var vehicleParking = ParkingRequest.of().withCost(PARK_COST).build();
    assertEquals(vehicleParking, subject.parking());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(BikeRequest.DEFAULT, BikeRequest.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withSpeed(0.7).build();
    var same = other.copyOf().withSpeed(SPEED).build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("BikeRequest{}", BikeRequest.DEFAULT.toString());
    assertEquals(
      "BikeRequest{" +
      "speed: 2.0, " +
      "reluctance: 1.2, " +
      "boardCost: $660, " +
      "parking: ParkingRequest{cost: $30}, " +
      "rental: RentalRequest{pickupTime: 30s}, " +
      "optimizeType: TRIANGLE, " +
      "optimizeTriangle: TimeSlopeSafetyTriangle[time=0.0, slope=1.0, safety=0.0]" +
      "}",
      subject.toString()
    );
  }

  @Test
  void testForcedTriangleOptimization() {
    var triangleRequest = BikeRequest.of()
      .withForcedOptimizeTriangle(it -> it.withSlope(1).build())
      .build();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, triangleRequest.optimizeType());

    var conflictingRequest = BikeRequest.of()
      .withOptimizeType(VehicleRoutingOptimizeType.SAFE_STREETS)
      .withForcedOptimizeTriangle(it -> it.withSlope(1).build())
      .build();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, conflictingRequest.optimizeType());

    var emptyTriangleRequest = BikeRequest.of()
      .withOptimizeType(VehicleRoutingOptimizeType.SAFE_STREETS)
      .withForcedOptimizeTriangle(it -> it.build())
      .build();
    assertEquals(VehicleRoutingOptimizeType.SAFE_STREETS, emptyTriangleRequest.optimizeType());
  }
}
