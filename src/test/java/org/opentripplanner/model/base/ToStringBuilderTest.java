package org.opentripplanner.model.base;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.util.time.TimeUtils;

public class ToStringBuilderTest {

  @Test
  public void addFieldIfTrue() {
    assertEquals("ToStringBuilderTest{x}", subject().addFieldIfTrue("x", true).toString());
    assertEquals("ToStringBuilderTest{}", subject().addFieldIfTrue("x", false).toString());
  }

  @Test
  public void addNum() {
    assertEquals(
        "ToStringBuilderTest{num: 3.0}",
        subject().addNum("num", 3.0000000d).toString()
    );
    assertEquals(
        "ToStringBuilderTest{num: 30,000.0}",
        subject().addNum("num", 30_000.00f).toString()
    );
    assertEquals("ToStringBuilderTest{num: 3}", subject().addNum("num", 3).toString());
    assertEquals("ToStringBuilderTest{num: 3}", subject().addNum("num", 3L).toString());
  }

  @Test
  public void testAddNumWithDefaults() {
    assertEquals(
        "ToStringBuilderTest{b: 3.0}",
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
        "ToStringBuilderTest{a: 3,000s, b: 7m}",
        subject().addNum("a", 3000, "s").addNum("b", 7, "m").toString()
    );
  }

  @Test
  public void addBool() {
    assertEquals(
        "ToStringBuilderTest{a: true, b: false}",
        subject().addBool("a", true).addBool("b", false).toString()
    );
  }

  @Test
  public void addStr() {
    assertEquals("ToStringBuilderTest{a: 'text'}", subject().addStr("a", "text").toString());
  }

  @Test
  public void addEnum() {
    assertEquals("ToStringBuilderTest{a: A}", subject().addEnum("a", AEnum.A).toString());
    assertEquals("ToStringBuilderTest{}", subject().addEnum("b", null).toString());
  }

  @Test
  public void addTransitEntity() {
    Trip trip = new Trip(new FeedScopedId("F", "1"));
    assertEquals(
        "ToStringBuilderTest{tripId: F:1}",
        subject().addEntityId("tripId", trip).toString()
    );
  }

  @Test
  public void addObj() {
    assertEquals(
        "ToStringBuilderTest{obj: Foo{a: 5, b: 'X'}}",
        subject().addObj("obj", new Foo(5, "X")).toString()
    );
    assertEquals(
        "ToStringBuilderTest{obj: Foo{}}",
        subject().addObj("obj", new Foo(0, null)).toString()
    );
  }

  @Test
  public void addIntArray() {
    assertEquals(
        "ToStringBuilderTest{a: [1, 2, 3]}",
        subject().addInts("a", new int[] {1, 2, 3}).toString()
    );
  }

  @Test
  public void addCollection() {
    assertEquals(
        "ToStringBuilderTest{c: [1, 3.0, true]}",
        subject().addCol("c", List.of(1, 3d, true)).toString()
    );
  }

  @Test
  public void addCollectionWithLimit() {
    assertEquals(
        "ToStringBuilderTest{c: [1, 2, 3]}",
        subject().addCollection("c", List.of(1, 2, 3), 2).toString()
    );
    assertEquals(
        "ToStringBuilderTest{c(2/4): [1, 2, ..]}",
        subject().addCollection("c", List.of(1, 2, 3, 4), 2).toString()
    );
  }

  @Test
  public void addIntArraySize() {
    assertEquals(
        "ToStringBuilderTest{c: 2/3}",
        subject().addIntArraySize("c", new int[]{1, -1, 3}, -1).toString()
    );
  }

  @Test
  public void addBitSetSize() {
    var bset = new BitSet(8);
    bset.set(0, true);
    bset.set(3, true);
    bset.set(5, false);
    assertEquals(
        "ToStringBuilderTest{bitSet: 2/4}",
        subject().addBitSetSize("bitSet", bset).toString()
    );
  }

  @Test
  public void addCalTime() {
    Calendar c = GregorianCalendar.from(
        ZonedDateTime.of(
            LocalDateTime.of(2012, 1, 28, 23, 45, 12),
            ZoneId.systemDefault()
        )
    );
    assertEquals("ToStringBuilderTest{c: 23:45:12}", subject().addCalTime("c", c).toString());
  }

  @Test
  public void addServiceTime() {
    // 02:30:04 in seconds is:
    int seconds = TimeUtils.time("2:30:04");
    assertEquals(
        "ToStringBuilderTest{t: 2:30:04}",
        subject().addServiceTime("t", seconds, -1).toString()
    );
  }


  @Test
  public void addServiceTimeSchedule() {
    int[] times = TimeUtils.times("10:10 12:03");
    assertEquals(
        "ToStringBuilderTest{t: [10:10 12:03]}",
        subject().addServiceTimeSchedule("t", times).toString()
    );
  }

  @Test
  public void addCoordinate() {
    assertEquals(
        "ToStringBuilderTest{lat: 60.98766, lon: 11.98, r: 0.0}",
        subject()
            .addCoordinate("lat", 60.9876599999999d)
            .addCoordinate("lon", 11.98d)
            .addCoordinate("r", 0d)
            .toString()
    );
  }

  @Test
  public void addDuration() {
    assertEquals("ToStringBuilderTest{d: 35s}", subject().addDurationSec("d", 35).toString());
    assertEquals(
        "ToStringBuilderTest{d: 1d2h50m45s}",
        subject().addDurationSec("d", (26 * 60 + 50) * 60 + 45).toString()
    );
    assertEquals(
        "ToStringBuilderTest{d: 2m5s}",
        subject().addDuration("d", Duration.ofSeconds(125)).toString()
    );
    assertEquals(
        "ToStringBuilderTest{}",
        subject().addDurationSec("d", 12, 12).toString()
    );

  }

  private ToStringBuilder subject() {
    return ToStringBuilder.of(ToStringBuilderTest.class);
  }

  private enum AEnum { A }
  private static class Foo {
    int a;
    String b;

    public Foo(int a, String b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      Foo foo = (Foo) o;
      return a == foo.a && Objects.equals(b, foo.b);
    }
    @Override
    public String toString() {
      return ToStringBuilder.of(Foo.class).addNum("a", a, 0).addStr("b", b).toString();
    }
  }
}