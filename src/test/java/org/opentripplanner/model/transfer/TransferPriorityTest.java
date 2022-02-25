package org.opentripplanner.model.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.model.transfer.TransferPriority.*;


public class TransferPriorityTest {

    @Test
    public void cost() {
        assertEquals(1_00, PREFERRED.cost());
        assertEquals(2_00, RECOMMENDED.cost());
        assertEquals(3_00, ALLOWED.cost());
        assertEquals(1000_00, NOT_ALLOWED.cost());
    }

    @Test
    public void isConstrained() {
        assertTrue(PREFERRED.isConstrained());
        assertTrue(RECOMMENDED.isConstrained());
        assertFalse(ALLOWED.isConstrained());
        assertTrue(NOT_ALLOWED.isConstrained());
    }
}