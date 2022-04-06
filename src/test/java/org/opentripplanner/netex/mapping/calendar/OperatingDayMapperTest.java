package org.opentripplanner.netex.mapping.calendar;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import org.junit.Test;
import org.opentripplanner.model.calendar.ServiceDate;
import org.rutebanken.netex.model.OperatingDay;

public class OperatingDayMapperTest {

  private static final LocalDateTime LD1 = LocalDateTime.of(2020, 11, 30, 20, 55);
  private static final OperatingDay OPERATING_DAY = new OperatingDay().withCalendarDate(LD1);
  private static final ServiceDate SD1 = new ServiceDate(LD1.toLocalDate());

  @Test
  public void map() {
    assertEquals(SD1, OperatingDayMapper.map(OPERATING_DAY));
  }
}
