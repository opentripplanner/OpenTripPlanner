package org.opentripplanner.netex.loader.parser;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.entur.netex2.model.DayType;
import org.entur.netex2.model.DayTypeAssignment;
import org.entur.netex2.model.DayTypeAssignmentsInFrame_RelStructure;
import org.entur.netex2.model.DayTypeAssignments_RelStructure;
import org.entur.netex2.model.DayTypesInFrame_RelStructure;
import org.entur.netex2.model.DayTypes_RelStructure;
import org.entur.netex2.model.OperatingDay;
import org.entur.netex2.model.OperatingDaysInFrame_RelStructure;
import org.entur.netex2.model.OperatingPeriod_VersionStructure;
import org.entur.netex2.model.OperatingPeriodsInFrame_RelStructure;
import org.entur.netex2.model.OperatingPeriods_RelStructure;
import org.entur.netex2.model.ServiceCalendar;
import org.entur.netex2.model.ServiceCalendarFrame_VersionFrameStructure;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.support.JAXBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServiceCalendarFrameParser extends NetexParser<ServiceCalendarFrame_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceCalendarFrameParser.class);

  private final Collection<DayType> dayTypes = new ArrayList<>();
  private final Collection<OperatingPeriod_VersionStructure> operatingPeriods = new ArrayList<>();
  private final Collection<OperatingDay> operatingDays = new ArrayList<>();
  private final Multimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId =
    ArrayListMultimap.create();

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
    if (serviceCalendar == null) {
      return;
    }

    parseDayTypes(serviceCalendar.getDayTypes());
    parseOperatingPeriods(serviceCalendar.getOperatingPeriods());
    parseDayTypeAssignments(serviceCalendar.getDayTypeAssignments());
  }

  //List<JAXBElement<? extends DataManagedObjectStructure>>
  private void parseDayTypes(DayTypesInFrame_RelStructure element) {
    if (element == null) {
      return;
    }
    for (JAXBElement<?> dt : element.getDayType()) {
      parseDayType(dt);
    }
  }

  private void parseDayTypes(DayTypes_RelStructure dayTypes) {
    if (dayTypes == null) {
      return;
    }
    for (JAXBElement<?> dt : dayTypes.getDayTypeRefOrDayType()) {
      parseDayType(dt);
    }
  }

  private void parseDayType(JAXBElement<?> dt) {
    if (dt.getValue() instanceof DayType) {
      dayTypes.add((DayType) dt.getValue());
    }
  }

  private void parseOperatingPeriods(OperatingPeriodsInFrame_RelStructure operatingPeriods) {
    if (operatingPeriods == null) {
      return;
    }

    for (OperatingPeriod_VersionStructure p : operatingPeriods.getOperatingPeriodOrUicOperatingPeriod()) {
      parseOperatingPeriod(p);
    }
  }

  private void parseOperatingPeriods(OperatingPeriods_RelStructure operatingPeriods) {
    if (operatingPeriods == null) {
      return;
    }
    JAXBUtils.forEachJAXBElementValue(
      Object.class,
      operatingPeriods.getOperatingPeriodRefOrOperatingPeriodOrUicOperatingPeriod(),
      this::parseOperatingPeriod
    );
  }

  private void parseOperatingPeriod(Object operatingPeriod) {
    if (operatingPeriod instanceof OperatingPeriod_VersionStructure op) {
      operatingPeriods.add(op);
    } else {
      NetexParser.warnOnMissingMapping(LOG, operatingPeriod);
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
