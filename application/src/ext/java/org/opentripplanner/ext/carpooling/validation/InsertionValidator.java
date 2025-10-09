package org.opentripplanner.ext.carpooling.validation;

import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Validates whether an insertion of pickup/dropoff points into a route is valid.
 * <p>
 * Validators check specific constraints (capacity, direction, etc.) and can
 * reject insertions that violate those constraints.
 */
@FunctionalInterface
public interface InsertionValidator {
  /**
   * Validates an insertion.
   *
   * @param context The validation context containing all necessary information
   * @return Validation result indicating success or failure with reason
   */
  ValidationResult validate(ValidationContext context);

  /**
   * Context object containing all information needed for validation.
   */
  record ValidationContext(
    int pickupPosition,
    int dropoffPosition,
    WgsCoordinate pickup,
    WgsCoordinate dropoff,
    java.util.List<WgsCoordinate> routePoints,
    org.opentripplanner.ext.carpooling.util.PassengerCountTimeline passengerTimeline
  ) {}

  /**
   * Result of a validation check.
   */
  sealed interface ValidationResult {
    boolean isValid();

    String reason();

    record Valid() implements ValidationResult {
      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public String reason() {
        return "Valid";
      }
    }

    record Invalid(String reason) implements ValidationResult {
      @Override
      public boolean isValid() {
        return false;
      }
    }

    static ValidationResult valid() {
      return new Valid();
    }

    static ValidationResult invalid(String reason) {
      return new Invalid(reason);
    }
  }

  /**
   * Returns a validator that always accepts.
   */
  static InsertionValidator acceptAll() {
    return ctx -> ValidationResult.valid();
  }

  /**
   * Combines this validator with another using AND logic.
   */
  default InsertionValidator and(InsertionValidator other) {
    return ctx -> {
      ValidationResult first = this.validate(ctx);
      if (!first.isValid()) {
        return first;
      }
      return other.validate(ctx);
    };
  }
}
