package org.opentripplanner.framework.tostring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class ValueObjectToStringBuilderTest {

  @Test
  public void addNum() {
    assertEquals("30,000.0", subject().addNum(30_000d).toString());
    assertEquals("3.0", subject().addNum(3.0000f).toString());
    assertEquals("3,000", subject().addNum(3000).toString());
    assertEquals("3", subject().addNum(3L).toString());
    assertEquals(
      "(-null)",
      subject()
        .addText("(")
        .skipNull()
        .addNum(null)
        .addText("-")
        .includeNull()
        .addNum(null)
        .addText(")")
        .toString()
    );
  }

  @Test
  public void testAddNumWithUnit() {
    assertEquals(
      "3 minutes 7,000 seconds",
      subject()
        .addNum(3, " minutes")
        .addNum(7000, " seconds")
        .skipNull()
        .addNum(null, "cows")
        .toString()
    );
  }

  @Test
  public void addBool() {
    assertEquals(
      "include nothing null",
      subject()
        .addBool(true, "include", "skip")
        .addBool(false, "everything", "nothing")
        .addBool(null, "all", "nothing")
        .skipNull()
        .addBool(null, "to skip", "or not to skip")
        .toString()
    );
  }

  @Test
  public void addStr() {
    assertEquals("'text'", subject().addStr("text").toString());
    assertEquals("null-", subject().addStr(null).addText("-").skipNull().addStr(null).toString());
  }

  @Test
  public void addText() {
    assertEquals("abba", subject().addText("ab").addText("ba").toString());
    assertEquals("a_2_b", subject().addText("a_").addNum(2).addText("_b").toString());
  }

  @Test
  public void addEnum() {
    assertEquals("A", subject().addEnum(AEnum.A).toString());
    assertEquals("null", subject().addEnum(null).toString());
    assertEquals("", subject().skipNull().addEnum(null).toString());
  }

  @Test
  public void addObj() {
    assertEquals("5 meters 'X'", subject().addObj(new Foo(5, "X")).toString());
    assertEquals("null", subject().addObj(null).toString());
    assertEquals("", subject().skipNull().addObj(null).toString());
  }

  @Test
  public void addCoordinate() {
    assertEquals(
      "(60.98766, 11.98)",
      subject().addCoordinate(60.9876599999999d, 11.98d).toString()
    );
    assertEquals("(null, null)", subject().addCoordinate(null, null).toString());
    assertEquals("", subject().skipNull().addCoordinate(null, null).toString());
  }

  @Test
  public void addSecondsPastMidnight() {
    assertEquals("0:00:35", subject().addServiceTime(35).toString());
    assertEquals("2:50:45+1d", subject().addServiceTime((26 * 60 + 50) * 60 + 45).toString());
    assertEquals("23:59:59-1d", subject().addServiceTime(-1).toString());
  }

  @Test
  public void addDuration() {
    assertEquals("35s", subject().addDurationSec(35).toString());
    assertEquals("1d2h50m45s", subject().addDurationSec((26 * 60 + 50) * 60 + 45).toString());
    assertEquals("35s", subject().addDuration(Duration.ofSeconds(35)).toString());

    assertEquals("", subject().skipNull().addDurationSec(null).toString());
    assertEquals("", subject().skipNull().addDuration(null).toString());
  }

  @Test
  public void addTime() {
    assertEquals("1970-01-01T01:01:01Z", subject().addTime(Instant.ofEpochSecond(3661)).toString());
  }

  @Test
  public void addCost() {
    assertEquals("null", subject().addCostCenti(null).toString());
    assertEquals("", subject().skipNull().addCostCenti(null).toString());
    assertEquals("$-0.01", subject().addCostCenti(-1).toString());
    assertEquals("$0", subject().addCostCenti(0).toString());
    assertEquals("$0.01", subject().addCostCenti(1).toString());
    assertEquals("$1", subject().addCostCenti(100).toString());
    assertEquals("$100.01", subject().addCostCenti(10001).toString());

    assertEquals("null", subject().addCost(null).toString());
    assertEquals("", subject().skipNull().addCost(null).toString());
    assertEquals("$-1", subject().addCost(-1).toString());
    assertEquals("$0", subject().addCost(0).toString());
    assertEquals("$100", subject().addCost(100).toString());

    assertEquals("null", subject().addCostCenti(null, "pip").toString());
    assertEquals("", subject().skipNull().addCostCenti(null, "pip").toString());
    assertEquals("$-0.01pip", subject().addCostCenti(-1, "pip").toString());
    assertEquals("$1pip", subject().addCostCenti(100, "pip").toString());
  }

  private ValueObjectToStringBuilder subject() {
    return ValueObjectToStringBuilder.of();
  }

  private enum AEnum {
    A,
  }

  private static class Foo {

    int a;
    String b;

    public Foo(int a, String b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public String toString() {
      return ValueObjectToStringBuilder.of().addNum(a, " meters").addStr(b).toString();
    }
  }
}
