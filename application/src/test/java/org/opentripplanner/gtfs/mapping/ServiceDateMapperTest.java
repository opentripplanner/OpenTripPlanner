package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.gtfs.mapping.ServiceDateMapper.mapLocalDate;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class ServiceDateMapperTest {

  @Test
  public void testMapServiceDate() throws Exception {
    ServiceDate input = new ServiceDate(2017, 10, 3);

    LocalDate result = mapLocalDate(input);

    assertEquals(2017, result.getYear());
    assertEquals(Month.OCTOBER, result.getMonth());
    assertEquals(3, result.getDayOfMonth());
  }

  @Test
  public void testMapServiceDateNullRef() throws Exception {
    assertNull(mapLocalDate(null));
  }
}
