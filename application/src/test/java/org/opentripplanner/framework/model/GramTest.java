package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GramTest {

  private static final double VALUE = 7.5;
  private static final int VALUE_ONE = 1;

  private Gram one = Gram.of(VALUE_ONE);
  private Gram subject = Gram.of(VALUE);

  @Test
  void ofNullable() {
    assertEquals(Gram.ZERO, Gram.ofNullable(null));
    assertEquals(subject, Gram.ofNullable(VALUE));
  }

  @Test
  void plus() {
    assertEquals(Gram.of(VALUE), subject.plus(Gram.ZERO));
    assertEquals(Gram.of(VALUE + VALUE_ONE), subject.plus(one));
  }

  @Test
  void multiply() {
    assertEquals(Gram.of(VALUE * 2), subject.multiply(2.0));
    assertEquals(Gram.of(VALUE * 2), subject.multiply(2));
  }

  @Test
  void dividedBy() {
    assertEquals(Gram.of(VALUE / 2), subject.dividedBy(2));
    assertEquals(Gram.of(VALUE / 2), subject.dividedBy(2.0));
  }

  @Test
  void testEqualsAndHashCode() {
    var same = Gram.of(VALUE);

    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());
  }

  @Test
  void compareTo() {
    var same = Gram.of(VALUE);
    // Within precission (100)
    var less = Gram.of(VALUE - 0.001);

    assertTrue(subject.compareTo(same) == 0);
    assertTrue(less.compareTo(subject) < 0);
    assertTrue(subject.compareTo(less) > 0);

    // The delta is to small (precission is 100)
    var sameValue = Gram.of(VALUE - 0.00049);
    assertEquals(subject, sameValue);
    assertTrue(sameValue.compareTo(subject) == 0);
  }

  static List<Arguments> toStringTestCases() {
    return List.of(
      Arguments.of("1mg", 0.001),
      Arguments.of("-1mg", -0.001),
      Arguments.of("999mg", 0.999),
      Arguments.of("-999mg", -0.999),
      Arguments.of("1g", 1.0),
      Arguments.of("-1g", -1.0),
      Arguments.of("1.001g", 1.001),
      Arguments.of("-1.001g", -1.001),
      Arguments.of("19g", 19),
      Arguments.of("999g", 999),
      Arguments.of("-999g", -999),
      Arguments.of("1kg", 1000),
      Arguments.of("-1kg", -1000),
      Arguments.of("1001g", 1001),
      Arguments.of("-7g", -7)
    );
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("toStringTestCases")
  void testToString(String expected, Object input) {
    if (input instanceof Integer iValue) {
      assertEquals(expected, Gram.of(iValue.intValue()).toString());
    } else if (input instanceof Double dValue) {
      assertEquals(expected, Gram.of(dValue.doubleValue()).toString());
    } else {
      fail("Input should be an integer or double: " + input.getClass());
    }
  }

  @Test
  void asDouble() {
    assertEquals(VALUE, subject.asDouble());
  }

  @Test
  void isZero() {
    assertTrue(Gram.ZERO.isZero());
    assertTrue(Gram.of(0).isZero());
    assertFalse(subject.isZero());
  }

  static List<Arguments> ofStringTestCases() {
    return List.of(
      Arguments.of(0.0, "0"),
      Arguments.of(200.0, "200"),
      Arguments.of(20.0, "20.0"),
      Arguments.of(-7.5, "-7.5"),
      Arguments.of(0.0, "0g"),
      Arguments.of(1.0, "1g"),
      Arguments.of(200.0, "200 g"),
      Arguments.of(-7.5, "-7.5 g"),
      Arguments.of(0.0, "0mg"),
      Arguments.of(0.001, "1mg"),
      Arguments.of(0.2, "200 mg"),
      Arguments.of(-0.007, "-7 mg"),
      Arguments.of(0.0, "0kg"),
      Arguments.of(1000.0, "1kg"),
      Arguments.of(2200.0, "2.2 kg"),
      Arguments.of(-7500.0, "-7.5 kg")
    );
  }

  @ParameterizedTest(name = "{1}  >  {0}")
  @MethodSource("ofStringTestCases")
  void ofString(Double expected, String input) {
    assertEquals(expected, Gram.of(input).asDouble());
  }

  @Test
  void ofStringIllegalValues() {
    var ex = assertThrows(IllegalArgumentException.class, () -> Gram.of("200.0 tonn"));
    assertEquals("Parse error! Illegal gram value: '200.0 tonn'", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> Gram.of("0x001 g"));
    assertEquals("Parse error! Illegal gram value: '0x001 g'", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> Gram.of("1 gk"));
    assertEquals("Parse error! Illegal gram value: '1 gk'", ex.getMessage());
  }
}
