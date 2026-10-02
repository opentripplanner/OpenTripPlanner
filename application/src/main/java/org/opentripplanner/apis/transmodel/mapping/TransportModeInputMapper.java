package org.opentripplanner.apis.transmodel.mapping;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Maps values of the GraphQL {@code TransportMode} enum, used as filter input, to
 * {@link TransitMode}.
 * <p>
 * The enum value {@code unknown} has no {@link TransitMode} counterpart and is mapped to a
 * String. No trip, line or stop has an unknown mode, so it cannot be used as a filter and is
 * rejected as invalid client input.
 */
public class TransportModeInputMapper {

  /**
   * Map a {@code TransportMode} input value. A {@code null} value is returned as is, so that the
   * caller decides how a missing mode is handled.
   *
   * @throws InvalidInputException if the value is not a {@link TransitMode}, e.g. {@code unknown}
   */
  @Nullable
  public static TransitMode mapTransitMode(@Nullable Object value) {
    if (value == null || value instanceof TransitMode) {
      return (TransitMode) value;
    }
    throw new InvalidInputException(
      "transportMode '%s' cannot be used as a filter.".formatted(value)
    );
  }

  /**
   * Map a list of {@code TransportMode} input values. A {@code null} list is returned as is.
   *
   * @throws InvalidInputException if a value is not a {@link TransitMode}, e.g. {@code unknown}
   */
  @Nullable
  public static List<TransitMode> mapTransitModes(@Nullable List<?> values) {
    if (values == null) {
      return null;
    }
    return values.stream().map(TransportModeInputMapper::mapTransitMode).toList();
  }
}
