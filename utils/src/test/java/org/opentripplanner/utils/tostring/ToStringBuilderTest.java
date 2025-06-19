package org.opentripplanner.utils.tostring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.time.ZoneIds;

public class ToStringBuilderTest {

  private static final ZoneId TIME_ZONE_ID_PARIS = ZoneIds.PARIS;

  @Test
  public void ofName() {
    assertEquals("Name{}", ToStringBuilder.of("Name").toString());
  }

  @Test
  public void addFieldIfTrue() {
    assertEquals("ToStringBuilderTest{x}", subject().addBoolIfTrue("x", true).toString());
    assertEquals("ToStringBuilderTest{}", subject().addBoolIfTrue("x", false).toString());
  }

  @Test
  public void addNum() {
    assertEquals("ToStringBuilderTest{num: 3.0}", subject().addNum("num", 3.0000000d).toString());
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
      "ToStringBuilderTest{b: 3.0, d: null}",
      subject()
        .addNum("a", 3d, 3d)
        .addNum("b", 3d, 2d)
        .addNum("c", -1d, -1d)
        .addNum("d", null, 2)
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
  public void addCost() {
    assertEquals(
      "ToStringBuilderTest{a: $30, c: $33.33}",
      subject()
        .addCost("a", 30, 0)
        .addCost("b", 7, 7)
        .addCostCenti("c", 3333, 0)
        .addCostCenti("d", 7, 7)
        .toString()
    );
  }

  @Test
  public void addBool() {
    assertEquals(
      "ToStringBuilderTest{a: true, b: false}",
      subject().addBool("a", true).addBool("b", false).toString()
    );
    assertEquals("ToStringBuilderTest{}", subject().addBool("x", false, false).toString());
    assertEquals("ToStringBuilderTest{x: false}", subject().addBool("x", false, true).toString());
    assertEquals("ToStringBuilderTest{x: true}", subject().addBool("x", true, false).toString());
    assertEquals("ToStringBuilderTest{}", subject().addBool("x", true, true).toString());
  }

  @Test
  public void addStr() {
    assertEquals("{a: 'text'}", ToStringBuilder.of().addStr("a", "text").toString());
    assertEquals("{}", ToStringBuilder.of().addStr("a", null).toString());
    assertEquals("{}", ToStringBuilder.of().addStr("a", "text", "text").toString());
  }

  @Test
  public void addEnum() {
    assertEquals("ToStringBuilderTest{a: A}", subject().addEnum("a", AEnum.A).toString());
    assertEquals("ToStringBuilderTest{}", subject().addEnum("a", AEnum.A, AEnum.A).toString());
    assertEquals("ToStringBuilderTest{}", subject().addEnum("b", null).toString());
    assertEquals("ToStringBuilderTest{a: A}", subject().addEnum("a", AEnum.A, AEnum.B).toString());
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
  public void addObjOpSafe() {
    assertEquals(
      "ToStringBuilderTest{obj: Foo{a: 5, b: 'X'}}",
      subject().addObjOpSafe("obj", () -> new Foo(5, "X")).toString()
    );
    assertEquals("ToStringBuilderTest{}", subject().addObjOpSafe("obj", () -> null).toString());
    assertEquals(
      "ToStringBuilderTest{}",
      subject()
        .addObjOpSafe("obj", () -> {
          throw new IllegalStateException("Ignore");
        })
        .toString()
    );
  }

  @Test
  public void addObjOp() {
    var duration = Duration.ofMinutes(1);
    assertEquals(
      "ToStringBuilderTest{p: 60}",
      subject().addObjOp("p", duration, Duration::toSeconds).toString()
    );
    assertEquals(
      "ToStringBuilderTest{}",
      subject().addObjOp("p", null, Duration::toSeconds).toString()
    );
    assertEquals(
      "ToStringBuilderTest{}",
      subject().addObjOp("p", duration, Duration.ofSeconds(60), Duration::toSeconds).toString()
    );
  }

  @Test
  public void addIntArray() {
    assertEquals(
      "ToStringBuilderTest{a: [1, 2, 3]}",
      subject().addInts("a", new int[] { 1, 2, 3 }).toString()
    );
  }

  @Test
  public void addDoubleArray() {
    assertEquals("ToStringBuilderTest{a: null}", subject().addDoubles("a", null, 1.0).toString());
    assertEquals(
      "ToStringBuilderTest{b: [1.0, 3.0]}",
      subject().addDoubles("b", new double[] { 1.0, 3.0 }, 1.0).toString()
    );
    assertEquals(
      "ToStringBuilderTest{}",
      subject().addDoubles("c", new double[] { 1.0, 1.0 }, 1.0).toString()
    );
  }

  @Test
  public void addCollection() {
    assertEquals("ToStringBuilderTest{}", subject().addCol("c", null).toString());
    assertEquals("ToStringBuilderTest{}", subject().addCol("c", List.of()).toString());
    assertEquals(
      "ToStringBuilderTest{c: [1, 3.0, true]}",
      subject().addCol("c", List.of(1, 3d, true)).toString()
    );
  }

  @Test
  public void addCollectionIgnoreDefault() {
    var dftValue = List.of("A", "B");
    var list = List.of("A", "B");
    var other = List.of("A");
    assertEquals("ToStringBuilderTest{c: null}", subject().addCol("c", null, dftValue).toString());
    assertEquals("ToStringBuilderTest{}", subject().addCol("c", list, dftValue).toString());
    assertEquals("ToStringBuilderTest{c: [A]}", subject().addCol("c", other, dftValue).toString());
  }

  @Test
  public void addCollectionWithCustomToStringOperation() {
    Function<?, String> op = e -> {
      // Should not happen for null and empty collection
      throw new IllegalStateException("" + e);
    };
    assertEquals("ToStringBuilderTest{}", subject().addCol("c", null, op).toString());
    assertEquals("ToStringBuilderTest{}", subject().addCol("c", List.of(), op).toString());

    assertEquals(
      "ToStringBuilderTest{c: [<1>, <3.0>, <true>]}",
      subject().addCol("c", List.of(1, 3d, true), e -> "<" + e + ">").toString()
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

    // null element in list
    var list = new ArrayList<>();
    list.add(null);
    assertEquals(
      "ToStringBuilderTest{c: [null]}",
      subject().addCollection("c", list, 2).toString()
    );
    // collection is null
    assertEquals("ToStringBuilderTest{}", subject().addCollection("c", null, 2).toString());
  }

  @Test
  public void addColSize() {
    assertEquals(
      "ToStringBuilderTest{c: 3 items}",
      subject().addColSize("c", List.of(1, 3, 7)).toString()
    );
    assertEquals("ToStringBuilderTest{}", subject().addColSize("c", null).toString());
  }

  @Test
  public void addIntArraySize() {
    assertEquals(
      "ToStringBuilderTest{c: 2/3}",
      subject().addIntArraySize("c", new int[] { 1, -1, 3 }, -1).toString()
    );
    assertEquals("ToStringBuilderTest{}", subject().addIntArraySize("c", null, -1).toString());
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

    assertEquals("ToStringBuilderTest{}", subject().addBitSetSize("bitSet", null).toString());
  }

  @Test
  public void addDateTime() {
    var time = ZonedDateTime.of(
      LocalDateTime.of(2012, 1, 28, 23, 45, 12),
      TIME_ZONE_ID_PARIS
    ).toInstant();
    assertEquals(
      "ToStringBuilderTest{t: 2012-01-28T22:45:12Z}",
      subject().addDateTime("t", time).toString()
    );
    assertEquals("ToStringBuilderTest{}", subject().addDateTime("t", null).toString());
    assertEquals("ToStringBuilderTest{}", subject().addDateTime("t", time, time).toString());
  }

  @Test
  public void addTime() {
    ZonedDateTime c = ZonedDateTime.of(
      LocalDateTime.of(2012, 1, 28, 23, 45, 12),
      TIME_ZONE_ID_PARIS
    );
    assertEquals(
      "ToStringBuilderTest{t: 2012-01-28T23:45:12}",
      subject().addTime("t", c).toString()
    );
  }

  @Test
  public void addServiceTime() {
    var EXPECTED = "ToStringBuilderTest{t: 2:30:04}";
    // 02:30:04 in seconds is:
    int seconds = TimeUtils.time("2:30:04");

    assertEquals(EXPECTED, subject().addServiceTime("t", seconds, -1).toString());
    assertEquals(EXPECTED, subject().addServiceTime("t", seconds).toString());

    // Expect ignore value
    assertEquals("ToStringBuilderTest{}", subject().addServiceTime("t", -1, -1).toString());
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
  void addDate() {
    LocalDate d = LocalDate.of(2012, 1, 28);
    assertEquals("ToStringBuilderTest{d: 2012-01-28}", subject().addDate("d", d).toString());
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
    var D2m5s = Duration.ofSeconds(125);
    var D1d2h50m45s = Duration.parse("P1dT2h50m45s");

    assertEquals("ToStringBuilderTest{d: 35s}", subject().addDurationSec("d", 35).toString());
    assertEquals(
      "ToStringBuilderTest{d: 1d2h50m45s}",
      subject().addDurationSec("d", (int) D1d2h50m45s.toSeconds()).toString()
    );
    assertEquals("ToStringBuilderTest{d: 2m5s}", subject().addDuration("d", D2m5s).toString());
    assertEquals("ToStringBuilderTest{}", subject().addDurationSec("d", 12, 12).toString());
    assertEquals("ToStringBuilderTest{}", subject().addDuration("d", null, null).toString());
    assertEquals("ToStringBuilderTest{}", subject().addDuration("d", D2m5s, D2m5s).toString());
  }

  @Test
  public void nullSafeToString() {
    assertEquals("null", ToStringBuilder.nullSafeToString(null));
    assertEquals("PT55S", ToStringBuilder.nullSafeToString(Duration.ofSeconds(55)));
  }

  private ToStringBuilder subject() {
    return ToStringBuilder.of(ToStringBuilderTest.class);
  }

  private enum AEnum {
    A,
    B,
  }

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
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Foo foo = (Foo) o;
      return a == foo.a && Objects.equals(b, foo.b);
    }

    @Override
    public String toString() {
      return ToStringBuilder.of(Foo.class).addNum("a", a, 0).addStr("b", b).toString();
    }
  }
}
