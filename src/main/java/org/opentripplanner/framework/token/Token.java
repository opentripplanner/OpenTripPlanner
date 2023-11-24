package org.opentripplanner.framework.token;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Given a schema definition and a token version this class holds the values for
 * all fields in a token.
 */
public class Token {

  private final TokenDefinition definition;
  private final List<Object> fieldValues;

  Token(TokenDefinition definition, List<Object> fieldValues) {
    this.definition = Objects.requireNonNull(definition);
    this.fieldValues = Objects.requireNonNull(fieldValues);
  }

  public int version() {
    return definition.version();
  }

  public byte getByte(String fieldName) {
    return (byte) get(fieldName, TokenType.BYTE);
  }

  public Duration getDuration(String fieldName) {
    return (Duration) get(fieldName, TokenType.DURATION);
  }

  public int getInt(String fieldName) {
    return (int) get(fieldName, TokenType.INT);
  }

  public String getString(String fieldName) {
    return (String) get(fieldName, TokenType.STRING);
  }

  public Instant getTimeInstant(String fieldName) {
    return (Instant) get(fieldName, TokenType.TIME_INSTANT);
  }

  private Object get(String fieldName, TokenType type) {
    return fieldValues.get(definition.getIndex(fieldName, type));
  }

  @Override
  public String toString() {
    return (
      "(v" +
      version() +
      ", " +
      fieldValues.stream().map(Objects::toString).collect(Collectors.joining(", ")) +
      ')'
    );
  }
}
