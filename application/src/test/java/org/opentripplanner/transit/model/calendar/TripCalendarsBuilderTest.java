package org.opentripplanner.transit.model.calendar;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.FeedInfoTestFactory;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class TripCalendarsBuilderTest {

  private static final FeedScopedId SERVICE_ALLDAYS_ID = id("alldays");

  private static final FeedScopedId SERVICE_WEEKDAYS_ID = id("weekdays");

  private static final LocalDate A_FRIDAY = LocalDate.of(2009, 1, 2);

  private static final LocalDate A_SUNDAY = LocalDate.of(2009, 1, 4);

  private static final LocalDate A_MONDAY = LocalDate.of(2009, 1, 5);

  private static TripCalendars tripCalendars;

  @BeforeAll
  public static void setup() throws IOException {
    // The context builder uses the TripCalendarsBuilder to create data
    tripCalendars = createCtxBuilder().getTripCalendars();
  }

  @Test
  public void testListServiceIds() {
    assertEquals("[F:alldays, F:weekdays]", toString(tripCalendars.listServiceIds()));
  }

  @Test
  public void testListServiceDates() {
    Set<LocalDate> alldays = tripCalendars.listServiceDates(SERVICE_ALLDAYS_ID);
    assertTrue(alldays.contains(A_FRIDAY));
    assertTrue(alldays.contains(A_SUNDAY));
    assertEquals(
      "[20090101, 20090102, 20090103, 20090104, 20090106, 20090107, 20090108]",
      sevenFirstDays(alldays).toString()
    );
    assertEquals(14975, alldays.size());

    Set<LocalDate> weekdays = tripCalendars.listServiceDates(SERVICE_WEEKDAYS_ID);
    assertTrue(weekdays.contains(A_FRIDAY));
    assertEquals(
      "[20090101, 20090102, 20090105, 20090106, 20090107, 20090108, 20090109]",
      sevenFirstDays(weekdays).toString()
    );
    assertEquals(10697, weekdays.size());
  }

  @Test
  public void testListServiceIdsOnServiceDate() {
    Set<FeedScopedId> servicesOnFriday = tripCalendars.listServiceIdsOnServiceDate(A_FRIDAY);
    assertEquals("[F:alldays, F:weekdays]", sort(servicesOnFriday).toString());

    Set<FeedScopedId> servicesOnSunday = tripCalendars.listServiceIdsOnServiceDate(A_SUNDAY);
    assertEquals("[F:alldays]", servicesOnSunday.toString());

    // Test exclusion of serviceCalendarDate
    Set<FeedScopedId> servicesOnMonday = tripCalendars.listServiceIdsOnServiceDate(A_MONDAY);
    assertEquals("[F:weekdays]", servicesOnMonday.toString());
  }

  @Test
  public void addWeeklyCalendarTwiceForSameServiceIdThrowsEvenWhenFirstCalendarIsOutsidePeriodLimit() {
    // The period limit excludes the first calendar's period entirely, so it is never stored -
    // but a service id may still only have one weekly calendar registered against it.
    LocalDateRange periodLimit = LocalDateRange.ofInclusiveEnd(A_MONDAY, A_MONDAY);
    TripCalendarsBuilder subject = TripCalendars.of(periodLimit);
    FeedScopedId serviceId = id("duplicate");

    subject.addWeeklyCalendar(
      serviceId,
      EnumSet.allOf(DayOfWeek.class),
      LocalDateRange.ofInclusiveEnd(A_FRIDAY, A_FRIDAY)
    );

    assertThrows(MultipleCalendarsForServiceIdException.class, () ->
      subject.addWeeklyCalendar(
        serviceId,
        EnumSet.allOf(DayOfWeek.class),
        LocalDateRange.ofInclusiveEnd(A_MONDAY, A_MONDAY)
      )
    );
  }

  private static GtfsContext createCtxBuilder() throws IOException {
    GtfsContextBuilder ctxBuilder = contextBuilder(
      TransitRepositoryForTest.FEED_ID,
      ConstantsForTests.SIMPLE_GTFS
    );
    TransitDataImportBuilder builder = ctxBuilder
      .withDataImportIssueStore(DataImportIssueStore.NOOP)
      .getTransitBuilder();

    // Supplement test data with at least one entity in all collections
    builder.tripCalendars().removeServiceDate(SERVICE_ALLDAYS_ID, LocalDate.of(2009, 1, 5));
    builder.getFeedInfos().add(FeedInfoTestFactory.dummyForTest(TransitRepositoryForTest.FEED_ID));

    return ctxBuilder.build();
  }

  private static <T> List<T> sort(Collection<? extends T> c) {
    return c.stream().sorted(comparing(T::toString)).collect(toList());
  }

  private static String toString(Collection<?> c) {
    return c.stream().sorted(comparing(Object::toString)).toList().toString();
  }

  private static List<String> sevenFirstDays(Collection<LocalDate> dates) {
    return dates
      .stream()
      .sorted()
      .limit(7)
      .map(ServiceDateUtils::asCompactString)
      .collect(toList());
  }
}
