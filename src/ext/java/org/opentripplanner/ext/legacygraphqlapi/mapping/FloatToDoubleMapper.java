package org.opentripplanner.ext.legacygraphqlapi.mapping;

import javax.annotation.Nullable;

/**
 * Maps a nullable {@link Float} to a nullable {@link Double}.
 */
public class FloatToDoubleMapper {

  @Nullable
  public static Double toDouble(@Nullable Number input) {
    if (input != null) {
      return input.doubleValue();
    } else {
      return null;
    }
  }
}
