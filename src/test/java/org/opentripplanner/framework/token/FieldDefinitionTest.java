package org.opentripplanner.framework.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldDefinitionTest {

  private static final String NAME = "foo";
  public static final TokenType TOKEN_TYPE = TokenType.STRING;
  private final FieldDefinition subject = new FieldDefinition(NAME, TOKEN_TYPE);

  @Test
  void name() {
    assertEquals(NAME, subject.name());
  }

  @Test
  void type() {
    assertEquals(TOKEN_TYPE, subject.type());
  }

  @Test
  void testDeprecate() {
    assertFalse(subject.deprecated());
    assertTrue(subject.deprecate().deprecated());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = new FieldDefinition(NAME, TOKEN_TYPE);
    var other1 = new FieldDefinition(NAME, TokenType.INT);
    var other2 = new FieldDefinition("Bar", TOKEN_TYPE);
    var other3 = subject.deprecate();

    assertEquals(subject, subject);
    assertEquals(same, subject);
    assertNotEquals(other1, subject);
    assertNotEquals(other2, subject);
    assertNotEquals(other3, subject);

    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other1.hashCode(), subject.hashCode());
    assertNotEquals(other2.hashCode(), subject.hashCode());
    assertNotEquals(other3.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("foo:STRING", subject.toString());
  }
}
