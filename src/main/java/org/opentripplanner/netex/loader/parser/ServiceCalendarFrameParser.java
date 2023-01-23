package org.opentripplanner.netex.loader.parser;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeAssignmentsInFrame_RelStructure;
import org.rutebanken.netex.model.DayTypeAssignments_RelStructure;
import org.rutebanken.netex.model.DayTypesInFrame_RelStructure;
import org.rutebanken.netex.model.DayTypes_RelStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDaysInFrame_RelStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.OperatingPeriodsInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceCalendar;
import org.rutebanken.netex.model.ServiceCalendarFrame_VersionFrameStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServiceCalendarFrameParser extends NetexParser<ServiceCalendarFrame_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceCalendarFrameParser.class);

  private final Collection<DayType> dayTypes = new ArrayList<>();
  private final Collection<OperatingPeriod> operatingPeriods = new ArrayList<>();
  private final Collection<OperatingDay> operatingDays = new ArrayList<>();
  private final Multimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId = ArrayListMultimap.create();

  @Override
  void parse(ServiceCalendarFrame_VersionFrameStructure frame) {
    parseServiceCalendar(frame.getServiceCalendar());
    parseDayTypes(frame.getDayTypes());
    parseOperatingPeriods(frame.getOperatingPeriods());
    parseOperatingDays(frame.getOperatingDays());
    parseDayTypeAssignments(frame.getDayTypeAssignments());

    // Keep list sorted alphabetically

    warnOnMissingMapping(LOG, frame.getTimebands());
    warnOnMissingMapping(LOG, frame.getGroupOfTimebands());

    verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
  }

  @Override
  void setResultOnIndex(NetexEntityIndex netexIndex) {
    netexIndex.dayTypeById.addAll(dayTypes);
    netexIndex.operatingPeriodById.addAll(operatingPeriods);
    netexIndex.operatingDayById.addAll(operatingDays);
    netexIndex.dayTypeAssignmentByDayTypeId.addAll(dayTypeAssignmentByDayTypeId);
  }

  private void parseServiceCalendar(ServiceCalendar serviceCalendar) {
    if (serviceCalendar == null) return;

    parseDayTypes(serviceCalendar.getDayTypes());
    // TODO OTP2 - What about OperatingPeriods here?
    parseDayTypeAssignments(serviceCalendar.getDayTypeAssignments());
  }

  //List<JAXBElement<? extends DataManagedObjectStructure>>
  private void parseDayTypes(DayTypesInFrame_RelStructure element) {
    if (element == null) return;
    for (JAXBElement<?> dt : element.getDayType_()) {
      parseDayType(dt);
    }
  }

  private void parseDayTypes(DayTypes_RelStructure dayTypes) {
    for (JAXBElement<?> dt : dayTypes.getDayTypeRefOrDayType_()) {
      parseDayType(dt);
    }
  }

  private void parseDayType(JAXBElement<?> dt) {
    if (dt.getValue() instanceof DayType) {
      dayTypes.add((DayType) dt.getValue());
    }
  }

  private void parseOperatingPeriods(OperatingPeriodsInFrame_RelStructure element) {
    if (element == null) {
      return;
    }

    for (OperatingPeriod_VersionStructure p : element.getOperatingPeriodOrUicOperatingPeriod()) {
      operatingPeriods.add((OperatingPeriod) p);
    }
  }

  private void parseOperatingDays(OperatingDaysInFrame_RelStructure element) {
    if (element == null) {
      return;
    }
    operatingDays.addAll(element.getOperatingDay());
  }

  private void parseDayTypeAssignments(DayTypeAssignments_RelStructure element) {
    if (element == null) {
      return;
    }
    parseDayTypeAssignments(element.getDayTypeAssignment());
  }

  private void parseDayTypeAssignments(DayTypeAssignmentsInFrame_RelStructure element) {
    if (element == null) {
      return;
    }
    parseDayTypeAssignments(element.getDayTypeAssignment());
  }

  private void parseDayTypeAssignments(List<DayTypeAssignment> elements) {
    for (DayTypeAssignment it : elements) {
      String ref = it.getDayTypeRef().getValue().getRef();
      dayTypeAssignmentByDayTypeId.put(ref, it);
    }
  }
}
