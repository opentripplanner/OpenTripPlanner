package org.opentripplanner.framework.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class CharacterEscapeFormatterTest {

  @Test
  public void encodeDecode() {
    var subject = new CharacterEscapeFormatter('\\', ';', '|');

    var original = "This;is;a:text: ;\\;, \\; ;\\ |\\|, \\| |\\";
    var escapedText = subject.encode(original);
    var result = subject.decode(escapedText);

    assertEquals(original, result);
    assertFalse(escapedText.contains(";"), escapedText);
  }
}
