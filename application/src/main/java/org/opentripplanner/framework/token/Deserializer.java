package org.opentripplanner.framework.token;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class Deserializer {

  private static final Pattern SPLIT_PATTERN = Pattern.compile(
    "[" + TokenFormatterConfiguration.fieldSeparator() + "]"
  );

  private final List<String> values;

  Deserializer(String token) {
    byte[] bytes = Base64.getUrlDecoder().decode(token);
    var tokenFormatter = TokenFormatterConfiguration.tokenFormatter();
    this.values = Stream.of(SPLIT_PATTERN.split(new String(bytes), -1))
      .map(tokenFormatter::decode)
      .toList();
  }

  List<Object> deserialize(TokenDefinition definition) {
    try {
      // Assume deprecated fields are included in the token
      return readFields(definition, false);
    } catch (Exception ignore) {
      // If the token is the next version, then deprecated field are removed. Try
      // skipping the deprecated tokens
      return readFields(definition, true);
    }
  }

  private List<Object> readFields(TokenDefinition definition, boolean matchNewVersionPlusOne) {
    List<Object> result = new ArrayList<>();
    matchVersion(definition, matchNewVersionPlusOne);
    int index = 1;

    for (FieldDefinition field : definition.listFields()) {
      if (matchNewVersionPlusOne && field.deprecated()) {
        continue;
      }
      var v = read(field, index);
      if (!field.deprecated()) {
        result.add(v);
      }
      ++index;
    }
    return result;
  }

  private void matchVersion(TokenDefinition definition, boolean matchVersionPlusOne) {
    int matchVersion = (matchVersionPlusOne ? 1 : 0) + definition.version();

    int version = readVersion();
    if (version != matchVersion) {
      throw new IllegalStateException(
        "Version does not match. Token version: " +
        version +
        ", schema version: " +
        definition.version()
      );
    }
  }

  private Object read(FieldDefinition field, int index) {
    return field.type().stringToValue(values.get(index));
  }

  private int readVersion() {
    return Integer.parseInt(values.get(0));
  }
}
