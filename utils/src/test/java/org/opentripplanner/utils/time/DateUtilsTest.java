package org.opentripplanner.utils.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class DateUtilsTest {

  public static final ZoneId UTC = ZoneIds.UTC;

  @Test
  public final void testToDate() {
    ZonedDateTime date = DateUtils.toZonedDateTime("1970-01-01", "00:00", UTC);
    assertEquals("1970-01-01", date.toLocalDate().toString());
    assertEquals(0, date.toEpochSecond());

    date = DateUtils.toZonedDateTime(null, "00:00", UTC);
    assertEquals(LocalDate.now(UTC).toString(), date.toLocalDate().toString());
    assertEquals(0, date.toEpochSecond() % TimeUtils.ONE_DAY_SECONDS);
  }
}
