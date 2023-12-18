package org.opentripplanner.framework.token;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.DurationUtils;

/**
 * List of types we can store in a token.
 * <p>
 * Enums are not safe, so do not add support for them. The reason is that new values can be added
 * to the enum and the previous version will fail to read the new version - it is no longer forward
 * compatible with the new value of the enum.
 */
public enum TokenType {
  BOOLEAN,
  BYTE,
  DURATION,
  ENUM,
  INT,
  STRING,
  TIME_INSTANT;

  private static final String EMPTY = "";

  boolean isNot(TokenType other) {
    return this != other;
  }

  public String valueToString(@Nullable Object value) {
    if (value == null) {
      return EMPTY;
    }
    return switch (this) {
      case BOOLEAN -> Boolean.toString((boolean) value);
      case BYTE -> Byte.toString((byte) value);
      case DURATION -> DurationUtils.durationToStr((Duration) value);
      case ENUM -> ((Enum<?>) value).name();
      case INT -> Integer.toString((int) value);
      case STRING -> (String) value;
      case TIME_INSTANT -> value.toString();
    };
  }

  public Object stringToValue(String value) {
    if (EMPTY.equals(value)) {
      return null;
    }
    return switch (this) {
      case BOOLEAN -> Boolean.valueOf(value);
      case BYTE -> Byte.valueOf(value);
      case DURATION -> DurationUtils.duration(value);
      case ENUM -> value;
      case INT -> Integer.valueOf(value);
      case STRING -> value;
      case TIME_INSTANT -> Instant.parse(value);
    };
  }
}
