package org.opentripplanner.routing.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TraverseResultTest {

    @Test
    public void testAddToExistingResultChain() {

        TraverseResult resultChain = null;

        for (int i = 0; i < 4; i++) {
            TraverseResult r = new TraverseResult(i, null);
            resultChain = r.addToExistingResultChain(resultChain);
        }

        assertEquals(3.0, resultChain.weight, 0.0);

        resultChain = resultChain.getNextResult();
        assertEquals(2.0, resultChain.weight, 0.0);

        resultChain = resultChain.getNextResult();
        assertEquals(1.0, resultChain.weight, 0.0);

        resultChain = resultChain.getNextResult();
        assertEquals(0.0, resultChain.weight, 0.0);

        resultChain = resultChain.getNextResult();
        assertNull(resultChain);
    }
}
