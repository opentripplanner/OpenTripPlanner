package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.core.model.basic.Cost.costOfSeconds;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;

class RentalRequestTest {

  public static final int PICKUP_TIME = 25;
  public static final int PICKUP_COST = 250;
  public static final int DROPOFF_TIME = 45;
  public static final int DROPOFF_COST = 450;
  public static final Cost ARRIVE_IN_RENTAL_COST = costOfSeconds(500);
  public static final boolean USE_AVAILABILITY_INFORMATION = true;
  public static final boolean ALLOW_ARRIVING_IN_RENTED_VEHICLE = true;
  public static final Set<String> ALLOWED_NETWORKS = Set.of("foo");
  public static final Set<String> BANNED_NETWORKS = Set.of("bar");

  private final RentalRequest subject;

  {
    RentalRequest.Builder builder = RentalRequest.of();
    subject = builder
      .withPickupTime(Duration.ofSeconds(PICKUP_TIME))
      .withPickupCost(costOfSeconds(PICKUP_COST))
      .withDropOffTime(Duration.ofSeconds(DROPOFF_TIME))
      .withDropOffCost(costOfSeconds(DROPOFF_COST))
      .withArrivingInRentalVehicleAtDestinationCost(ARRIVE_IN_RENTAL_COST)
      .withUseAvailabilityInformation(USE_AVAILABILITY_INFORMATION)
      .withAllowArrivingInRentedVehicleAtDestination(ALLOW_ARRIVING_IN_RENTED_VEHICLE)
      .withAllowedNetworks(ALLOWED_NETWORKS)
      .withBannedNetworks(BANNED_NETWORKS)
      .build();
  }

  @Test
  void pickupTime() {
    assertEquals(Duration.ofSeconds(PICKUP_TIME), subject.pickupTime());
  }

  @Test
  void pickupCost() {
    assertEquals(costOfSeconds(PICKUP_COST), subject.pickupCost());
  }

  @Test
  void dropoffTime() {
    assertEquals(Duration.ofSeconds(DROPOFF_TIME), subject.dropOffTime());
  }

  @Test
  void dropoffCost() {
    assertEquals(costOfSeconds(DROPOFF_COST), subject.dropOffCost());
  }

  @Test
  void useAvailabilityInformation() {
    assertEquals(USE_AVAILABILITY_INFORMATION, subject.useAvailabilityInformation());
  }

  @Test
  void arrivingInRentalVehicleAtDestinationCost() {
    assertEquals(ARRIVE_IN_RENTAL_COST, subject.arrivingInRentalVehicleAtDestinationCost());
  }

  @Test
  void allowArrivingInRentedVehicleAtDestination() {
    assertEquals(
      ALLOW_ARRIVING_IN_RENTED_VEHICLE,
      subject.allowArrivingInRentedVehicleAtDestination()
    );
  }

  @Test
  void allowedNetworks() {
    assertEquals(ALLOWED_NETWORKS, subject.allowedNetworks());
  }

  @Test
  void bannedNetworks() {
    assertEquals(BANNED_NETWORKS, subject.bannedNetworks());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(RentalRequest.DEFAULT, RentalRequest.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    RentalRequest.Builder builder1 = subject.copyOf();
    var other = builder1.withPickupTime(Duration.ofSeconds(450)).build();
    RentalRequest.Builder builder = other.copyOf();
    var copy = builder.withPickupTime(Duration.ofSeconds(PICKUP_TIME)).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("RentalRequest{}", RentalRequest.DEFAULT.toString());
    assertEquals(
      "RentalRequest{" +
        "pickupTime: 25s, " +
        "pickupCost: $250, " +
        "dropOffTime: 45s, " +
        "dropOffCost: $450, " +
        "useAvailabilityInformation, " +
        "arrivingInRentalVehicleAtDestinationCost: $500, " +
        "allowArrivingInRentedVehicleAtDestination, " +
        "allowedNetworks: [foo], " +
        "bannedNetworks: [bar]" +
        "}",
      subject.toString()
    );
  }
}
