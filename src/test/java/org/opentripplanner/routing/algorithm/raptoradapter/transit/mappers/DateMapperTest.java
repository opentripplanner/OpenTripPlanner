package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper.asStartOfService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class DateMapperTest {
  private static final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");
  private static final LocalTime TIME = LocalTime.of(10,26);

  private static final LocalDate D2019_03_30 = LocalDate.of(2019, 3, 30);
  // Daylight Saving Time change from Winter to Summer time on MAR 03 2019 in Europe
  private static final LocalDate D2019_03_31 = LocalDate.of(2019, 3, 31);
  private static final LocalDate D2019_04_01 = LocalDate.of(2019, 4, 1);

  private static final LocalDate D2019_10_26 = LocalDate.of(2019, 10, 26);
  // Daylight Saving Time change from Summer to Winter time on OCT 27 2019 in Europe
  private static final LocalDate D2019_10_27 = LocalDate.of(2019, 10, 27);
  private static final LocalDate D2019_10_28 = LocalDate.of(2019, 10, 28);


  private static final ZonedDateTime Z0 = ZonedDateTime.of(D2019_03_30, LocalTime.MIDNIGHT, ZONE_ID);


  // Zoned dates in spring around DST switch
  private static final ZonedDateTime Z1 = ZonedDateTime.of(D2019_03_30, TIME, ZONE_ID);
  private static final ZonedDateTime Z2 = ZonedDateTime.of(D2019_03_31, TIME, ZONE_ID);
  private static final ZonedDateTime Z3 = ZonedDateTime.of(D2019_04_01, TIME, ZONE_ID);

  // Zoned dates in fall around DST switch
  private static final ZonedDateTime Z4 = ZonedDateTime.of(D2019_10_26, TIME, ZONE_ID);
  private static final ZonedDateTime Z5 = ZonedDateTime.of(D2019_10_27, TIME, ZONE_ID);
  private static final ZonedDateTime Z6 = ZonedDateTime.of(D2019_10_28, TIME, ZONE_ID);


  @Test
  public void testAsStartOfServiceWithZonedDatesAroundDST() {
    // Test Zoned dates around DST
    assertEquals("2019-03-30T00:00+01:00[Europe/Paris]", asStartOfService(Z1).toString());
    assertEquals("2019-03-30T23:00+01:00[Europe/Paris]", asStartOfService(Z2).toString());
    assertEquals("2019-04-01T00:00+02:00[Europe/Paris]", asStartOfService(Z3).toString());
    assertEquals("2019-10-26T00:00+02:00[Europe/Paris]", asStartOfService(Z4).toString());
    assertEquals("2019-10-27T01:00+02:00[Europe/Paris]", asStartOfService(Z5).toString());
    assertEquals("2019-10-28T00:00+01:00[Europe/Paris]", asStartOfService(Z6).toString());
  }

  @Test
  public void testAsStartOfServiceWithLocalDatesAndZoneAroundDST() {
    // Test LocalDates with Zone around DST
    assertEquals("2019-03-30T00:00+01:00[Europe/Paris]", asStartOfService(D2019_03_30, ZONE_ID).toString());
    assertEquals("2019-03-30T23:00+01:00[Europe/Paris]", asStartOfService(D2019_03_31, ZONE_ID).toString());
    assertEquals("2019-04-01T00:00+02:00[Europe/Paris]", asStartOfService(D2019_04_01, ZONE_ID).toString());
    assertEquals("2019-10-26T00:00+02:00[Europe/Paris]", asStartOfService(D2019_10_26, ZONE_ID).toString());
    assertEquals("2019-10-27T01:00+02:00[Europe/Paris]", asStartOfService(D2019_10_27, ZONE_ID).toString());
    assertEquals("2019-10-28T00:00+01:00[Europe/Paris]", asStartOfService(D2019_10_28, ZONE_ID).toString());
  }

  @Test
  public void testAsStartOfServiceWithInstance() {
    var time = Instant.parse("2019-03-30T10:00:00Z");
    assertEquals("2019-03-30T00:00+01:00[Europe/Paris]", asStartOfService(time, ZONE_ID).toString());
  }

  @Test
  public void secondsSinceStartOfTime() {
    assertEquals(0, DateMapper.secondsSinceStartOfTime(Z0, D2019_03_30));
    assertEquals(23*3600, DateMapper.secondsSinceStartOfTime(Z0, D2019_03_31));
    assertEquals((23+24)*3600, DateMapper.secondsSinceStartOfTime(Z0, D2019_04_01));

    // Test the Instant version of this method too
    Instant instant = D2019_04_01.atStartOfDay(ZONE_ID).toInstant();
    assertEquals((23+24)*3600, DateMapper.secondsSinceStartOfTime(Z0, instant));
  }
}
