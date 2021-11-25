package org.opentripplanner.model.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OtpNumberFormatTest {

    @Test
    void formatCost() {
        assertEquals("$-0.01", OtpNumberFormat.formatCost(-1));
        assertEquals("$0", OtpNumberFormat.formatCost(0));
        assertEquals("$0.01", OtpNumberFormat.formatCost(1));
        assertEquals("$22", OtpNumberFormat.formatCost(2200));
        assertEquals("$21.97", OtpNumberFormat.formatCost(2197));
        assertEquals("$9999.99", OtpNumberFormat.formatCost(999_999));
        assertEquals("$10000", OtpNumberFormat.formatCost(1_000_000));
    }
}