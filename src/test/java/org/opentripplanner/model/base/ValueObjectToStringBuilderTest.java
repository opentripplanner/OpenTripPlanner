package org.opentripplanner.model.base;

import org.junit.Test;

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
                    .addNum(a, " meters")
                    .addStr(b)
                    .toString();
        }
    }

    private ValueObjectToStringBuilder subject() { return ValueObjectToStringBuilder.of(); }

    @Test
    public void addNum() {
        assertEquals("30,000.0", subject().addNum(30_000d).toString());
        assertEquals("3.0", subject().addNum(3.0000f).toString());
        assertEquals("3,000", subject().addNum(3000).toString());
        assertEquals("3", subject().addNum(3L).toString());
    }

    @Test
    public void testAddNumWithUnit() {
        assertEquals(
                "3 minutes 7,000 seconds",
                subject()
                        .addNum(3, " minutes")
                        .addNum(7000, " seconds")
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
    public void addLbl() {
        assertEquals("abba", subject().addText("ab").addText("ba").toString());
        assertEquals("a_2_b", subject().addText("a_").addNum(2).addText("_b").toString());
    }

    @Test
    public void addEnum() {
        assertEquals("A", subject().addEnum(AEnum.A).toString());
        assertEquals("null", subject().addEnum(null).toString());
    }

    @Test
    public void addObj() {
        assertEquals(
                "5 meters 'X'",
                subject().addObj(new Foo(5, "X")).toString()
        );
        assertEquals(
                "null",
                subject().addObj(null).toString()
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
    public void addSecondsPastMidnight() {
        assertEquals(
                "00:00:35",
                subject().addServiceTime(35).toString()
        );
        assertEquals(
                "02:50:45+1d",
                subject().addServiceTime((26 * 60 + 50) * 60 + 45).toString()
        );
        assertEquals(
                "23:59:59-1d",
                subject().addServiceTime(-1).toString()
        );
    }

    @Test
    public void addDuration() {
        assertEquals(
                "35s",
                subject().addDuration(35).toString()
        );
        assertEquals(
                "1d2h50m45s",
                subject().addDuration((26 * 60 + 50) * 60 + 45).toString()
        );
    }
}