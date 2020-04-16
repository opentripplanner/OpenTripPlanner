package org.opentripplanner.model.calendar;

import org.junit.Test;

import java.text.ParseException;
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
    public void parse() throws ParseException {
        ServiceDate subject;

        subject = ServiceDate.parseString("20201231");
        assertEquals(2020, subject.getYear());
        assertEquals(12, subject.getMonth());
        assertEquals(31, subject.getDay());

        subject = ServiceDate.parseString("2020-03-12");
        assertEquals(2020, subject.getYear());
        assertEquals(3, subject.getMonth());
        assertEquals(12, subject.getDay());

        try {
            // Even though this is a valid date, we only support parsing of dates with
            // 4 digits in the year
            ServiceDate.parseString("0-03-12");
            fail("Expected ParseException");
        }
        catch (ParseException e) {
            assertEquals("error parsing date: 0-03-12", e.getMessage());
        }
    }

    @Test
    public void minMax() throws ParseException {
        ServiceDate d1 = ServiceDate.parseString("2020-12-30");
        ServiceDate d2 = ServiceDate.parseString("2020-12-31");


        assertSame(d1, d1.min(d2));
        assertSame(d1, d2.min(d1));
        assertSame(d2, d1.max(d2));
        assertSame(d2, d2.max(d1));

        // Test isMinMax
        assertFalse(d1.isMinMax());
        assertTrue(ServiceDate.MIN_DATE.isMinMax());
        assertTrue(ServiceDate.MAX_DATE.isMinMax());
    }

    @Test
    public void shift() {
        ServiceDate subject = new ServiceDate(2020, 3, 12);
        assertEquals("2020-03-11", subject.previous().asISO8601());
        assertEquals("2020-03-13", subject.next().asISO8601());
        assertEquals("2020-02-29", subject.shift(-12).asISO8601());
        assertEquals("2020-04-01", subject.shift(20).asISO8601());
        assertEquals("2021-03-12", subject.shift(365).asISO8601());
    }

    @Test
    public void asCompactString() {
        assertEquals("99991231", ServiceDate.MAX_DATE.asCompactString());
        assertEquals("00000101", ServiceDate.MIN_DATE.asCompactString());
        assertEquals("20200312", new ServiceDate(2020, 3, 12).asCompactString());
    }

    @Test
    public void asISO8601() {
        assertEquals("9999-12-31", ServiceDate.MAX_DATE.asISO8601());
        assertEquals("0-01-01", ServiceDate.MIN_DATE.asISO8601());
        assertEquals("2020-03-12", new ServiceDate(2020, 3, 12).asISO8601());
    }

    @Test
    public void testToString() {
        assertEquals("MAX", ServiceDate.MAX_DATE.toString());
        assertEquals("MIN", ServiceDate.MIN_DATE.toString());
        assertEquals("2020-03-12", new ServiceDate(2020, 3, 12).asISO8601());
    }
}