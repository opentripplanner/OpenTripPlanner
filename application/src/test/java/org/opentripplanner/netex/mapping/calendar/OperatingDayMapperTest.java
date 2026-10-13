package org.opentripplanner.netex.mapping.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.entur.netex2.model.OperatingDay;
import org.junit.jupiter.api.Test;

public class OperatingDayMapperTest {

  private static final LocalDate LD1 = LocalDate.of(2020, 11, 30);
  private static final OperatingDay OPERATING_DAY = new OperatingDay().withCalendarDate(LD1);

  @Test
  public void map() {
    assertEquals(LD1, OperatingDayMapper.map(OPERATING_DAY));
  }
}
