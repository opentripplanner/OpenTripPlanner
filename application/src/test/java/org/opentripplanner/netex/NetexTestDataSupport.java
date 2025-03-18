package org.opentripplanner.netex;

import jakarta.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.JourneyRefStructure;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriodRefStructure;
import org.rutebanken.netex.model.PropertiesOfDay_RelStructure;
import org.rutebanken.netex.model.PropertyOfDay;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.UicOperatingPeriod;

public final class NetexTestDataSupport {

  public static final String QUAY_ID = "TEST_QUAY_ID";
  public static final String QUAY_NAME = "TEST_QUAY_NAME";
  public static final String QUAY_VERSION = "TEST_QUAY_VERSION";
  public static final double QUAY_LAT = 0.1;
  public static final double QUAY_LON = 0.2;
  public static final String QUAY_PLATFORM_CODE = "TEST_QUAY_PLATFORM_CODE";

  public static final String STOP_PLACE_ID = "TEST_STOP_PLACE_ID";
  public static final String STOP_PLACE_NAME = "TEST_STOP_PLACE_NAME";
  public static final String STOP_PLACE_VERSION = "TEST_STOP_PLACE_VERSION";
  public static final double STOP_PLACE_LAT = 0.11;
  public static final double STOP_PLACE_LON = 0.22;
  public static final AllVehicleModesOfTransportEnumeration STOP_PLACE_TRANSPORT_MODE =
    AllVehicleModesOfTransportEnumeration.BUS;

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

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
    return createDayTypeAssignment(dayTypeId, isAvailable).withOperatingPeriodRef(
      new ObjectFactory()
        .createOperatingPeriodRef(new OperatingPeriodRefStructure().withRef(opPeriodId))
    );
  }

  public static DayTypeAssignment createDayTypeAssignmentWithOpDay(
    String dayTypeId,
    String opDayId,
    Boolean isAvailable
  ) {
    return createDayTypeAssignment(dayTypeId, isAvailable).withOperatingDayRef(
      new OperatingDayRefStructure().withRef(opDayId)
    );
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

  public static OperatingPeriod createOperatingPeriodWithOperatingDays(
    String id,
    String fromOperatingDayId,
    String toOperatingDayId
  ) {
    return new OperatingPeriod()
      .withId(id)
      .withFromOperatingDayRef(operatingDayRef(fromOperatingDayId))
      .withToOperatingDayRef(operatingDayRef(toOperatingDayId));
  }

  public static UicOperatingPeriod createUicOperatingPeriod(
    String id,
    LocalDate fromDate,
    LocalDate toDate,
    String validDayBits
  ) {
    return new UicOperatingPeriod()
      .withId(id)
      .withFromDate(createLocalDateTime(fromDate))
      .withToDate(createLocalDateTime(toDate))
      .withValidDayBits(validDayBits);
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

  public static OperatingDayRefStructure operatingDayRef(String id) {
    return new OperatingDayRefStructure().withRef(id);
  }

  public static StopPlace createStopPlace(
    String id,
    String name,
    String version,
    Double lat,
    Double lon,
    AllVehicleModesOfTransportEnumeration transportMode,
    Quay quay
  ) {
    StopPlace stopPlace = new StopPlace()
      .withName(createMLString(name))
      .withVersion(version)
      .withId(id)
      .withCentroid(createSimplePoint(lat, lon))
      .withTransportMode(transportMode);

    if (quay != null) {
      Collection<JAXBElement<?>> jaxbQuays = List.of(OBJECT_FACTORY.createQuay(quay));
      Quays_RelStructure quays = OBJECT_FACTORY.createQuays_RelStructure()
        .withQuayRefOrQuay(jaxbQuays);
      stopPlace.withQuays(quays);
    }

    return stopPlace;
  }

  public static StopPlace createStopPlace(
    String id,
    String name,
    String version,
    Double lat,
    Double lon,
    AllVehicleModesOfTransportEnumeration transportMode
  ) {
    return createStopPlace(id, name, version, lat, lon, transportMode, null);
  }

  public static StopPlace createStopPlace(Quay quay) {
    return createStopPlace(
      STOP_PLACE_ID,
      STOP_PLACE_NAME,
      STOP_PLACE_VERSION,
      STOP_PLACE_LAT,
      STOP_PLACE_LON,
      STOP_PLACE_TRANSPORT_MODE,
      quay
    );
  }

  public static StopPlace createStopPlace() {
    return createStopPlace(null);
  }

  public static Quay createQuay(
    String id,
    String name,
    String version,
    Double lat,
    Double lon,
    String platformCode
  ) {
    return new Quay()
      .withName(createMLString(name))
      .withId(id)
      .withVersion(version)
      .withPublicCode(platformCode)
      .withCentroid(createSimplePoint(lat, lon));
  }

  public static Quay createQuay() {
    return createQuay(QUAY_ID, QUAY_NAME, QUAY_VERSION, QUAY_LAT, QUAY_LON, QUAY_PLATFORM_CODE);
  }

  private static MultilingualString createMLString(String name) {
    return new MultilingualString().withValue(name);
  }

  private static SimplePoint_VersionStructure createSimplePoint(Double lat, Double lon) {
    return new SimplePoint_VersionStructure()
      .withLocation(
        new LocationStructure().withLatitude(new BigDecimal(lat)).withLongitude(new BigDecimal(lon))
      );
  }
}
