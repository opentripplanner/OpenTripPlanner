package org.opentripplanner.model.calendar.impl;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;
import static org.opentripplanner.model.calendar.ServiceCalendarDate.EXCEPTION_TYPE_REMOVE;
import static org.opentripplanner.model.calendar.impl.CalendarServiceDataFactoryImpl.merge;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (08.11.2017)
 */
public class CalendarServiceDataFactoryImplTest {

  private static final FeedScopedId SERVICE_ALLDAYS_ID = id("alldays");

  private static final FeedScopedId SERVICE_WEEKDAYS_ID = id("weekdays");

  private static final LocalDate A_FRIDAY = LocalDate.of(2009, 1, 2);

  private static final LocalDate A_SUNDAY = LocalDate.of(2009, 1, 4);

  private static final LocalDate A_MONDAY = LocalDate.of(2009, 1, 5);

  private static CalendarServiceData data;

  private static CalendarService calendarService;

  @BeforeAll
  public static void setup() throws IOException {
    // The context builder uses the CalendarServiceDataFactoryImpl to create data
    data = createCtxBuilder().getCalendarServiceData();
    calendarService = new CalendarServiceImpl(data);
  }

  @Test
  public void testMerge() {
    Set<Character> result = merge(asList('A', 'B'), asList('B', 'C'));

    assertTrue(result.containsAll(asList('A', 'B', 'C')), result.toString());
    assertEquals(3, result.size());
  }

  @Test
  public void testDataGetServiceIds() {
    assertEquals("[F:alldays, F:weekdays]", toString(data.getServiceIds()));
  }

  @Test
  public void testDataGetServiceDatesForServiceId() {
    List<LocalDate> alldays = data.getServiceDatesForServiceId(SERVICE_ALLDAYS_ID);
    assertEquals(
      "[20090101, 20090102, 20090103, 20090104, 20090106, 20090107, 20090108]",
      sevenFirstDays(alldays).toString()
    );
    assertEquals(14975, alldays.size());

    List<LocalDate> weekdays = data.getServiceDatesForServiceId(SERVICE_WEEKDAYS_ID);
    assertEquals(
      "[20090101, 20090102, 20090105, 20090106, 20090107, 20090108, 20090109]",
      sevenFirstDays(weekdays).toString()
    );
    assertEquals(10697, weekdays.size());
  }

  @Test
  public void testServiceGetServiceIdsOnDate() {
    Set<FeedScopedId> servicesOnFriday = calendarService.getServiceIdsOnDate(A_FRIDAY);
    assertEquals("[F:alldays, F:weekdays]", sort(servicesOnFriday).toString());

    Set<FeedScopedId> servicesOnSunday = calendarService.getServiceIdsOnDate(A_SUNDAY);
    assertEquals("[F:alldays]", servicesOnSunday.toString());

    // Test exclusion of serviceCalendarDate
    Set<FeedScopedId> servicesOnMonday = calendarService.getServiceIdsOnDate(A_MONDAY);
    assertEquals("[F:weekdays]", servicesOnMonday.toString());
  }

  @Test
  public void testServiceGetServiceIds() {
    Set<FeedScopedId> serviceIds = calendarService.getServiceIds();
    assertEquals("[F:alldays, F:weekdays]", sort(serviceIds).toString());
  }

  @Test
  public void testServiceGetServiceDatesForServiceId() {
    Set<LocalDate> alldays = calendarService.getServiceDatesForServiceId(SERVICE_ALLDAYS_ID);

    assertTrue(alldays.contains(A_FRIDAY));
    assertTrue(alldays.contains(A_SUNDAY));
    assertEquals(14975, alldays.size());

    Set<LocalDate> weekdays = calendarService.getServiceDatesForServiceId(SERVICE_WEEKDAYS_ID);

    assertTrue(weekdays.contains(A_FRIDAY));
    Assertions.assertFalse(weekdays.contains(A_SUNDAY));
    assertEquals(10697, weekdays.size());
  }

  private static GtfsContext createCtxBuilder() throws IOException {
    GtfsContextBuilder ctxBuilder = contextBuilder(
      TransitModelForTest.FEED_ID,
      ConstantsForTests.SIMPLE_GTFS
    );
    OtpTransitServiceBuilder builder = ctxBuilder
      .withDataImportIssueStore(DataImportIssueStore.NOOP)
      .getTransitBuilder();

    // Supplement test data with at least one entity in all collections
    builder.getCalendarDates().add(removeMondayFromAlldays());
    builder.getFeedInfos().add(FeedInfo.dummyForTest(TransitModelForTest.FEED_ID));

    return ctxBuilder.build();
  }

  private static ServiceCalendarDate removeMondayFromAlldays() {
    return new ServiceCalendarDate(
      SERVICE_ALLDAYS_ID,
      LocalDate.of(2009, 1, 5),
      EXCEPTION_TYPE_REMOVE
    );
  }

  private static <T> List<T> sort(Collection<? extends T> c) {
    return c.stream().sorted(comparing(T::toString)).collect(toList());
  }

  private static String toString(Collection<?> c) {
    return c.stream().sorted(comparing(Object::toString)).toList().toString();
  }

  private static List<String> sevenFirstDays(List<LocalDate> dates) {
    return dates.stream().limit(7).map(ServiceDateUtils::asCompactString).collect(toList());
  }
}
