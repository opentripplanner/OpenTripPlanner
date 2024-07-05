package org.opentripplanner.framework.token;

import java.util.Objects;

class FieldDefinition {

  private final String name;
  private final TokenType type;
  private final boolean deprecated;

  private FieldDefinition(String name, TokenType type, boolean deprecated) {
    this.name = Objects.requireNonNull(name);
    this.type = Objects.requireNonNull(type);
    this.deprecated = deprecated;
  }

  public FieldDefinition(String name, TokenType type) {
    this(name, type, false);
  }

  public String name() {
    return name;
  }

  public TokenType type() {
    return type;
  }

  public boolean deprecated() {
    return deprecated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FieldDefinition that = (FieldDefinition) o;
    return deprecated == that.deprecated && Objects.equals(name, that.name) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, deprecated);
  }

  @Override
  public String toString() {
    return (deprecated ? "@deprecated " : "") + name + ":" + type;
  }

  public FieldDefinition deprecate() {
    return new FieldDefinition(name, type, true);
  }
}
