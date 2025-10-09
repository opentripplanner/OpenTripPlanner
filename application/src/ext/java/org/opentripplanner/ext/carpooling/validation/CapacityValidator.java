package org.opentripplanner.ext.carpooling.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that inserting a passenger won't exceed vehicle capacity.
 * <p>
 * Checks all positions between pickup and dropoff to ensure capacity
 * constraints are maintained throughout the passenger's journey.
 */
public class CapacityValidator implements InsertionValidator {

  private static final Logger LOG = LoggerFactory.getLogger(CapacityValidator.class);

  @Override
  public ValidationResult validate(ValidationContext context) {
    // Check capacity at pickup position
    int pickupPassengers = context
      .passengerTimeline()
      .getPassengerCount(context.pickupPosition() - 1);
    int capacity = context.passengerTimeline().getCapacity();

    if (pickupPassengers >= capacity) {
      String reason = String.format(
        "No capacity at pickup position %d: %d passengers, %d capacity",
        context.pickupPosition(),
        pickupPassengers,
        capacity
      );
      LOG.debug(reason);
      return ValidationResult.invalid(reason);
    }

    // Check capacity throughout the journey (pickup to dropoff)
    boolean hasCapacity = context
      .passengerTimeline()
      .hasCapacityInRange(context.pickupPosition(), context.dropoffPosition(), 1);

    if (!hasCapacity) {
      String reason = String.format(
        "Capacity exceeded between positions %d and %d",
        context.pickupPosition(),
        context.dropoffPosition()
      );
      LOG.debug(reason);
      return ValidationResult.invalid(reason);
    }

    return ValidationResult.valid();
  }
}
