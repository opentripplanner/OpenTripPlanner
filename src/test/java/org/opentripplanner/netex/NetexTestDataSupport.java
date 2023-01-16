package org.opentripplanner.netex;

import jakarta.xml.bind.JAXBElement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.JourneyRefStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriodRefStructure;
import org.rutebanken.netex.model.PropertiesOfDay_RelStructure;
import org.rutebanken.netex.model.PropertyOfDay;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public final class NetexTestDataSupport {

  /** Utility class, prevent instantiation. */
  private NetexTestDataSupport() {}

  /* XML TYPES */

  public static <T> JAXBElement<T> jaxbElement(T e, Class<T> clazz) {
    return new JAXBElement<>(new QName("x"), clazz, e);
  }

  /* JAVA TYPES*/

  @Nullable
  public static LocalDateTime createLocalDateTime(LocalDate day) {
    return day == null ? null : LocalDateTime.of(day, LocalTime.of(12, 0));
  }

  /* NETEX TYPES */

  public static OperatingDay createOperatingDay(String id, LocalDate day) {
    return new OperatingDay().withId(id).withCalendarDate(createLocalDateTime(day));
  }

  public static DayType createDayType(String id, DayOfWeekEnumeration... daysOfWeek) {
    DayType dayType = new DayType().withId(id);
    if (daysOfWeek != null && daysOfWeek.length > 0) {
      dayType.setProperties(
        new PropertiesOfDay_RelStructure()
          .withPropertyOfDay(new PropertyOfDay().withDaysOfWeek(daysOfWeek))
      );
    }
    return dayType;
  }

  public static DayTypeRefStructure createDayTypeRef(String id) {
    return new DayTypeRefStructure().withRef(id);
  }

  public static DayTypeRefs_RelStructure createDayTypeRefList(String... dayTypeIds) {
    var list = new DayTypeRefs_RelStructure();
    for (String it : dayTypeIds) {
      list.getDayTypeRef().add(jaxbElement(createDayTypeRef(it), DayTypeRefStructure.class));
    }
    return list;
  }

  public static DayTypeAssignment createDayTypeAssignment(String dayTypeId, Boolean isAvailable) {
    return new DayTypeAssignment()
      .withDayTypeRef(jaxbElement(createDayTypeRef(dayTypeId), DayTypeRefStructure.class))
      .withIsAvailable(isAvailable);
  }

  public static DayTypeAssignment createDayTypeAssignment(
    String dayTypeId,
    LocalDate date,
    Boolean isAvailable
  ) {
    return createDayTypeAssignment(dayTypeId, isAvailable).withDate(createLocalDateTime(date));
  }

  public static DayTypeAssignment createDayTypeAssignmentWithPeriod(
    String dayTypeId,
    String opPeriodId,
    Boolean isAvailable
  ) {
    return createDayTypeAssignment(dayTypeId, isAvailable)
      .withOperatingPeriodRef(new OperatingPeriodRefStructure().withRef(opPeriodId));
  }

  public static DayTypeAssignment createDayTypeAssignmentWithOpDay(
    String dayTypeId,
    String opDayId,
    Boolean isAvailable
  ) {
    return createDayTypeAssignment(dayTypeId, isAvailable)
      .withOperatingDayRef(new OperatingDayRefStructure().withRef(opDayId));
  }

  public static OperatingPeriod createOperatingPeriod(
    String id,
    LocalDate fromDate,
    LocalDate toDate
  ) {
    return new OperatingPeriod()
      .withId(id)
      .withFromDate(createLocalDateTime(fromDate))
      .withToDate(createLocalDateTime(toDate));
  }

  public static DatedServiceJourney createDatedServiceJourney(
    String id,
    String opDayId,
    String sjId
  ) {
    return createDatedServiceJourney(id, opDayId, sjId, null);
  }

  @SuppressWarnings("unchecked")
  public static DatedServiceJourney createDatedServiceJourney(
    String id,
    String opDayId,
    String sjId,
    ServiceAlterationEnumeration alt
  ) {
    var sjRef = jaxbElement(journeyRef(sjId), JourneyRefStructure.class);
    var odRef = new OperatingDayRefStructure().withRef(opDayId);
    return new DatedServiceJourney()
      .withId(id)
      .withJourneyRef(sjRef)
      .withOperatingDayRef(odRef)
      .withServiceAlteration(alt);
  }

  public static JourneyRefStructure journeyRef(String sjId) {
    return new JourneyRefStructure().withRef(sjId);
  }
}
