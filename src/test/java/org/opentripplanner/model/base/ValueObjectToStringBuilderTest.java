package org.opentripplanner.model.base;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;

public class ValueObjectToStringBuilderTest {
    private enum  AEnum { A }
    private static class Foo {
        int a;
        String b;

        public Foo(int a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return ValueObjectToStringBuilder.of()
                    .addNum(a, 0)
                    .addStr(b)
                    .toString();
        }
    }

    private ValueObjectToStringBuilder subject() { return ValueObjectToStringBuilder.of(); }

    @Test
    public void addNum() {
        assertEquals("3.0", subject().addNum(3.0000000d).toString());
        assertEquals("3.0", subject().addNum(3.0000000f).toString());
        assertEquals("3", subject().addNum(3).toString());
        assertEquals("3", subject().addNum(3L).toString());
    }

    @Test
    public void testAddNumWithDefaults() {
        assertEquals(
                "3.0",
                subject()
                        .addNum(4d, 4d)
                        .addNum(3d, 2d)
                        .addNum(-1d, -1d)
                        .toString()
        );
    }

    @Test
    public void testAddNumWithUnit() {
        assertEquals(
                "3 minutes 7 seconds",
                subject()
                        .addNum(3, " minutes")
                        .addNum(7, " seconds")
                        .toString()
        );
    }

    @Test
    public void addBool() {
        assertEquals(
                "include nothing",
                subject()
                        .addBool(true, "include", "skip")
                        .addBool(false, "everything", "nothing")
                        .toString()
        );
    }

    @Test
    public void addStr() {
        assertEquals("'text'", subject().addStr("text").toString());
    }

    @Test
    public void addEnum() {
        assertEquals("A", subject().addEnum(AEnum.A).toString());
        assertEquals("<empty>", subject().addEnum(null).toString());
    }

    @Test
    public void addObj() {
        assertEquals(
                "5 'X'",
                subject().addObj(new Foo(5, "X")).toString()
        );
        assertEquals(
                "<empty>",
                subject().addObj(null).toString()
        );
    }

    @Test
    public void addCalTime() {
        Calendar c = GregorianCalendar.from(
                ZonedDateTime.of(
                        LocalDateTime.of(2012, 1, 28, 23,45, 12),
                        ZoneId.systemDefault()
                )
        );
        assertEquals(
                "23:45:12",
                subject().addCalTime(c).toString()
        );
    }

    @Test
    public void addCoordinate() {
        assertEquals(
                "(60.98766, 11.98)",
                subject().addCoordinate(60.9876599999999d, 11.98d).toString()
        );
    }

    @Test
    public void addDuration() {
        assertEquals(
                "35s",
                subject().addDuration(35).toString()
        );
        assertEquals(
                "26h50m45s",
                subject().addDuration((26 * 60 + 50) * 60 + 45).toString()
        );
    }
}