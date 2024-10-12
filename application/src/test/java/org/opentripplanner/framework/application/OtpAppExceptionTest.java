package org.opentripplanner.framework.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OtpAppExceptionTest {

  @Test
  public void testFormatArgsWorksProperly() {
    OtpAppException e = new OtpAppException("Hi, %s - %1.0f", 55L, 3.14d);
    assertEquals("Hi, 55 - 3", e.getMessage());
  }

  @Test
  public void testFormatNeverFails() {
    // The format string do not match the arguments
    OtpAppException e = new OtpAppException("Hi, %f - %d", 55L, "Alf");
    assertEquals("Hi, %f - %d [55, Alf]", e.getMessage());
  }
}
