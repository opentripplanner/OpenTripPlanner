package org.opentripplanner.framework.token;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Given a schema definition and a token version this class holds the values for all fields in a
 * token. The field accessors return an optional value and there is no accessors to get "required"
 * fields. The responsibility of enforcing required fields is left to the concrete domain
 * specific representation, like the {@code PageCursor}.
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

  public Optional<Boolean> getBoolean(String fieldName) {
    var value = (Boolean) get(fieldName, TokenType.BOOLEAN);
    return Optional.ofNullable(value);
  }

  public Optional<Duration> getDuration(String fieldName) {
    return Optional.ofNullable((Duration) get(fieldName, TokenType.DURATION));
  }

  public OptionalInt getInt(String fieldName) {
    Integer v = (Integer) get(fieldName, TokenType.INT);
    return v == null ? OptionalInt.empty() : OptionalInt.of(v);
  }

  public Optional<String> getString(String fieldName) {
    return Optional.ofNullable((String) get(fieldName, TokenType.STRING));
  }

  /**
   * Be careful with enums. If values are added or deleted the backward/forward compatibility
   * is compromised. This method return an empty value if the enum does not exist.
   * <p>
   * To ensure that enum values are forward compatible the value must first be added, and then it
   * can not be used in a token before OTP is released and deployed. Then when the enum value
   * exist in the deployed server, then a new version of OTP can be rolled out which now can use
   * the new value.
   * <p>
   * To ensure backwards compatibility, enum values should be **deprecated**, not removed. The enum
   * value can only be deleted, when all tokens with the value has expired (depends on use-case).
   */
  public <T extends Enum<T>> Optional<T> getEnum(String fieldName, Class<T> enumClass) {
    try {
      String value = (String) get(fieldName, TokenType.ENUM);
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(Enum.valueOf(enumClass, value));
    } catch (IllegalArgumentException ignore) {
      return Optional.empty();
    }
  }

  public Optional<Instant> getTimeInstant(String fieldName) {
    return Optional.ofNullable((Instant) get(fieldName, TokenType.TIME_INSTANT));
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

  @Nullable
  private Object get(String fieldName, TokenType type) {
    return fieldValues.get(definition.getIndex(fieldName, type));
  }
}
