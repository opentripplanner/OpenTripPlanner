package org.opentripplanner.ext.carpooling.validation;

import org.opentripplanner.ext.carpooling.util.DirectionalCalculator;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that inserting pickup/dropoff points maintains forward progress.
 * <p>
 * Prevents backtracking by checking that insertions don't cause the route
 * to deviate too far from its intended direction.
 */
public class DirectionalValidator implements InsertionValidator {

  private static final Logger LOG = LoggerFactory.getLogger(DirectionalValidator.class);

  /** Maximum bearing deviation allowed for forward progress (90° allows detours, prevents U-turns) */
  public static final double FORWARD_PROGRESS_TOLERANCE_DEGREES = 90.0;

  private final double toleranceDegrees;

  public DirectionalValidator() {
    this(FORWARD_PROGRESS_TOLERANCE_DEGREES);
  }

  public DirectionalValidator(double toleranceDegrees) {
    this.toleranceDegrees = toleranceDegrees;
  }

  @Override
  public ValidationResult validate(ValidationContext context) {
    // Validate pickup insertion
    if (context.pickupPosition() > 0 && context.pickupPosition() < context.routePoints().size()) {
      WgsCoordinate prevPoint = context.routePoints().get(context.pickupPosition() - 1);
      WgsCoordinate nextPoint = context.routePoints().get(context.pickupPosition());

      if (!maintainsForwardProgress(prevPoint, context.pickup(), nextPoint)) {
        String reason = String.format(
          "Pickup insertion at position %d causes backtracking",
          context.pickupPosition()
        );
        LOG.debug(reason);
        return ValidationResult.invalid(reason);
      }
    }

    // Validate dropoff insertion (in modified route with pickup already inserted)
    // Note: dropoffPosition is in the context of the original route
    // After pickup insertion, dropoff is one position later
    int dropoffPosInModified = context.dropoffPosition();
    if (dropoffPosInModified > 0 && dropoffPosInModified <= context.routePoints().size()) {
      // Get the previous point (which might be the pickup if dropoff is right after)
      WgsCoordinate prevPoint;
      if (dropoffPosInModified == context.pickupPosition()) {
        prevPoint = context.pickup(); // Previous point is the pickup
      } else if (dropoffPosInModified - 1 < context.routePoints().size()) {
        prevPoint = context.routePoints().get(dropoffPosInModified - 1);
      } else {
        // Edge case: dropoff at the end
        return ValidationResult.valid();
      }

      // Get next point if it exists
      if (dropoffPosInModified < context.routePoints().size()) {
        WgsCoordinate nextPoint = context.routePoints().get(dropoffPosInModified);

        if (!maintainsForwardProgress(prevPoint, context.dropoff(), nextPoint)) {
          String reason = String.format(
            "Dropoff insertion at position %d causes backtracking",
            context.dropoffPosition()
          );
          LOG.debug(reason);
          return ValidationResult.invalid(reason);
        }
      }
    }

    return ValidationResult.valid();
  }

  /**
   * Checks if inserting a new point maintains forward progress.
   */
  private boolean maintainsForwardProgress(
    WgsCoordinate previous,
    WgsCoordinate newPoint,
    WgsCoordinate next
  ) {
    // Calculate intended direction (previous → next)
    double intendedBearing = DirectionalCalculator.calculateBearing(previous, next);

    // Calculate detour directions
    double bearingToNew = DirectionalCalculator.calculateBearing(previous, newPoint);
    double bearingFromNew = DirectionalCalculator.calculateBearing(newPoint, next);

    // Check deviations
    double deviationToNew = DirectionalCalculator.bearingDifference(intendedBearing, bearingToNew);
    double deviationFromNew = DirectionalCalculator.bearingDifference(
      intendedBearing,
      bearingFromNew
    );

    // Allow some deviation but not complete reversal
    return (deviationToNew <= toleranceDegrees && deviationFromNew <= toleranceDegrees);
  }
}
