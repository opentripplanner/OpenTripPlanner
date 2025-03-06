package org.opentripplanner.netex.mapping;

import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayType;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayTypeAssignment;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayTypeRefList;
import static org.opentripplanner.netex.NetexTestDataSupport.jaxbElement;

import com.google.common.collect.ArrayListMultimap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMultimap;
import org.opentripplanner.netex.mapping.calendar.CalendarServiceBuilder;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.JourneyRefStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;

public class TripCalendarBuilderTest {

  private static final String FEED_ID = "F";

  private static final LocalDate D2020_11_01 = LocalDate.of(2020, 11, 1);
  private static final LocalDate D2020_11_02 = LocalDate.of(2020, 11, 2);

  private static final LocalTime ANY_TIME = LocalTime.of(12, 0);

  private static final String DAY_TYPE_1 = "DT-1";

  private static final Boolean AVAILABLE = TRUE;

  private static final String SJ_1 = "SJ-1";
  private static final String SJ_2 = "SJ-2";
  private static final String SJ_3 = "SJ-3";

  private static final HierarchicalMapById<OperatingDay> EMPTY_OPERATING_DAYS =
    new HierarchicalMapById<>();
  private static final HierarchicalMapById<OperatingPeriod_VersionStructure> EMPTY_PERIODS =
    new HierarchicalMapById<>();
  public static final String OD_1 = "OD-1";

  private final DataImportIssueStore issueStore = DataImportIssueStore.NOOP;
  private final CalendarServiceBuilder calendarServiceBuilder = new CalendarServiceBuilder(
    new FeedScopedIdFactory(FEED_ID)
  );

  private final TripCalendarBuilder subject = new TripCalendarBuilder(
    calendarServiceBuilder,
    issueStore
  );

  @Test
  public void mapDayTypesToLocalDatesForAGivenDate() {
    // GIVEN a calendar
    subject.addDayTypeAssignments(
      dayType1(),
      dayType1Assignment_2020_11_01(),
      EMPTY_OPERATING_DAYS,
      EMPTY_PERIODS
    );
    subject.addDatedServiceJourneys(operatingDays_2020_11_02(), dsj_2020_11_02(SJ_2));
    subject.addDatedServiceJourneys(operatingDays_2020_11_02(), dsj_2020_11_02(SJ_3));

    // Push calendar data before getting serviceIds, this make sure the calendar data is
    // available even if pushed
    subject.push();

    var result = subject.createTripCalendar(
      List.of(
        new ServiceJourney().withId(SJ_1).withDayTypes(createDayTypeRefList(DAY_TYPE_1)),
        new ServiceJourney().withId(SJ_2).withDayTypes(createDayTypeRefList(DAY_TYPE_1)),
        new ServiceJourney().withId(SJ_3)
      )
    );

    // Pop calendar data before creating ServiceCalendarDates to verify no data is lost in the
    // pop() operation
    subject.pop();

    var calendar = calendarServiceBuilder.createServiceCalendar();

    assertEquals(3, result.size());
    var serviceId1 = result.get(SJ_1);
    var serviceId2 = result.get(SJ_2);
    var serviceId3 = result.get(SJ_3);

    assertNotNull(serviceId1);
    assertNotNull(serviceId2);
    assertNotNull(serviceId3);

    assertEquals(4, calendar.size());
    assertEquals("[2020-11-01]", listDates(calendar, serviceId1));
    assertEquals("[2020-11-01, 2020-11-02]", listDates(calendar, serviceId2));
    assertEquals("[2020-11-02]", listDates(calendar, serviceId3));
  }

  /* private helper methods */

  private static String listDates(
    Collection<ServiceCalendarDate> calendar,
    FeedScopedId serviceId
  ) {
    return calendar
      .stream()
      .filter(it -> serviceId.equals(it.getServiceId()))
      .map(ServiceCalendarDate::getDate)
      .sorted()
      .collect(Collectors.toList())
      .toString();
  }

  private static HierarchicalMapById<OperatingDay> operatingDays_2020_11_02() {
    HierarchicalMapById<OperatingDay> opDays = new HierarchicalMapById<>();
    opDays.add(
      new OperatingDay().withId(OD_1).withCalendarDate(LocalDateTime.of(D2020_11_02, ANY_TIME))
    );
    return opDays;
  }

  private static HierarchicalMapById<DayType> dayType1() {
    var dayTypes = new HierarchicalMapById<DayType>();
    dayTypes.add(createDayType(DAY_TYPE_1));
    return dayTypes;
  }

  private static HierarchicalMultimap<String, DayTypeAssignment> dayType1Assignment_2020_11_01() {
    var assignments = new HierarchicalMultimap<String, DayTypeAssignment>();
    // Simple assignments on 1.11.2020
    assignments.add(DAY_TYPE_1, createDayTypeAssignment(DAY_TYPE_1, D2020_11_01, AVAILABLE));
    return assignments;
  }

  private ArrayListMultimap<String, DatedServiceJourney> dsj_2020_11_02(String sjId) {
    var dsj = new DatedServiceJourney();
    dsj.withOperatingDayRef(new OperatingDayRefStructure().withRef(OD_1));
    dsj
      .getJourneyRef()
      .add(jaxbElement(new JourneyRefStructure().withRef(sjId), JourneyRefStructure.class));
    ArrayListMultimap<String, DatedServiceJourney> map = ArrayListMultimap.create();
    map.put(sjId, dsj);
    return map;
  }

  private String toStr(Map<String, Set<LocalDate>> result, String key) {
    return result.get(key).stream().sorted().collect(Collectors.toList()).toString();
  }
}
