package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opentripplanner.model.ServiceCalendarDate.EXCEPTION_TYPE_ADD;
import static org.opentripplanner.model.ServiceCalendarDate.EXCEPTION_TYPE_REMOVE;
import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

// TODO OTP2 - Add Unit tests
// TODO OTP2 - JavaDoc needed
class CalendarMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarMapper.class);

    private final HierarchicalMultimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId;
    private final DayTypeAssignmentMapper dayTypeAssignmentMapper;


    CalendarMapper(
            HierarchicalMultimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId,
            HierarchicalMapById<OperatingPeriod> operatingPeriodById,
            HierarchicalMapById<DayType> dayTypeById
    ) {
        this.dayTypeAssignmentByDayTypeId = dayTypeAssignmentByDayTypeId;
        this.dayTypeAssignmentMapper = new DayTypeAssignmentMapper(dayTypeById, operatingPeriodById);
    }

    static Collection<ServiceCalendarDate> mapToCalendarDates(
            DayTypeRefsToServiceIdAdapter dayTypeRef,
            NetexImportDataIndex netexIndex
    ) {
        return new CalendarMapper(
                netexIndex.dayTypeAssignmentByDayTypeId,
                netexIndex.operatingPeriodById,
                netexIndex.dayTypeById
        ).mapToCalendarDates(dayTypeRef);
    }

    Collection<ServiceCalendarDate> mapToCalendarDates(DayTypeRefsToServiceIdAdapter dayTypeRefs) {
        String serviceId = dayTypeRefs.getServiceId();

        // The mapper store intermediate results and need to be initialized before use
        dayTypeAssignmentMapper.clear();

        for (String dayTypeId : dayTypeRefs.getDayTypeRefs()) {
            dayTypeAssignmentMapper.mapAll(dayTypeId, dayTypeAssignments(dayTypeId));
        }

        Set<LocalDateTime> dates = dayTypeAssignmentMapper.mergeDates();

        if (dates.isEmpty()) {
            LOG.warn("ServiceCode " + serviceId + " does not contain any serviceDates");
            // Add one date exception when list is empty to ensure serviceId is not lost
            LocalDateTime today = LocalDate.now().atStartOfDay();
            return Collections.singleton(
                    newServiceCalendarDate(today, serviceId, EXCEPTION_TYPE_REMOVE)
            );
        }
        return dates.stream()
                .map(it -> newServiceCalendarDate(it, serviceId, EXCEPTION_TYPE_ADD))
                .collect(Collectors.toList());
    }

    private Collection<DayTypeAssignment> dayTypeAssignments(String dayTypeId) {
        return dayTypeAssignmentByDayTypeId.lookup(dayTypeId);
    }

    private static ServiceCalendarDate newServiceCalendarDate(
            LocalDateTime date, String serviceId, Integer exceptionType
    ) {
        ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
        serviceCalendarDate.setServiceId(createFeedScopedId(serviceId));
        serviceCalendarDate.setDate(
                new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
        serviceCalendarDate.setExceptionType(exceptionType);
        return serviceCalendarDate;
    }
}