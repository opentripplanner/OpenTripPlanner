package org.opentripplanner.transit.model.basic;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.utils.lang.DoubleUtils;

/**
 * Represents a ratio within the range [0, 1].
 * The class ensures that the ratio value, represented as a double,
 * falls withing the specified range.
 */
public class Ratio {

  private final double ratio;

  private Ratio(double ratio) {
    this.ratio = DoubleUtils.roundTo3Decimals(ratio);
  }

  /**
   * This method is similar to {@link #of(double, Consumer)}, but throws an
   * {@link IllegalArgumentException} if the ratio is not valid.
   */
  public static Ratio of(double ratio) {
    return of(ratio, errMsg -> {
      throw new IllegalArgumentException(errMsg);
    }).orElseThrow();
  }

  public static Optional<Ratio> of(double ratio, Consumer<String> validationErrorHandler) {
    if (ratio >= 0d && ratio <= 1d) {
      return Optional.of(new Ratio(ratio));
    } else {
      validationErrorHandler.accept("Ratio must be in range [0,1], but was: " + ratio);
      return Optional.empty();
    }
  }

  public static Optional<Ratio> ofBoxed(
    @Nullable Double ratio,
    Consumer<String> validationErrorHandler
  ) {
    if (ratio == null) {
      return Optional.empty();
    }
    return of(ratio, validationErrorHandler);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var other = (Ratio) o;
    return Double.compare(ratio, other.ratio) == 0;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(ratio);
  }

  @Override
  public String toString() {
    return Double.toString(ratio);
  }

  public double asDouble() {
    return ratio;
  }
}
