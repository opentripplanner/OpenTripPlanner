package org.opentripplanner.framework.token;

import java.util.Base64;
import org.opentripplanner.utils.text.CharacterEscapeFormatter;

class Serializer {

  private final TokenDefinition definition;
  private final Object[] values;
  private final StringBuilder buf = new StringBuilder();
  private final CharacterEscapeFormatter tokenFormatter =
    TokenFormatterConfiguration.tokenFormatter();

  private Serializer(TokenDefinition definition, Object[] values) {
    this.definition = definition;
    this.values = values;
  }

  static String serialize(TokenDefinition definition, Object[] values) {
    var s = new Serializer(definition, values);
    s.writeVersion(definition.version());
    for (var fieldName : definition.fieldNames()) {
      s.write(fieldName);
    }
    return s.serialize();
  }

  private String serialize() {
    return Base64.getUrlEncoder().encodeToString(buf.toString().getBytes());
  }

  private void write(String fieldName) {
    write(fieldName, values[definition.index(fieldName)]);
  }

  private void write(String fieldName, Object value) {
    var type = definition.type(fieldName);
    writeString(type.valueToString(value));
  }

  private void writeVersion(int value) {
    writeString(TokenType.INT.valueToString(value));
  }

  private void writeString(String value) {
    if (value != null) {
      buf.append(tokenFormatter.encode(value));
    }
    buf.append(TokenFormatterConfiguration.fieldSeparator());
  }
}
