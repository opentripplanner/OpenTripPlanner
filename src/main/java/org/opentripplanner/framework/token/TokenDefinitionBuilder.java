package org.opentripplanner.framework.token;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.lang.IntUtils;

public class TokenDefinitionBuilder {

  private final int version;
  private final List<TokenDefinition> tokensHead = new ArrayList<>();
  private final List<FieldDefinition> fields = new ArrayList<>();

  TokenDefinitionBuilder(int version) {
    this.version = IntUtils.requireInRange(version, 1, 1_000_000);
  }

  TokenDefinitionBuilder(List<TokenDefinition> head, TokenDefinition last) {
    this(last.version() + 1);
    this.fields.addAll(last.listNonDeprecatedFields());
    this.tokensHead.addAll(head);
    this.tokensHead.add(last);
  }

  public TokenDefinitionBuilder addByte(String fieldName) {
    return add(fieldName, TokenType.BYTE);
  }

  public TokenDefinitionBuilder addBoolean(String fieldName) {
    return add(fieldName, TokenType.BOOLEAN);
  }

  public TokenDefinitionBuilder addDuration(String fieldName) {
    return add(fieldName, TokenType.DURATION);
  }

  public TokenDefinitionBuilder addEnum(String fieldName) {
    return add(fieldName, TokenType.ENUM);
  }

  public TokenDefinitionBuilder addInt(String fieldName) {
    return add(fieldName, TokenType.INT);
  }

  public TokenDefinitionBuilder addString(String fieldName) {
    return add(fieldName, TokenType.STRING);
  }

  public TokenDefinitionBuilder addTimeInstant(String fieldName) {
    return add(fieldName, TokenType.TIME_INSTANT);
  }

  /**
   * A deprecated field will be removed from the *next* token. A value must be provided for the
   * deprecated field when encoding the current version. But, you can not read it! This make sure
   * that the previous version sees the deprecated value, while this version will still work with
   * a token provided with the next version. The deprecated field is automatically removed from
   * the next version.
   */
  public TokenDefinitionBuilder deprecate(String fieldName) {
    int index = indexOfField(fieldName);
    if (index < 0) {
      throw new IllegalArgumentException(
        "The field '" + fieldName + "' does not exist! Deprecation failed."
      );
    }
    fields.set(index, fields.get(index).deprecate());
    return this;
  }

  private int indexOfField(String fieldName) {
    for (int i = 0; i < fields.size(); i++) {
      if (fields.get(i).name().equals(fieldName)) {
        return i;
      }
    }
    return -1;
  }

  public TokenDefinitionBuilder newVersion() {
    return new TokenDefinitionBuilder(tokensHead, buildIt());
  }

  public TokenSchema build() {
    return new TokenSchema(ListUtils.combine(tokensHead, List.of(buildIt())));
  }

  private TokenDefinition buildIt() {
    return new TokenDefinition(version, fields);
  }

  private TokenDefinitionBuilder add(String fieldName, TokenType type) {
    fields.add(new FieldDefinition(fieldName, type));
    return this;
  }
}
