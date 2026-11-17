package org.opentripplanner.apis.gtfs.mapping;

import org.jspecify.annotations.Nullable;

/**
 * Maps a nullable {@link Nullable} to a nullable {@link Double}.
 */
public class NumberMapper {

  @Nullable
  public static Double toDouble(@Nullable Number input) {
    if (input != null) {
      return input.doubleValue();
    } else {
      return null;
    }
  }
}
