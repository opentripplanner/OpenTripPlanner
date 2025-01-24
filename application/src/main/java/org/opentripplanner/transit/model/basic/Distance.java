package org.opentripplanner.transit.model.basic;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public class Distance {

  private static final int MILLIMETERS_PER_M = 1000;
  private static final int MILLIMETERS_PER_KM = 1000 * MILLIMETERS_PER_M;
  private final int millimeters;

  /**
   * Represents a distance.
   * The class ensures that the distance, saved as an integer
   * representing the millimeters, is not negative.
   */
  private Distance(int distanceInMillimeters) {
    this.millimeters = distanceInMillimeters;
  }

  /**
   * This method is similar to {@link #of(double, Consumer)}, but throws an
   * {@link IllegalArgumentException} if the distance is negative.
   */
  private static Distance of(int distanceInMillimeters) {
    return of(
      distanceInMillimeters,
      errMsg -> {
        throw new IllegalArgumentException(errMsg);
      }
    )
      .orElseThrow();
  }

  private static Optional<Distance> of(
    int distanceInMillimeters,
    Consumer<String> validationErrorHandler
  ) {
    if (distanceInMillimeters >= 0) {
      return Optional.of(new Distance(distanceInMillimeters));
    } else {
      validationErrorHandler.accept(
        "Distance must be greater or equal than 0, but was: " + distanceInMillimeters
      );
      return Optional.empty();
    }
  }

  private static Optional<Distance> ofBoxed(
    @Nullable Double value,
    Consumer<String> validationErrorHandler,
    int multiplier
  ) {
    if (value == null) {
      return Optional.empty();
    }
    return of((int) (value * multiplier), validationErrorHandler);
  }

  /** Returns a Distance object representing the given number of meters */
  public static Optional<Distance> ofMetersBoxed(
    @Nullable Double value,
    Consumer<String> validationErrorHandler
  ) {
    return ofBoxed(value, validationErrorHandler, MILLIMETERS_PER_M);
  }

  /** Returns a Distance object representing the given number of kilometers */
  public static Optional<Distance> ofKilometersBoxed(
    @Nullable Double value,
    Consumer<String> validationErrorHandler
  ) {
    return ofBoxed(value, validationErrorHandler, MILLIMETERS_PER_KM);
  }

  /** Returns the distance in meters */
  public int toMeters() {
    double meters = (double) this.millimeters / (double) MILLIMETERS_PER_M;
    return (int) Math.round(meters);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var other = (Distance) o;
    return this.millimeters == other.millimeters;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(this.millimeters);
  }

  @Override
  public String toString() {
    if (millimeters > MILLIMETERS_PER_KM) {
      return ValueObjectToStringBuilder
        .of()
        .addNum((double) this.millimeters / (double) MILLIMETERS_PER_KM, "km")
        .toString();
    } else {
      return ValueObjectToStringBuilder
        .of()
        .addNum((double) this.millimeters / (double) MILLIMETERS_PER_M, "m")
        .toString();
    }
  }
}
