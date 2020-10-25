package org.opentripplanner.netex.mapping.calendar;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ServiceCodeDoesNotContainServiceDates;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.opentripplanner.model.calendar.ServiceCalendarDate.EXCEPTION_TYPE_ADD;
import static org.opentripplanner.model.calendar.ServiceCalendarDate.EXCEPTION_TYPE_REMOVE;

// TODO OTP2 - Add Unit tests
//           - JavaDoc needed
public class CalendarMapper {
    private final DataImportIssueStore issueStore;

    private final Function<String, FeedScopedId> idFactory;
    private final ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> dayTypeAssignmentByDayTypeId;
    private final ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriodById;
    private final ReadOnlyHierarchicalMapById<DayType> dayTypeById;


    public CalendarMapper(
        Function<String, FeedScopedId> idFactory,
            ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> dayTypeAssignmentByDayTypeId,
            ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriodById,
            ReadOnlyHierarchicalMapById<DayType> dayTypeById,
            DataImportIssueStore issueStore
    ) {
        this.idFactory = idFactory;
        this.dayTypeAssignmentByDayTypeId = dayTypeAssignmentByDayTypeId;
        this.operatingPeriodById = operatingPeriodById;
        this.dayTypeById = dayTypeById;
        this.issueStore = issueStore;
    }

    protected void addDataImportIssue(DataImportIssue issue) {
        issueStore.add(issue);
    }

    public Collection<ServiceCalendarDate> mapToCalendarDates(DayTypeRefsToServiceIdAdapter dayTypeRefs) {
        String serviceId = dayTypeRefs.getServiceId();

        // The mapper store intermediate results and need to be initialized every time
        DayTypeAssignmentMapper dayTypeAssignmentMapper = new DayTypeAssignmentMapper(dayTypeById, operatingPeriodById);

        for (String dayTypeId : dayTypeRefs.getDayTypeRefs()) {
            dayTypeAssignmentMapper.mapAll(dayTypeId, dayTypeAssignments(dayTypeId));
        }

        Set<LocalDateTime> dates = dayTypeAssignmentMapper.mergeDates();

        if (dates.isEmpty()) {
            addDataImportIssue(new ServiceCodeDoesNotContainServiceDates(serviceId));
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

    private ServiceCalendarDate newServiceCalendarDate(
            LocalDateTime date, String serviceId, Integer exceptionType
    ) {
        return new ServiceCalendarDate(
                idFactory.apply(serviceId),
                new ServiceDate(date.toLocalDate()),
                exceptionType
        );
    }
}