package org.opentripplanner.utils.tostring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultiLineToStringBuilderTest {

  @Test
  void testAdd() {
    assertEquals(
      """
      Test {
        foo... : bar
        number : 500
      }
      """.trim(),
      MultiLineToStringBuilder.of("Test")
        .add("foo", "bar")
        .add("number", 500)
        .add("null", null)
        .toString()
    );
  }

  @Test
  void testAddDuration() {
    assertEquals(
      """
      Test {
        foo : 5m20s
      }
      """.trim(),
      MultiLineToStringBuilder.of("Test").addDuration("foo", Duration.ofSeconds(320)).toString()
    );
  }

  @Test
  void testAddColNl() {
    assertEquals(
      """
      Test {
        foo : [
          A new line
          for each entry
          in list!
        ]
      }
      """.trim(),
      MultiLineToStringBuilder.of("Test")
        .addColNl("foo", List.of("A new line", "for each entry", "in list!"))
        // These are not added:
        .addColNl("null", null)
        .addColNl("empty", List.of())
        .toString()
    );
  }
}
