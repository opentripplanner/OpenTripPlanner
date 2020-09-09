package org.opentripplanner.routing.api.request;

import org.junit.Test;

import java.util.function.DoubleFunction;

import static org.junit.Assert.*;

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
    try {
      RequestFunctions.parse("not-a-function");
      fail();
    }
    catch (IllegalArgumentException e) {
      assertEquals("Unable to parse function: 'not-a-function'", e.getMessage());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseIllegalNumber() {
    // Must use '.' as decimal separator, not ','
    RequestFunctions.parse("3,0 + 2.0 x");
  }
}