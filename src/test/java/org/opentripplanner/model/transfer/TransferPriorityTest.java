package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TransferPriorityTest {

    @Test
    public void cost() {
        assertEquals(0, TransferPriority.ALLOWED.cost(false, false));
        assertEquals(-100, TransferPriority.ALLOWED.cost(true, false));
        assertEquals(-10, TransferPriority.ALLOWED.cost(false, true));
        assertEquals(-110, TransferPriority.ALLOWED.cost(true, true));

        assertEquals(-2, TransferPriority.PREFERRED.cost(false, false));
        assertEquals(-112, TransferPriority.PREFERRED.cost(true, true));
        assertEquals(-1, TransferPriority.RECOMMENDED.cost(false, false));
        assertEquals(1_000, TransferPriority.NOT_ALLOWED.cost(false, false));
    }
}