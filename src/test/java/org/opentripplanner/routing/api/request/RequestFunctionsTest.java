package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.DoubleFunction;
import org.junit.jupiter.api.Test;

public class RequestFunctionsTest {

  @Test
  public void calculate() {
    DoubleFunction<Double> f = RequestFunctions.createLinearFunction(2.0, 3.0);

    assertEquals(f.apply(0.0), 2.0, 1e-6);
    assertEquals(f.apply(1.0), 5.0, 1e-6);
    assertEquals(f.apply(2.0), 8.0, 1e-6);
  }

  @Test
  public void testToString() {
    assertEquals("f(x) = 2.0 + 3.0 x", RequestFunctions.createLinearFunction(2.0, 3.0).toString());
  }

  @Test
  public void parse() {
    assertEquals("f(x) = 2.0 + 3.0 x", RequestFunctions.parse("2+3x").toString());
    assertEquals("f(x) = 2.0 + 3.0 x", RequestFunctions.parse(" 2 + 3 X ").toString());
    assertEquals("f(x) = 5.1 + 3.1415 x", RequestFunctions.parse("5.1 + 3.1415 x").toString());
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
