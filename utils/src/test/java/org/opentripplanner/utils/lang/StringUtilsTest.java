package org.opentripplanner.utils.lang;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class StringUtilsTest {

  static Stream<Arguments> hasValueTestCases() {
    return Stream.of(
      Arguments.of("Text", TRUE),
      Arguments.of("T", TRUE),
      Arguments.of(null, FALSE),
      Arguments.of("", FALSE),
      Arguments.of("\t\n", FALSE)
    );
  }

  @ParameterizedTest
  @MethodSource("hasValueTestCases")
  void hasValue(String input, Boolean hasValue) {
    assertEquals(hasValue, StringUtils.hasValue(input));
    assertEquals(!hasValue, StringUtils.hasNoValue(input));
    assertEquals(!hasValue, StringUtils.hasNoValueOrNullAsString(input));
  }

  @Test
  void hasNoValueOrNullAsString() {
    assertTrue(StringUtils.hasNoValueOrNullAsString("null"));
  }

  @Test
  public void assertValueExist() {
    // Ok if any value
    StringUtils.assertHasValue("a");

    // Should fail for these values
    var illegalValues = new String[] { null, "", " ", "\t", " \n\r\t\f" };

    for (var it : illegalValues) {
      assertThrows(IllegalArgumentException.class, () -> StringUtils.assertHasValue(it));

      Throwable thrown = assertThrows(IllegalArgumentException.class, () ->
        StringUtils.assertHasValue(it, "Illegal value %s", it)
      );
      assertTrue(thrown.getMessage().startsWith("Illegal value " + it));
    }
  }

  @Test
  void padLeft() {
    assertEquals("?????ABC", StringUtils.padLeft("ABC", '?', 8));
    assertEquals("ABC", StringUtils.padLeft("ABC", '?', 3));
    assertEquals("????????", StringUtils.padLeft(null, '?', 8));
  }

  @Test
  void padCenter() {
    assertEquals("??AB??", StringUtils.padBoth("AB", '?', 6));
    assertEquals("???AB??", StringUtils.padBoth("AB", '?', 7));
    assertEquals("??ABC?", StringUtils.padBoth("ABC", '?', 6));
    assertEquals("??ABC??", StringUtils.padBoth("ABC", '?', 7));
    assertEquals("ABC", StringUtils.padBoth("ABC", '?', 3));
    assertEquals("????????", StringUtils.padBoth(null, '?', 8));
  }

  @Test
  void padRight() {
    assertEquals("ABC???", StringUtils.padRight("ABC", '?', 6));
    assertEquals("??????", StringUtils.padRight(null, '?', 6));
    assertEquals("ABC", StringUtils.padRight("ABC", '?', 3));
  }

  @Test
  void quoteReplace() {
    assertEquals("\"key\" : \"value\"", StringUtils.quoteReplace("'key' : 'value'"));
  }

  @ParameterizedTest
  @ValueSource(
    strings = { "\u200B", "\n", "\t", "\thello", "f\noo", "\ntri\nmet:123\t", "tri\u200Bmet:123" }
  )
  void containsInvisibleChars(String input) {
    assertTrue(StringUtils.containsInvisibleCharacters(input));
  }

  @ParameterizedTest
  @ValueSource(strings = { "", " ", "hello", " hello", " fo o " })
  void noInvisibleChars(String input) {
    assertFalse(StringUtils.containsInvisibleCharacters(input));
  }

  @ParameterizedTest
  @ValueSource(strings = { "AAA Bbb", "aAa bbb", "aaa bbb", "aaa   bbb", "AAA_BBB" })
  void slugify(String input) {
    assertEquals("aaa-bbb", StringUtils.slugify(input));
  }
}
