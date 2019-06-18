package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.Collection;

class ServiceCalendarFrameParser {

    private final HierarchicalMapById<DayType> dayTypeById =
            new HierarchicalMapById<>();

    private final HierarchicalMapById<OperatingPeriod> operatingPeriodById =
            new HierarchicalMapById<>();

    private final HierarchicalMultimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId =
            new HierarchicalMultimap<>();

    void parse(ServiceCalendarFrame scf) {
        if (scf.getServiceCalendar() != null) {
            DayTypes_RelStructure dayTypes = scf.getServiceCalendar().getDayTypes();
            for (JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()) {
                if (dt.getValue() instanceof DayType) {
                    DayType dayType = (DayType) dt.getValue();
                    dayTypeById.add(dayType);
                }
            }
        }

        if (scf.getDayTypes() != null) {
            Collection<JAXBElement<? extends DataManagedObjectStructure>> dayTypes = scf.getDayTypes()
                    .getDayType_();
            for (JAXBElement dt : dayTypes) {
                if (dt.getValue() instanceof DayType) {
                    DayType dayType = (DayType) dt.getValue();
                    dayTypeById.add(dayType);
                }
            }
        }

        if (scf.getOperatingPeriods() != null) {
            for (OperatingPeriod_VersionStructure p : scf
                    .getOperatingPeriods().getOperatingPeriodOrUicOperatingPeriod()) {
                operatingPeriodById.add((OperatingPeriod) p);
            }
        }

        Collection<DayTypeAssignment> dayTypeAssignments = scf.getDayTypeAssignments()
                .getDayTypeAssignment();
        for (DayTypeAssignment dayTypeAssignment : dayTypeAssignments) {
            String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();
            dayTypeAssignmentByDayTypeId.add(ref, dayTypeAssignment);
        }
    }

    HierarchicalMapById<DayType> getDayTypeById() {
        return dayTypeById;
    }

    HierarchicalMapById<OperatingPeriod> getOperatingPeriodById() {
        return operatingPeriodById;
    }

    HierarchicalMultimap<String, DayTypeAssignment> getDayTypeAssignmentByDayTypeId() {
        return dayTypeAssignmentByDayTypeId;
    }
}
