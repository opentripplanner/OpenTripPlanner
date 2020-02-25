package org.opentripplanner.model.base;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ToStringBuilderTest {
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
            return new ToStringBuilder(Foo.class)
                    .addNum("a", a, 0)
                    .addStr("b", b)
                    .toString();
        }
    }

    private ToStringBuilder subject() { return new ToStringBuilder("Test"); }

    @Test
    public void addNum() {
        assertEquals("Test{num:3.0}", subject().addNum("num", 3.0000000d).toString());
        assertEquals("Test{num:3.0}", subject().addNum("num", 3.0000000f).toString());
        assertEquals("Test{num:3}", subject().addNum("num", 3).toString());
        assertEquals("Test{num:3}", subject().addNum("num", 3L).toString());
    }

    @Test
    public void testAddNumWithDefaults() {
        assertEquals(
                "Test{b:3.0, NOT_SET:[a, c]}",
                subject()
                        .addNum("a", 3d, 3d)
                        .addNum("b", 3d, 2d)
                        .addNum("c", -1d, -1d)
                        .toString()
        );
    }

    @Test
    public void testAddNumWithUnit() {
        assertEquals(
                "Test{a:3s, b:7m}",
                subject()
                        .addNum("a", 3, "s")
                        .addNum("b", 7, "m")
                        .toString()
        );
    }

    @Test
    public void addBool() {
        assertEquals(
                "Test{a:true, b:false}",
                subject()
                        .addBool("a", true)
                        .addBool("b", false)
                        .toString()
        );
    }

    @Test
    public void addStr() {
        assertEquals("Test{a:'text'}", subject().addStr("a", "text").toString());
    }

    @Test
    public void addEnum() {
        assertEquals("Test{a:A}", subject().addEnum("a", AEnum.A).toString());
        assertEquals("Test{NOT_SET:[b]}", subject().addEnum("b", null).toString());
    }

    @Test
    public void addObj() {
        assertEquals(
                "Test{obj:Foo{a:5, b:'X'}}",
                subject().addObj("obj", new Foo(5, "X")).toString()
        );
        assertEquals(
                "Test{obj:Foo{NOT_SET:[a, b]}}",
                subject().addObj("obj", new Foo(0, null)).toString()
        );
    }

    @Test
    public void addCollection() {
        assertEquals(
                "Test{c:[1, 3.0, true]}",
                subject().addCol("c", List.of(1, 3d, true)).toString()
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
                "Test{c:23:45:12}",
                subject().addCalTime("c", c).toString()
        );
    }

    @Test
    public void addCoordinate() {
        assertEquals(
                "Test{lat:60.98766, lon:11.98, r:0.0}",
                subject()
                        .addCoordinate("lat", 60.9876599999999d)
                        .addCoordinate("lon", 11.98d)
                        .addCoordinate("r", 0d)
                        .toString()
        );
    }

    @Test
    public void addDuration() {
        assertEquals(
                "Test{d:35s}",
                subject().addDuration("d", 35).toString()
        );
        assertEquals(
                "Test{d:26h50m45s}",
                subject().addDuration("d", (26 * 60 + 50) * 60 + 45).toString()
        );
    }
}