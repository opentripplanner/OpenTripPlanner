package org.opentripplanner.framework.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CharacterEscapeFormatterTest {

  @ParameterizedTest
  @ValueSource(
    strings = {
      "This;is;a:text",
      " ; ",
      " ;",
      "; ",
      ";;;",
      "^",
      " ^ ",
      " ^",
      "^ ",
      ";^;",
      "^;",
      ";^",
      "%^%",
      "^%",
      "%^",
      ";^;",
      "^;",
      ";^",
    }
  )
  public void encodeDecode(String original) {
    var subject = new CharacterEscapeFormatter('^', ';', '%');

    var escapedText = subject.encode(original);
    var result = subject.decode(escapedText);

    assertNotEquals(escapedText, original);

    assertEquals(original, result);
    assertFalse(escapedText.contains(";"), escapedText);
  }
}
