package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;

class CarRequestTest {

  private static final double RELUCTANCE = 5.111;
  private static final double EXPECTED_RELUCTANCE = 5.1;
  private static final int PICKUP_TIME = 600;
  private static final Cost PICKUP_COST = Cost.costOfSeconds(500);
  private static final double ACCELERATION_SPEED = 3.1;
  private static final double DECELERATION_SPEED = 3.5;
  private static final int RENTAL_PICKUP_TIME = 30;
  private static final Cost PARK_COST = Cost.costOfSeconds(30);

  private final CarRequest subject = CarRequest.of()
    .withReluctance(RELUCTANCE)
    .withPickupTime(Duration.ofSeconds(PICKUP_TIME))
    .withPickupCost(PICKUP_COST)
    .withAccelerationSpeed(ACCELERATION_SPEED)
    .withDecelerationSpeed(DECELERATION_SPEED)
    .withRental(rental -> rental.withPickupTime(Duration.ofSeconds(RENTAL_PICKUP_TIME)).build())
    .withParking(parking -> parking.withCost(PARK_COST).build())
    .build();

  @Test
  void reluctance() {
    assertEquals(EXPECTED_RELUCTANCE, subject.reluctance());
  }

  @Test
  void pickupTime() {
    assertEquals(Duration.ofSeconds(PICKUP_TIME), subject.pickupTime());
  }

  @Test
  void pickupCost() {
    assertEquals(PICKUP_COST, subject.pickupCost());
  }

  @Test
  void accelerationSpeed() {
    assertEquals(ACCELERATION_SPEED, subject.accelerationSpeed());
  }

  @Test
  void decelerationSpeed() {
    assertEquals(DECELERATION_SPEED, subject.decelerationSpeed());
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
  void testCopyOfEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(CarRequest.DEFAULT, CarRequest.of().build());
    assertSame(subject, subject.copyOf().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withReluctance(0.0).build();
    var same = other.copyOf().withReluctance(RELUCTANCE).build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("CarRequest{}", CarRequest.DEFAULT.toString());
    assertEquals(
      "CarRequest{" +
        "reluctance: 5.1, " +
        "parking: ParkingRequest{cost: $30}, " +
        "rental: RentalRequest{pickupTime: 30s}, " +
        "pickupTime: PT10M, " +
        "pickupCost: $500, " +
        "accelerationSpeed: 3.1, decelerationSpeed: 3.5" +
        "}",
      subject.toString()
    );
  }
}
