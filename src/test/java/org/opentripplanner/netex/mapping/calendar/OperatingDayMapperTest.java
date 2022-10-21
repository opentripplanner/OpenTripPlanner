package org.opentripplanner.netex.mapping.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.OperatingDay;

public class OperatingDayMapperTest {

  private static final LocalDateTime LD1 = LocalDateTime.of(2020, 11, 30, 20, 55);
  private static final OperatingDay OPERATING_DAY = new OperatingDay().withCalendarDate(LD1);

  @Test
  public void map() {
    assertEquals(LD1.toLocalDate(), OperatingDayMapper.map(OPERATING_DAY));
  }
}
