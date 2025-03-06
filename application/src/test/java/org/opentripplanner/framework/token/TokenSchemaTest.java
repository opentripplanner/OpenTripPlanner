package org.opentripplanner.framework.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TokenSchemaTest implements TestTokenSchemaConstants {

  // Token field names. These are used to reference a specific field value in the
  // token to avoid index errors. They are not part of the serialized token.

  private static final TokenSchema BOOLEAN_SCHEMA = TokenSchema.ofVersion(1)
    .addBoolean(BOOLEAN_TRUE_FIELD)
    .addBoolean(BOOLEAN_FALSE_FIELD)
    .build();

  private static final TokenSchema BYTE_SCHEMA = TokenSchema.ofVersion(1)
    .addByte(BYTE_FIELD)
    .build();
  private static final TokenSchema DURATION_SCHEMA = TokenSchema.ofVersion(2)
    .addDuration(DURATION_FIELD)
    .build();
  private static final TokenSchema ENUM_SCHEMA = TokenSchema.ofVersion(3)
    .addEnum(ENUM_FIELD)
    .build();
  private static final TokenSchema INT_SCHEMA = TokenSchema.ofVersion(3).addInt(INT_FIELD).build();
  private static final TokenSchema STRING_SCHEMA = TokenSchema.ofVersion(7)
    .addString(STRING_FIELD)
    .build();
  private static final TokenSchema TIME_INSTANT_SCHEMA = TokenSchema.ofVersion(13)
    .addTimeInstant(TIME_INSTANT_FIELD)
    .build();

  @Test
  public void encodeBoolean() {
    // Add in opposite order of Schema, test naming fields work
    var token = BOOLEAN_SCHEMA.encode()
      .withBoolean(BOOLEAN_FALSE_FIELD, false)
      .withBoolean(BOOLEAN_TRUE_FIELD, true)
      .build();
    assertEquals(BOOLEAN_ENCODED, token);
    assertTrue(BOOLEAN_SCHEMA.decode(token).getBoolean(BOOLEAN_TRUE_FIELD));

    var tokenResult = BOOLEAN_SCHEMA.decode(token);
    assertFalse(tokenResult.getBoolean(BOOLEAN_FALSE_FIELD));
  }

  @Test
  public void encodeByte() {
    var token = BYTE_SCHEMA.encode().withByte(BYTE_FIELD, (byte) 17).build();
    assertEquals(BYTE_ENCODED, token);
    assertEquals(BYTE_VALUE, BYTE_SCHEMA.decode(token).getByte(BYTE_FIELD));
  }

  @Test
  public void testDuration() {
    var token = DURATION_SCHEMA.encode().withDuration(DURATION_FIELD, DURATION_VALUE).build();
    assertEquals(DURATION_ENCODED, token);
    assertEquals(DURATION_VALUE, DURATION_SCHEMA.decode(token).getDuration(DURATION_FIELD));
  }

  @Test
  public void testEnum() {
    var token = ENUM_SCHEMA.encode().withEnum(ENUM_FIELD, ENUM_VALUE).build();
    assertEquals(ENUM_ENCODED, token);
    assertEquals(ENUM_VALUE, ENUM_SCHEMA.decode(token).getEnum(ENUM_FIELD, ENUM_CLASS).get());
    assertEquals(Optional.empty(), ENUM_SCHEMA.decode(token).getEnum(ENUM_FIELD, DayOfWeek.class));
  }

  @Test
  public void testInt() {
    var token = INT_SCHEMA.encode().withInt(INT_FIELD, INT_VALUE).build();
    assertEquals(INT_ENCODED, token);
    assertEquals(INT_VALUE, INT_SCHEMA.decode(token).getInt(INT_FIELD));
  }

  @Test
  public void testString() {
    var token = STRING_SCHEMA.encode().withString(STRING_FIELD, STRING_VALUE).build();
    assertEquals(STRING_ENCODED, token);
    assertEquals(STRING_VALUE, STRING_SCHEMA.decode(token).getString(STRING_FIELD));
  }

  @Test
  public void encodeTimeInstant() {
    var token = TIME_INSTANT_SCHEMA.encode()
      .withTimeInstant(TIME_INSTANT_FIELD, TIME_INSTANT_VALUE)
      .build();
    assertEquals(TIME_INSTANT_ENCODED, token);
    assertEquals(
      TIME_INSTANT_VALUE,
      TIME_INSTANT_SCHEMA.decode(token).getTimeInstant(TIME_INSTANT_FIELD)
    );
  }

  @Test
  public void encodeUndefinedFields() {
    var ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
      INT_SCHEMA.encode().withString("foo", "A")
    );
    assertEquals("Unknown field: 'foo'", ex.getMessage());

    Assertions.assertThrows(NullPointerException.class, () ->
      BYTE_SCHEMA.encode().withString(null, "A")
    );
  }

  @Test
  public void encodeFieldValueWithTypeMismatch() {
    var ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
      STRING_SCHEMA.encode().withByte(STRING_FIELD, (byte) 12)
    );
    assertEquals("The defined type for 'AStr' is STRING not BYTE.", ex.getMessage());
  }

  @Test
  public void decodeUndefinedToken() {
    var ex = Assertions.assertThrows(IllegalArgumentException.class, () -> INT_SCHEMA.decode("foo")
    );
    assertEquals("Token is not valid. Unable to parse token: 'foo'.", ex.getMessage());
  }

  @Test
  public void testToString() {
    assertEquals(
      "TokenSchema{definition: TokenDefinition{version: 1, fields: [AByte:BYTE]}}",
      BYTE_SCHEMA.toString()
    );
    assertEquals(
      "TokenSchema{definition: TokenDefinition{version: 3, fields: [ANum:INT]}}",
      INT_SCHEMA.toString()
    );
    assertEquals(
      "TokenSchema{definition: TokenDefinition{version: 7, fields: [AStr:STRING]}}",
      STRING_SCHEMA.toString()
    );
    assertEquals(
      "TokenSchema{definition: TokenDefinition{version: 2, fields: [ADur:DURATION]}}",
      DURATION_SCHEMA.toString()
    );
    assertEquals(
      "TokenSchema{definition: TokenDefinition{version: 13, fields: [ATime:TIME_INSTANT]}}",
      TIME_INSTANT_SCHEMA.toString()
    );
  }

  @Test
  void testDefinitionEqualsAndHashCode() {
    var subject = BYTE_SCHEMA.currentDefinition();
    var same = TokenSchema.ofVersion(1).addByte(BYTE_FIELD).build().currentDefinition();

    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());

    assertNotEquals(subject, INT_SCHEMA.currentDefinition());
    assertNotEquals(subject.hashCode(), INT_SCHEMA.currentDefinition().hashCode());
  }
}
