package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.mapping.DayOfWeekMapper.mapDayOfWeek;
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

public class DayOfWeekMapperTest {

    private static final Set<DayOfWeek> WEEKDAYS_EXPECTED = EnumSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
    );
    private static final Set<DayOfWeek> WEEKEND_EXPECTED = EnumSet.of(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );
    private static final Set<DayOfWeek> EVERYDAY_EXPECTED = EnumSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );

    @Test public void mapDayOfWeekSingleValue() {
        assertEquals(EnumSet.of(DayOfWeek.MONDAY), mapDayOfWeek(MONDAY));
        assertEquals(EnumSet.of(DayOfWeek.TUESDAY), mapDayOfWeek(TUESDAY));
        assertEquals(EnumSet.of(DayOfWeek.WEDNESDAY), mapDayOfWeek(WEDNESDAY));
        assertEquals(EnumSet.of(DayOfWeek.THURSDAY), mapDayOfWeek(THURSDAY));
        assertEquals(EnumSet.of(DayOfWeek.FRIDAY), mapDayOfWeek(FRIDAY));
        assertEquals(EnumSet.of(DayOfWeek.SATURDAY), mapDayOfWeek(SATURDAY));
        assertEquals(EnumSet.of(DayOfWeek.SUNDAY), mapDayOfWeek(SUNDAY));

        assertEquals(WEEKDAYS_EXPECTED, mapDayOfWeek(WEEKDAYS));
        assertEquals(WEEKEND_EXPECTED, mapDayOfWeek(WEEKEND));
        assertEquals(EVERYDAY_EXPECTED, mapDayOfWeek(EVERYDAY));
        assertEquals(EnumSet.noneOf(DayOfWeek.class), mapDayOfWeek(NONE));
    }


    @Test public void mapDayOfWeekSet() {
        // Nothing is mapped to nothing
        assertEquals(
                EnumSet.noneOf(DayOfWeek.class),
                mapDayOfWeek(EnumSet.noneOf(DayOfWeekEnumeration.class))
        );
        // The union of one set with one element is just that one element
        assertEquals(EnumSet.of(DayOfWeek.MONDAY), mapDayOfWeek(EnumSet.of(MONDAY)));

        // The union of two sets with one element is a set with two elements
        assertEquals(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                mapDayOfWeek(EnumSet.of(MONDAY, WEDNESDAY))
        );
        // The union of two sets with the same element is a set with one elements
        assertEquals(
                EnumSet.of(DayOfWeek.MONDAY),
                mapDayOfWeek(Arrays.asList(MONDAY, MONDAY))
        );
        // The union of two none overlapping set contains all elements of both sets
        assertEquals(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                mapDayOfWeek(EnumSet.of(MONDAY, WEEKEND))
        );
        // Union of a set with NONE is a empty set
        assertEquals(
                EnumSet.noneOf(DayOfWeek.class),
                mapDayOfWeek(EnumSet.of(NONE))
        );
        // Union of sets with duplicate result in all duplicates removed
        assertEquals(
                EVERYDAY_EXPECTED,
                mapDayOfWeek(EnumSet.of(MONDAY, WEDNESDAY, WEEKEND, EVERYDAY))
        );
    }
}