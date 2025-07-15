package org.opentripplanner.framework.token;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.utils.lang.ObjectUtils;

/**
 * This class is used to create a {@link Token} before encoding it.
 */
public class TokenBuilder {

  private final TokenDefinition definition;
  private final Object[] values;

  public TokenBuilder(TokenDefinition definition) {
    this.definition = definition;
    this.values = new Object[definition.size()];
  }

  public TokenBuilder withBoolean(String fieldName, boolean v) {
    return with(fieldName, TokenType.BOOLEAN, v);
  }

  public TokenBuilder withEnum(String fieldName, Enum<?> v) {
    return with(fieldName, TokenType.ENUM, v);
  }

  public TokenBuilder withDuration(String fieldName, Duration v) {
    return with(fieldName, TokenType.DURATION, v);
  }

  public TokenBuilder withInt(String fieldName, int v) {
    return with(fieldName, TokenType.INT, v);
  }

  public TokenBuilder withString(String fieldName, String v) {
    return with(fieldName, TokenType.STRING, v);
  }

  public TokenBuilder withTimeInstant(String fieldName, Instant v) {
    return with(fieldName, TokenType.TIME_INSTANT, v);
  }

  public String build() {
    return Serializer.serialize(definition, values);
  }

  private TokenBuilder with(String fieldName, TokenType type, Object value) {
    int index = definition.getIndex(fieldName, type);
    values[index] = ObjectUtils.requireNotInitialized(fieldName, values[index], value);
    return this;
  }
}
