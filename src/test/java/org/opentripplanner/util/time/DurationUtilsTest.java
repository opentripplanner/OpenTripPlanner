package org.opentripplanner.util.time;

import org.junit.Test;

import java.time.Duration;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class DurationUtilsTest {
  private final int D9h31m = durationSec(9, 31, 0);
  private final int D9h36m55s = durationSec(9, 36, 55);
  private final int D13h33m57s = durationSec(13, 33, 57);
  private final int D14h0m47s = durationSec(14, 0, 47);
  private final int D15h37m = durationSec(15, 37, 0);
  private final int D9s = durationSec(0, 0, 9);

  @Test
  public void durationToStr() {
    assertEquals("9h36m55s", DurationUtils.durationToStr(D9h36m55s));
    assertEquals("13h33m57s", DurationUtils.durationToStr(D13h33m57s));
    assertEquals("9h31m", DurationUtils.durationToStr(D9h31m));
    assertEquals("9s", DurationUtils.durationToStr(D9s));
    assertEquals("14h47s", DurationUtils.durationToStr(D14h0m47s));
    assertEquals("15h37m", DurationUtils.durationToStr(D15h37m));
    int timeSeconds2 = -D13h33m57s;
    assertEquals("-13h33m57s", DurationUtils.durationToStr(timeSeconds2));
    int timeSeconds1 = -D9h31m;
    assertEquals("-9h31m", DurationUtils.durationToStr(timeSeconds1));
    int timeSeconds = -D9s;
    assertEquals("-9s", DurationUtils.durationToStr(timeSeconds));

    int notSet = 999_999;
    assertEquals("", DurationUtils.durationToStr(notSet, notSet));
  }

  @Test
  public void testDurationToStr() {
    assertEquals("9h36m55s", DurationUtils.durationToStr(Duration.ofSeconds(D9h36m55s)));
    assertEquals("9h31m", DurationUtils.durationToStr(Duration.ofSeconds(D9h31m)));
    assertEquals("9s", DurationUtils.durationToStr(Duration.ofSeconds(D9s)));
  }

  @Test
  public void duration() {
    assertEquals(1, DurationUtils.duration("1s"));
    assertEquals(60, DurationUtils.duration("1m"));
    assertEquals(3600, DurationUtils.duration("1h"));
    assertEquals(3600 + 120 + 3, DurationUtils.duration("1h2m3s"));
    assertEquals(26 * 3600, DurationUtils.duration("26h"));
  }

  @Test
  public void msToSecondsStr() {
    Locale defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.FRANCE);
      assertEquals("0 seconds", DurationUtils.msToSecondsStr(0));
      assertEquals("0,001 seconds", DurationUtils.msToSecondsStr(1));
      assertEquals("0,099 seconds", DurationUtils.msToSecondsStr(99));
      assertEquals("0,10 seconds", DurationUtils.msToSecondsStr(100));
      assertEquals("0,99 seconds", DurationUtils.msToSecondsStr(994));
      assertEquals("1,0 seconds", DurationUtils.msToSecondsStr(995));
      assertEquals("1,0 seconds", DurationUtils.msToSecondsStr(999));
      assertEquals("1 second", DurationUtils.msToSecondsStr(1000));
      assertEquals("1,0 seconds", DurationUtils.msToSecondsStr(1001));
      assertEquals("9,9 seconds", DurationUtils.msToSecondsStr(9_949));
      assertEquals("10 seconds", DurationUtils.msToSecondsStr(9_950));
      assertEquals("-0,456 seconds", DurationUtils.msToSecondsStr(-456));
    }
    finally {
      Locale.setDefault(defaultLocale);
    }
  }

  private static int durationSec(int hour, int min, int sec) {
    return 60 * (60 * hour + min) + sec;
  }
}