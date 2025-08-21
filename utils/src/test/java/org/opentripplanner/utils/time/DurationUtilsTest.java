package org.opentripplanner.utils.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.utils.time.DurationUtils.requireNonNegative;
import static org.opentripplanner.utils.time.DurationUtils.requireNonNegativeMax2days;
import static org.opentripplanner.utils.time.DurationUtils.requireNonNegativeMax2hours;
import static org.opentripplanner.utils.time.DurationUtils.requireNonNegativeMax30minutes;
import static org.opentripplanner.utils.time.DurationUtils.toIntMilliseconds;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DurationUtilsTest {

  private final Duration NEG_1s = Duration.ofSeconds(-1);
  private final Duration D1s = Duration.ofSeconds(1);
  private final Duration D3d = Duration.ofDays(3);
  private final Duration D2h = Duration.ofHours(2);
  private final Duration D5m = Duration.ofMinutes(5);
  private final Duration D9s = Duration.ofSeconds(9);
  private final Duration D3d5m9s = D3d.plus(D5m).plus(D9s);
  private final int I9h31m = durationSec(9, 31, 0);
  private final int I9h36m55s = durationSec(9, 36, 55);
  private final int I13h33m57s = durationSec(13, 33, 57);
  private final int I14h0m47s = durationSec(14, 0, 47);
  private final int I15h37m = durationSec(15, 37, 0);

  @Test
  public void durationToStrFromSeconds() {
    assertEquals("9h36m55s", DurationUtils.durationToStr(I9h36m55s));
    assertEquals("13h33m57s", DurationUtils.durationToStr(I13h33m57s));
    assertEquals("9h31m", DurationUtils.durationToStr(I9h31m));
    assertEquals("9s", DurationUtils.durationToStr(D9s));
    assertEquals("14h47s", DurationUtils.durationToStr(I14h0m47s));
    assertEquals("15h37m", DurationUtils.durationToStr(I15h37m));
    int timeSeconds2 = -I13h33m57s;
    assertEquals("-13h33m57s", DurationUtils.durationToStr(timeSeconds2));
    int timeSeconds1 = -I9h31m;
    assertEquals("-9h31m", DurationUtils.durationToStr(timeSeconds1));
    int timeSeconds = -(int) D9s.toSeconds();
    assertEquals("-9s", DurationUtils.durationToStr(timeSeconds));

    int notSet = 999_999;
    assertEquals("", DurationUtils.durationToStr(notSet, notSet));
  }

  @Test
  public void durationToStrFromDurationType() {
    assertEquals("9h36m55s", DurationUtils.durationToStr(Duration.ofSeconds(I9h36m55s)));
    assertEquals("9h31m", DurationUtils.durationToStr(Duration.ofSeconds(I9h31m)));
    assertEquals("9s", DurationUtils.durationToStr(D9s));
    assertEquals("", DurationUtils.durationToStr(null));
  }

  @Test
  public void durationInSeconds() {
    assertEquals(1, DurationUtils.durationInSeconds("1s"));
    assertEquals(60, DurationUtils.durationInSeconds("1m"));
    assertEquals(3600, DurationUtils.durationInSeconds("1h"));
    assertEquals(3600 + 120 + 3, DurationUtils.durationInSeconds("1h2m3s"));
    assertEquals(26 * 3600, DurationUtils.durationInSeconds("26h"));
    assertEquals(48 * 3600, DurationUtils.durationInSeconds("P2d"));
  }

  @Test
  public void duration() {
    assertEquals(D9s, DurationUtils.duration("9s"));
    assertEquals(D2h, DurationUtils.duration("2h"));
    assertEquals(D3d, DurationUtils.duration("3D"));
    assertEquals(D3d5m9s, DurationUtils.duration("3d5m9s"));

    // With the PT prefix/divider
    assertEquals(D9s, DurationUtils.duration("PT9s"));
    assertEquals(D3d, DurationUtils.duration("P3d"));
    assertEquals(D3d5m9s, DurationUtils.duration("P3dT5m9s"));

    // With unit
    assertEquals(D9s, DurationUtils.duration("PT9s", ChronoUnit.DAYS), "ignore unit");
    assertEquals(D9s, DurationUtils.duration("9", ChronoUnit.SECONDS));
    assertEquals(-D9s.toSeconds(), DurationUtils.duration("-9", ChronoUnit.SECONDS).toSeconds());
  }

  @Test
  public void parseSecondsOrDuration() {
    assertEquals(D9s, DurationUtils.parseSecondsOrDuration("9s").orElseThrow());
    assertEquals(D9s, DurationUtils.parseSecondsOrDuration("9").orElseThrow());
    assertEquals(D2h, DurationUtils.parseSecondsOrDuration("2h").orElseThrow());
    assertEquals(D2h, DurationUtils.parseSecondsOrDuration("7200").orElseThrow());
    assertEquals(D3d, DurationUtils.parseSecondsOrDuration("3D").orElseThrow());

    // Negative values
    assertEquals(D5m.negated(), DurationUtils.parseSecondsOrDuration("-5m").orElseThrow());
    assertEquals(D5m.negated(), DurationUtils.parseSecondsOrDuration("-300").orElseThrow());

    // Asserts handle bad input
    assertEquals(
      D3d,
      DurationUtils.parseSecondsOrDuration(Integer.toString(3 * 24 * 60 * 60)).orElseThrow()
    );

    assertThrows(DateTimeParseException.class, () ->
      DurationUtils.parseSecondsOrDuration("not-a-duration").orElseThrow()
    );
  }

  @Test
  public void durations() {
    assertEquals(List.of(), DurationUtils.durations(""));
    assertEquals(List.of(Duration.ZERO), DurationUtils.durations("0s"));
    assertEquals(List.of(D9s, D2h, D5m), DurationUtils.durations("9s 2h 5m"));
    assertEquals(List.of(D9s, D2h, D5m), DurationUtils.durations("9s;2h,5m"));
  }

  @Test
  public void testRequireNonNegative() {
    assertThrows(NullPointerException.class, () -> requireNonNegative(null));
    assertThrows(IllegalArgumentException.class, () -> requireNonNegative(Duration.ofSeconds(-1)));
  }

  @Test
  public void testRequireNonNegativeAndMaxLimit() {
    // Firs make sure legal values are accepted
    requireNonNegative(Duration.ZERO, D2h, "test");
    requireNonNegative(D2h.minus(D1s), D2h, "test");

    // null is not supported
    assertThrows(NullPointerException.class, () -> requireNonNegative(null, D2h, "test"));

    // Test max limit
    var ex = assertThrows(IllegalArgumentException.class, () ->
      requireNonNegative(D2h, D2h, "test")
    );
    assertEquals("Duration test can't be longer or equals too 2h: PT2H", ex.getMessage());

    // Test non-negative
    ex = assertThrows(IllegalArgumentException.class, () ->
      requireNonNegative(NEG_1s, D2h, "test")
    );
    assertEquals("Duration test can't be negative: PT-1S", ex.getMessage());
  }

  @Test
  public void testRequireNonNegativeLong() {
    assertThrows(NullPointerException.class, () -> requireNonNegativeMax2days(null, "test"));
    assertThrows(IllegalArgumentException.class, () ->
      requireNonNegativeMax2days(Duration.ofSeconds(-1), "test")
    );
    assertThrows(IllegalArgumentException.class, () ->
      requireNonNegativeMax2days(Duration.ofDays(3), "test")
    );
  }

  @Test
  public void testRequireNonNegativeMedium() {
    assertThrows(NullPointerException.class, () -> requireNonNegativeMax2hours(null, "test"));
    assertThrows(IllegalArgumentException.class, () ->
      requireNonNegativeMax2hours(Duration.ofSeconds(-1), "test")
    );
    assertThrows(IllegalArgumentException.class, () ->
      requireNonNegativeMax2hours(Duration.ofHours(3), "test")
    );
  }

  @Test
  public void testRequireNonNegativeShort() {
    assertThrows(NullPointerException.class, () -> requireNonNegativeMax30minutes(null, "test"));
    assertThrows(IllegalArgumentException.class, () ->
      requireNonNegativeMax30minutes(Duration.ofSeconds(-1), "test")
    );
    assertThrows(IllegalArgumentException.class, () ->
      requireNonNegativeMax30minutes(Duration.ofMinutes(31), "test")
    );
  }

  @Test
  public void testToIntMilliseconds() {
    assertEquals(20, toIntMilliseconds(null, 20));
    assertEquals(0, toIntMilliseconds(Duration.ZERO, 20));
    assertEquals(123000, toIntMilliseconds(Duration.ofSeconds(123), -1));
  }

  @Test
  @ResourceLock(Resources.LOCALE)
  public void msToSecondsStr() {
    Locale defaultLocale = Locale.getDefault();
    try {
      // Setting the default locale should have no effect
      Locale.setDefault(Locale.FRANCE);
      assertEquals("0 seconds", DurationUtils.msToSecondsStr(0));
      assertEquals("0.001 seconds", DurationUtils.msToSecondsStr(1));
      assertEquals("0.099 seconds", DurationUtils.msToSecondsStr(99));
      assertEquals("0.10 seconds", DurationUtils.msToSecondsStr(100));
      assertEquals("0.99 seconds", DurationUtils.msToSecondsStr(994));
      assertEquals("1.0 seconds", DurationUtils.msToSecondsStr(995));
      assertEquals("1.0 seconds", DurationUtils.msToSecondsStr(999));
      assertEquals("1 second", DurationUtils.msToSecondsStr(1000));
      assertEquals("1.0 seconds", DurationUtils.msToSecondsStr(1001));
      assertEquals("9.9 seconds", DurationUtils.msToSecondsStr(9_949));
      assertEquals("10 seconds", DurationUtils.msToSecondsStr(9_950));
      assertEquals("-0.456 seconds", DurationUtils.msToSecondsStr(-456));
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  static List<Arguments> durationCases() {
    return List.of(
      of(Duration.ofSeconds(30), "PT30S"),
      of(Duration.ofMinutes(30), "PT30M"),
      of(Duration.ofHours(23), "PT23H"),
      of(Duration.ofSeconds(-30), "-PT30S"),
      of(Duration.ofMinutes(-10), "-PT10M"),
      of(Duration.ofMinutes(-90), "-PT1H30M"),
      of(Duration.ofMinutes(-184), "-PT3H4M")
    );
  }

  @ParameterizedTest
  @MethodSource("durationCases")
  void formatDuration(Duration duration, String expected) {
    var string = DurationUtils.formatDurationWithLeadingMinus(duration);

    assertEquals(expected, string);

    var parsed = Duration.parse(expected);
    assertEquals(parsed, duration);
  }

  private static int durationSec(int hour, int min, int sec) {
    return 60 * (60 * hour + min) + sec;
  }
}
