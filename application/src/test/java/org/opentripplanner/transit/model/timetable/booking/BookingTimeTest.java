package org.opentripplanner.transit.model.timetable.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.TimeUtils;

class BookingTimeTest {

  BookingTime noon = new BookingTime(LocalTime.NOON, 0);
  BookingTime noonYesterday = new BookingTime(LocalTime.NOON, 1);
  BookingTime midnight = new BookingTime(LocalTime.MIDNIGHT, 0);
  BookingTime noon2 = new BookingTime(LocalTime.NOON, 0);

  @Test
  void equalsAndHashCode() {
    assertEquals(noon, noon);
    assertEquals(noon, noon2);
    assertNotEquals(noon, noonYesterday);
    assertNotEquals(noon, midnight);

    assertEquals(noon.hashCode(), noon.hashCode());
    assertEquals(noon.hashCode(), noon2.hashCode());
    assertNotEquals(noon.hashCode(), noonYesterday.hashCode());
    assertNotEquals(noon.hashCode(), midnight.hashCode());
  }

  @Test
  void getTime() {
    assertEquals(noon.getTime(), LocalTime.NOON);
  }

  @Test
  void testToString() {
    assertEquals("12:00", noon.toString());
    assertEquals("12:00-1d", noonYesterday.toString());
  }

  @Test
  void getDaysPrior() {
    assertEquals(noon.getDaysPrior(), 0);
    assertEquals(noonYesterday.getDaysPrior(), 1);
  }

  @Test
  void relativeTimeSeconds() {
    assertEquals(midnight.relativeTimeSeconds(), 0);
    assertEquals(noon.relativeTimeSeconds(), TimeUtils.ONE_DAY_SECONDS / 2);
    assertEquals(noonYesterday.relativeTimeSeconds(), -TimeUtils.ONE_DAY_SECONDS / 2);
  }
}
