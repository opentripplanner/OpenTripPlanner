package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Map between NeTEx {@link DayOfWeekEnumeration} to Java {@link DayOfWeek}. The NeTEx version have
 * "collection" type elements like WEEKDAYS, WEEKEND, EVERYDAY and NONE. Beacuse of this, the
 * mapping is not ono-to-one, but rather one-to-many.
 */
class DayOfWeekMapper {

    /** Utility class with static methods, prevent instantiation with private constructor */
    private DayOfWeekMapper() {}

    /**
     * Return a set Java DayOfWeek representing a union of all input values given. Each value is
     * mapped to a set of Java DayOfWeek, which is merged into one set.
     * <p/>
     * [MONDAY, SATURDAY, WEEKEND] => [MONDAY, SATURDAY, SUNDAY]
     */
    static Set<DayOfWeek> mapDayOfWeek(Collection<DayOfWeekEnumeration> values) {
        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeekEnumeration it : values) {
            result.addAll(mapDayOfWeek(it));
        }
        return result;
    }

    /**
     * Maps given {@code value} into a set of Java DayOfWeek.
     * <ul>
     * <li>MONDAY to SUNDAY is mapped to a Set with one element
     * <li>NONE is mapped to an empty set
     * <li>WEEKDAYS is mapped to a set of MONDAY..FRIDAY
     * <li>WEEKEND is mapped to a set of SATURDAY..SUNDAY
     * <li>EVERYDAY is mapped to a set of MONDAY..SUNDAY
     * </ul>
     */
    static Set<DayOfWeek> mapDayOfWeek(DayOfWeekEnumeration value) {
        switch (value) {
        case MONDAY : return EnumSet.of(DayOfWeek.MONDAY);
        case TUESDAY:  return EnumSet.of(DayOfWeek.TUESDAY);
        case WEDNESDAY: return EnumSet.of(DayOfWeek.WEDNESDAY);
        case THURSDAY : return EnumSet.of(DayOfWeek.THURSDAY);
        case FRIDAY:  return EnumSet.of(DayOfWeek.FRIDAY);
        case SATURDAY : return EnumSet.of(DayOfWeek.SATURDAY);
        case SUNDAY: return EnumSet.of(DayOfWeek.SUNDAY);
        case WEEKDAYS : return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        case WEEKEND : return EnumSet.range(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        case EVERYDAY : return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SUNDAY);
        case NONE: return EnumSet.noneOf(DayOfWeek.class);
        }
        throw new IllegalArgumentException("Day of week enum mapping missing: " + value);
    }


}
