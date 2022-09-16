package org.opentripplanner.util.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OtpNumberFormatTest {

  @Test
  void formatCost() {
    assertEquals("$-1", OtpNumberFormat.formatCost(-1));
    assertEquals("$0", OtpNumberFormat.formatCost(0));
    assertEquals("$1", OtpNumberFormat.formatCost(1));
    assertEquals("$2200", OtpNumberFormat.formatCost(2200));
  }

  @Test
  void formatCostCenti() {
    assertEquals("$-0.01", OtpNumberFormat.formatCostCenti(-1));
    assertEquals("$0", OtpNumberFormat.formatCostCenti(0));
    assertEquals("$0.01", OtpNumberFormat.formatCostCenti(1));
    assertEquals("$22", OtpNumberFormat.formatCostCenti(2200));
    assertEquals("$21.97", OtpNumberFormat.formatCostCenti(2197));
    assertEquals("$9999.99", OtpNumberFormat.formatCostCenti(999_999));
    assertEquals("$10000", OtpNumberFormat.formatCostCenti(1_000_000));
  }
}
