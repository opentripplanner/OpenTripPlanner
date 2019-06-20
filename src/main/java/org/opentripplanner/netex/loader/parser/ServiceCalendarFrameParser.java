package org.opentripplanner.netex.loader.parser;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypes_RelStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.ServiceCalendar;
import org.rutebanken.netex.model.ServiceCalendarFrame;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class ServiceCalendarFrameParser {

    private final Collection<DayType> dayTypes = new ArrayList<>();

    private final Collection<OperatingPeriod> operatingPeriods = new ArrayList<>();

    private final Multimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId =
            ArrayListMultimap.create();


    void parse(ServiceCalendarFrame scf) {
        if (scf.getServiceCalendar() != null) {
            parseServiceCalendar(scf.getServiceCalendar());
        }

        if (scf.getDayTypes() != null) {
            parseDayTypes(scf.getDayTypes().getDayType_());
        }

        if (scf.getOperatingPeriods() != null) {
            parseOperatingPeriods(
                    scf.getOperatingPeriods().getOperatingPeriodOrUicOperatingPeriod()
            );
        }
        parseDayTypeAssignments(scf.getDayTypeAssignments().getDayTypeAssignment());
    }

    private void parseServiceCalendar(ServiceCalendar serviceCalendar) {
        parseDayTypes(serviceCalendar.getDayTypes());
        // TODO OTP2 - What about OperatingPeriods here?
        parseDayTypeAssignments(serviceCalendar.getDayTypeAssignments().getDayTypeAssignment());
    }

    void setResultOnIndex(NetexImportDataIndex netexIndex) {
        netexIndex.dayTypeById.addAll(dayTypes);
        netexIndex.operatingPeriodById.addAll(operatingPeriods);
        netexIndex.dayTypeAssignmentByDayTypeId.addAll(dayTypeAssignmentByDayTypeId);
    }

    //List<JAXBElement<? extends DataManagedObjectStructure>>
    private void parseDayTypes(List<JAXBElement<? extends DataManagedObjectStructure>> elements) {
        for (JAXBElement dt : elements) {
            parseDayType(dt);
        }
    }

    private void parseDayTypes(DayTypes_RelStructure dayTypes) {
        for (JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()) {
            parseDayType(dt);
        }
    }

    private void parseDayType(JAXBElement dt) {
        if (dt.getValue() instanceof DayType) {
            dayTypes.add((DayType) dt.getValue());
        }
    }

    private void parseOperatingPeriods(List<OperatingPeriod_VersionStructure> periods) {
        for (OperatingPeriod_VersionStructure p : periods) {
            operatingPeriods.add((OperatingPeriod) p);
        }
    }

    private void parseDayTypeAssignments(Collection<DayTypeAssignment> dayTypeAssignments) {
        for (DayTypeAssignment dayTypeAssignment : dayTypeAssignments) {
            String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();
            dayTypeAssignmentByDayTypeId.put(ref, dayTypeAssignment);
        }
    }
}
