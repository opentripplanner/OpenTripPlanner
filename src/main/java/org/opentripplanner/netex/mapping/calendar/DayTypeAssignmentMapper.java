package org.opentripplanner.netex.mapping.calendar;

import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.opentripplanner.netex.mapping.support.NetexObjectDecorator.logUnmappedEntityRef;

/**
 * Map {@link DayTypeAssignment}s to set of {@link LocalDateTime}.
 * <p>
 * The mapper is stateful and uses a 2 step approtch:
 * <p>
 * - Create a new mapper
 * - Add each dayTypeId and list of assignments (multiple calls)
 * - Merge dates. Take the list of all available days and remove all none available dates.
 * <p>
 * THIS CLASS IS NOT THREAD-SAFE, USE SEPARATE INSTANCES FOR EACH THREAD.
 */
class DayTypeAssignmentMapper {
    private static final Logger LOG = LoggerFactory.getLogger(DayTypeAssignmentMapper.class);

    private final Set<LocalDateTime> dates = new HashSet<>();
    private final Set<LocalDateTime> datesToRemove = new HashSet<>();

    private final ReadOnlyHierarchicalMapById<DayType> dayTypeById;
    private final ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriodById;

    private String dayTypeId;

    /** This flag is used to prevent the merge operation to be performed more than one time. */
    private Boolean merged = false;

    DayTypeAssignmentMapper(
            ReadOnlyHierarchicalMapById<DayType> dayTypeById,
            ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriodById
    ) {
        this.dayTypeById = dayTypeById;
        this.operatingPeriodById = operatingPeriodById;
    }

    /**
     * Map all given {@code dayTypeAssignments} for {@code dayTypeId}. The result is kept
     * internally in the mapper until all mapping are performed.
     * <p>
     * Retrieve the results using {@link #mergeDates()}.
     */
    void mapAll(String dayTypeId, Collection<DayTypeAssignment> dayTypeAssignments) {
        assertNotMerged();
        this.dayTypeId = dayTypeId;
        for (DayTypeAssignment it : dayTypeAssignments) {
            map(it);
        }
    }

    /**
     * When mapping two lists of dates are created internally in this mapping class, these are
     * merged as a final step in the mapping process.
     * <p>
     * Do not call this method before you want to retrieve the result. Calling this method more
     * than once, may have unexpected effects.
     * <p>
     * @return the list of service dates for all dayTypes mapped.
     */
    Set<LocalDateTime> mergeDates() {
        dates.removeAll(datesToRemove);
        merged = true;
        return dates;
    }


    /* private methods */

    private void assertNotMerged() {
        if(merged) throw new IllegalStateException(
                "This mapper can only be used once, when the result is merged this method is not alloed any more."
        );
    }

    private void map(DayTypeAssignment dayTypeAssignment) {
        // Add or remove single days
        if (dayTypeAssignment.getDate() != null) {
            addSpecificDate(dayTypeAssignment);
        }
        // Add or remove periods
        else if (dayTypeAssignment.getOperatingPeriodRef() != null) {
            addOperationPeriod(dayTypeAssignment);
        }
        else if(dayTypeAssignment.getOperatingDayRef() != null) {
            // OperatingDay is part of the Norwegian profile, but no mapping is yet to be
            // implemented
            logUnmappedEntityRef(LOG, dayTypeAssignment.getOperatingDayRef());
        }
    }

    private void addSpecificDate(DayTypeAssignment dayTypeAssignment) {
        addDate(isDayTypeAvailableForAssigment(dayTypeAssignment), dayTypeAssignment.getDate());
    }

    private void addOperationPeriod(DayTypeAssignment dayTypeAssignment) {
        boolean isAvailable = isDayTypeAvailableForAssigment(dayTypeAssignment);

        String ref = dayTypeAssignment.getOperatingPeriodRef().getRef();
        OperatingPeriod period = operatingPeriodById.lookup(ref);

        if (period != null) {
            Set<DayOfWeek> daysOfWeek = daysOfWeekForDayType(dayTypeById.lookup(dayTypeId));

            // Plus 1 to make the end date exclusive - simplify the loop test
            LocalDateTime endDate = period.getToDate().plusDays(1);
            LocalDateTime date = period.getFromDate();

            for (; date.isBefore(endDate); date = date.plusDays(1)) {
                if (daysOfWeek.contains(date.getDayOfWeek())) {
                    addDate(isAvailable, date);
                }
            }
        }
    }

    private void addDate(boolean isAvailable, LocalDateTime date) {
        if (isAvailable) {
            dates.add(date);
        } else {
            datesToRemove.add(date);
        }
    }

    private static boolean isDayTypeAvailableForAssigment(DayTypeAssignment dta) {
        if(dta.isIsAvailable() == null) {
            return true;
        }
        return dta.isIsAvailable();
    }

    private static Set<DayOfWeek> daysOfWeekForDayType(DayType dayType) {
        Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);

        if (dayType.getProperties() != null) {
            List<PropertyOfDay> propertyOfDays = dayType.getProperties().getPropertyOfDay();

            for (PropertyOfDay p : propertyOfDays) {
                result.addAll(DayOfWeekMapper.mapDayOfWeek(p.getDaysOfWeek()));
            }
        }
        return result;
    }
}
