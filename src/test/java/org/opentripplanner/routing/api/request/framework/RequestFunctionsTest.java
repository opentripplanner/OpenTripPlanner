package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class RequestFunctionsTest {

  @Test
  public void calculate() {
    DoubleAlgorithmFunction f = RequestFunctions.createLinearFunction(2.0, 3.0);

    assertEquals(f.calculate(0.0), 2.0, 1e-6);
    assertEquals(f.calculate(1.0), 5.0, 1e-6);
    assertEquals(f.calculate(2.0), 8.0, 1e-6);
  }

  @Test
  public void testToString() {
    assertEquals("f(x) = 2 + 3.0 x", RequestFunctions.createLinearFunction(2.0, 3.0).toString());
  }

  @Test
  public void parse() {
    assertEquals("f(x) = 2 + 3.0 x", RequestFunctions.parse("2+3x").toString());
    assertEquals("f(x) = 2 + 3.0 x", RequestFunctions.parse(" 2 + 3 X ").toString());
    assertEquals("f(x) = 5 + 3.14 x", RequestFunctions.parse("5.123 + 3.1415 x").toString());
  }

  @Test
  public void parseIllegalValue() {
    assertThrows(
      IllegalArgumentException.class,
      () -> RequestFunctions.parse("not-a-function"),
      "Unable to parse function: 'not-a-function'"
    );
  }

  @Test
  public void parseIllegalNumber() {
    // Must use '.' as decimal separator, not ','
    assertThrows(IllegalArgumentException.class, () -> RequestFunctions.parse("3,0 + 2.0 x"));
  }
}
