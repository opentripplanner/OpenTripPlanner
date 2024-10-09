package org.opentripplanner.framework.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;

class LocalDateUtilsTest {

  @Test
  void asRelativeLocalDate() {
    var zero = LocalDate.of(2022, Month.MAY, 10);

    assertEquals(
      LocalDate.of(2022, Month.MAY, 10),
      LocalDateUtils.asRelativeLocalDate("P0d", zero)
    );
    assertEquals(
      LocalDate.of(2022, Month.MAY, 13),
      LocalDateUtils.asRelativeLocalDate("P3d", zero)
    );
    assertEquals(
      LocalDate.of(2021, Month.MAY, 9),
      LocalDateUtils.asRelativeLocalDate("-P1Y1D", zero)
    );
    assertEquals(
      LocalDate.of(2023, Month.MAY, 9),
      LocalDateUtils.asRelativeLocalDate("P1Y-1D", zero)
    );
  }
}
