package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RequestFunctionsTest {

  @Test
  public void calculate() {
    CostLinearFunction f = RequestFunctions.createLinearFunction(2.0, 3.0);

    assertEquals(f.calculate(0.0), 2.0, 1e-6);
    assertEquals(f.calculate(1.0), 5.0, 1e-6);
    assertEquals(f.calculate(2.0), 8.0, 1e-6);
  }

  @Test
  public void testToString() {
    assertEquals("2s + 3.0 t", RequestFunctions.createLinearFunction(2.0, 3.0).toString());
  }

  @Test
  public void parse() {
    assertEquals("2s + 3.0 t", CostLinearFunction.of("2+3x").toString());
    assertEquals("2s + 3.0 t", CostLinearFunction.of(" 2 + 2.95 X ").toString());
    assertEquals("5s + 3.1 t", CostLinearFunction.of("5.123 + 3.1415 x").toString());
  }

  @Test
  public void parseIllegalValue() {
    assertThrows(
      IllegalArgumentException.class,
      () -> CostLinearFunction.of("not-a-function"),
      "Unable to parse function: 'not-a-function'"
    );
  }

  @Test
  public void parseIllegalNumber() {
    // Must use '.' as decimal separator, not ','
    assertThrows(IllegalArgumentException.class, () -> CostLinearFunction.of("3,0 + 2.0 x"));
  }
}
