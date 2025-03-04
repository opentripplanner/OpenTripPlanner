package org.opentripplanner.framework.token;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A token definition is an ordered list of fields. A field has a name and a type. The
 * definition is used to encode/decode a token.
 */
public class TokenDefinition {

  private final int version;
  private final List<String> fieldNames;
  private final Map<String, FieldDefinition> fields;

  TokenDefinition(int version, List<FieldDefinition> fields) {
    this.version = version;
    this.fieldNames = fields.stream().map(FieldDefinition::name).toList();
    this.fields = immutableMapOf(fields);
  }

  public int version() {
    return version;
  }

  public List<String> fieldNames() {
    return fieldNames;
  }

  public TokenType type(String fieldName) {
    return fields.get(fieldName).type();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TokenDefinition that = (TokenDefinition) o;
    return version == that.version && listFields().equals(that.listFields());
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, listFields());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TokenDefinition.class)
      .addNum("version", version, 0)
      .addCol("fields", listFields())
      .toString();
  }

  int size() {
    return fieldNames.size();
  }

  int getIndex(String name, TokenType assertType) {
    assertType(name, assertType);
    return index(name);
  }

  int index(String name) {
    for (int i = 0; i < fieldNames.size(); ++i) {
      if (fieldNames.get(i).equals(name)) {
        return i;
      }
    }
    throw unknownFieldNameException(name);
  }

  List<FieldDefinition> listNonDeprecatedFields() {
    return listFields().stream().filter(it -> !it.deprecated()).toList();
  }

  List<FieldDefinition> listFields() {
    return fieldNames.stream().map(fields::get).toList();
  }

  private void assertType(String name, TokenType assertType) {
    Objects.requireNonNull(name);
    var field = fields.get(name);

    if (field == null) {
      throw unknownFieldNameException(name);
    }

    if (field.type().isNot(assertType)) {
      throw new IllegalArgumentException(
        "The defined type for '" + name + "' is " + field.type() + " not " + assertType + "."
      );
    }
  }

  private IllegalArgumentException unknownFieldNameException(String name) {
    return new IllegalArgumentException("Unknown field: '" + name + "'");
  }

  private static Map<String, FieldDefinition> immutableMapOf(List<FieldDefinition> fields) {
    return Map.copyOf(fields.stream().collect(Collectors.toMap(FieldDefinition::name, it -> it)));
  }
}
