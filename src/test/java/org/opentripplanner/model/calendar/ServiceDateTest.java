package org.opentripplanner.model.calendar;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class ServiceDateTest {
    private static final ServiceDate D_0 = new ServiceDate(2019, 7, 1);
    private static final ServiceDate D_1 = new ServiceDate(2019, 7, 2);
    private static final ServiceDate D_OTHER = new ServiceDate(2019, 7, 1);

    private static final ServiceDate M_0 = new ServiceDate(2020, 1, 20);
    private static final ServiceDate M_1 = new ServiceDate(2020, 2, 1);
    private static final ServiceDate M_OTHER = new ServiceDate(2020, 1, 20);

    private static final ServiceDate Y_0 = new ServiceDate(2019, 3, 20);
    private static final ServiceDate Y_1 = new ServiceDate(2020, 2, 7);
    private static final ServiceDate Y_OTHER = new ServiceDate(2019, 3, 20);

    @Test
    public void isBefore() {
        // Check day
        assertTrue(D_0.isBefore(D_1));
        assertFalse(D_1.isBefore(D_0));
        assertFalse(D_0.isBefore(D_OTHER));

        // Check month
        assertTrue(M_0.isBefore(M_1));
        assertFalse(M_1.isBefore(M_0));
        assertFalse(M_0.isBefore(M_OTHER));

        // Check year
        assertTrue(Y_0.isBefore(Y_1));
        assertFalse(Y_1.isBefore(Y_0));
        assertFalse(Y_0.isBefore(Y_OTHER));
    }

    @Test
    public void isBeforeOrEq() {
        assertTrue(D_0.isBeforeOrEq(D_1));
        assertFalse(D_1.isBeforeOrEq(D_0));
        assertTrue(D_0.isBeforeOrEq(D_OTHER));
    }

    @Test
    public void isAfter() {
        // Check day
        assertTrue(D_1.isAfter(D_0));
        assertFalse(D_0.isAfter(D_1));
        assertFalse(D_0.isAfter(D_OTHER));

        // Check month
        assertTrue(M_1.isAfter(M_0));
        assertFalse(M_0.isAfter(M_1));
        assertFalse(M_0.isAfter(M_OTHER));

        // Check year
        assertTrue(Y_1.isAfter(Y_0));
        assertFalse(Y_0.isAfter(Y_1));
        assertFalse(Y_0.isAfter(Y_OTHER));
    }

    @Test
    public void isAfterOrEq() {
        assertTrue(D_1.isAfterOrEq(D_0));
        assertFalse(D_0.isAfterOrEq(D_1));
        assertTrue(D_0.isAfterOrEq(D_OTHER));
    }

    @Test
    public void testLeagalRange() {
        new ServiceDate(0,1, 1);
        new ServiceDate(9999,12, 31);
    }

    @Test
    public void testEqualsAndHashCode() {
        ServiceDate d1 = new ServiceDate(2020, 3, 21);
        ServiceDate d2 = new ServiceDate(2020, 3, 21);

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    public void verifyThatAllHashCodesOverAYearIsUniqAndIncreasing() {
        // Loop over one year + an extra day
        LocalDate start = LocalDate.of(2020, 2, 1);
        LocalDate end = LocalDate.of(2021, 2, 2);
        LocalDate i = start;
        int lastHash = 0;

        while (i.isBefore(end)) {
            ServiceDate day = new ServiceDate(i);
            assertTrue(
                    "Hash is increasing for " + day + " : " + day.hashCode()
                            + ", last: " + lastHash,
                    day.hashCode() > lastHash
            );
            lastHash = day.hashCode();
            i = i.plusDays(1);
        }
    }

    @Test
    public void asISO8601() {
        assertNull("MAX", ServiceDate.MAX_DATE.asISO8601());
        assertNull("MIN", ServiceDate.MIN_DATE.asISO8601());
        assertEquals("2020-03-12", new ServiceDate(2020, 3, 12).asISO8601());
    }
}