package org.opentripplanner.framework.token;

/**
 * List of types we can store in a token.
 * <p>
 * Enums are not safe, so do not add support for them. The reason is that new values can be added
 * to the enum and the previous version will fail to read the new version - it is no longer forward
 * compatible with the new value of the enum.
 */
public enum TokenType {
  BYTE,
  DURATION,
  INT,
  STRING,
  TIME_INSTANT;

  boolean isNot(TokenType other) {
    return this != other;
  }
}
