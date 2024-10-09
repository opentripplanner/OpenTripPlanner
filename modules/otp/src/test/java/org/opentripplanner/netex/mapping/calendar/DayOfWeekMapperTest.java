package org.opentripplanner.netex.mapping.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.mapping.calendar.DayOfWeekMapper.mapDayOfWeek;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.EVERYDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.FRIDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.MONDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.NONE;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.SATURDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.SUNDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.THURSDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.TUESDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.WEDNESDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.WEEKDAYS;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.WEEKEND;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DayOfWeekMapperTest {

  private static final Set<DayOfWeek> WEEKDAYS_EXPECTED = Set.of(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
  );
  private static final Set<DayOfWeek> WEEKEND_EXPECTED = Set.of(
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
  );
  private static final Set<DayOfWeek> EVERYDAY_EXPECTED = Set.of(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
  );

  @Test
  public void mapDayOfWeekSingleValue() {
    assertEquals(Set.of(DayOfWeek.MONDAY), mapDayOfWeek(MONDAY));
    assertEquals(Set.of(DayOfWeek.TUESDAY), mapDayOfWeek(TUESDAY));
    assertEquals(Set.of(DayOfWeek.WEDNESDAY), mapDayOfWeek(WEDNESDAY));
    assertEquals(Set.of(DayOfWeek.THURSDAY), mapDayOfWeek(THURSDAY));
    assertEquals(Set.of(DayOfWeek.FRIDAY), mapDayOfWeek(FRIDAY));
    assertEquals(Set.of(DayOfWeek.SATURDAY), mapDayOfWeek(SATURDAY));
    assertEquals(Set.of(DayOfWeek.SUNDAY), mapDayOfWeek(SUNDAY));

    assertEquals(WEEKDAYS_EXPECTED, mapDayOfWeek(WEEKDAYS));
    assertEquals(WEEKEND_EXPECTED, mapDayOfWeek(WEEKEND));
    assertEquals(EVERYDAY_EXPECTED, mapDayOfWeek(EVERYDAY));
    assertEquals(Set.of(), mapDayOfWeek(NONE));
  }

  @Test
  public void mapDayOfWeekSet() {
    // Nothing is mapped to nothing
    assertEquals(Set.of(), DayOfWeekMapper.mapDayOfWeeks(Set.of()));

    // The union of one set with one element is just that one element
    assertEquals(Set.of(DayOfWeek.MONDAY), DayOfWeekMapper.mapDayOfWeeks(Set.of(MONDAY)));

    // The union of two sets with one element is a set with two elements
    assertEquals(
      Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
      DayOfWeekMapper.mapDayOfWeeks(Set.of(MONDAY, WEDNESDAY))
    );
    // The union of two sets with the same element is a set with one elements
    assertEquals(
      Set.of(DayOfWeek.MONDAY),
      DayOfWeekMapper.mapDayOfWeeks(Arrays.asList(MONDAY, MONDAY))
    );
    // The union of two none overlapping set contains all elements of both sets
    assertEquals(
      Set.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
      DayOfWeekMapper.mapDayOfWeeks(Set.of(MONDAY, WEEKEND))
    );
    // Union of a set with NONE is a empty set
    assertEquals(Set.of(), DayOfWeekMapper.mapDayOfWeeks(Set.of(NONE)));

    // Union of sets with duplicate result in all duplicates removed
    assertEquals(
      EVERYDAY_EXPECTED,
      DayOfWeekMapper.mapDayOfWeeks(Set.of(MONDAY, WEEKEND, EVERYDAY))
    );
  }
}
