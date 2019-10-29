package org.opentripplanner.transit.raptor.transitadapter;

import org.junit.Test;
import org.opentripplanner.transit.raptor.api.transit.TransferLeg;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LightweightTransferIteratorTest {

    private static final int STOP_A = 5001;
    private static final int STOP_B = 234;
    private static final int DURATION_A = 5002;
    private static final int DURATION_B = 25;

    @Test
    public void an_empthy_iterator_have_no_more_elements() {
        LightweightTransferIterator subject = new LightweightTransferIterator(new int[0]);
        assertFalse(subject.hasNext());
    }

    @Test
    public void an_iterator_should_return_the_elements_in_order() {
        Iterator<TransferLeg> subject = new LightweightTransferIterator(new int[]{STOP_A, DURATION_A, STOP_B, DURATION_B});
        TransferLeg d;

        // Check first element (stop and duration)
        assertTrue(subject.hasNext());
        d = subject.next();
        assertEquals(STOP_A, d.stop());
        assertEquals(DURATION_A, d.durationInSeconds());

        // Check last element (stop and duration)
        assertTrue(subject.hasNext());
        d = subject.next();
        assertEquals(STOP_B, d.stop());
        assertEquals(DURATION_B, d.durationInSeconds());


        // No more elements expected
        assertFalse(subject.hasNext());

        // Clone and assert that we can start over
        subject = ((LightweightTransferIterator) subject).clone();

        assertTrue(subject.hasNext());
        d = subject.next();
        assertEquals(STOP_A, d.stop());
        assertEquals(DURATION_A, d.durationInSeconds());
    }
}