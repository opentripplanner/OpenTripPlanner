package org.opentripplanner.netex.mapping.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.FEED_ID;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.calendar.TripCalendars;

public class CalendarServiceBuilderTest {

  private static final LocalDate D1 = LocalDate.of(2020, 11, 1);
  private static final LocalDate D2 = LocalDate.of(2020, 11, 2);

  private static final FeedScopedId EXP_SID_1 = FeedScopedIdForTestFactory.id("S000001");
  private static final FeedScopedId EXP_SID_2 = FeedScopedIdForTestFactory.id("S000002");
  private static final FeedScopedId EXP_SID_3 = FeedScopedIdForTestFactory.id("S000003");

  @Test
  public void addDatesForAGivenService() {
    CalendarServiceBuilder subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));

    assertEquals(
      CalendarServiceBuilder.EMPTY_SERVICE_ID,
      subject.registerDatesAndGetServiceId(Set.of())
    );
    assertEquals(EXP_SID_1, subject.registerDatesAndGetServiceId(Set.of(D1)));
    assertEquals(EXP_SID_2, subject.registerDatesAndGetServiceId(Set.of(D2)));
    assertEquals(EXP_SID_3, subject.registerDatesAndGetServiceId(Set.of(D1, D2)));

    assertEquals(EXP_SID_1, subject.registerDatesAndGetServiceId(Set.of(D1)));
    assertEquals(EXP_SID_3, subject.registerDatesAndGetServiceId(Set.of(D2, D1)));
    assertEquals(EXP_SID_3, subject.registerDatesAndGetServiceId(Set.of(D1, D2)));
  }

  @Test
  public void addServiceCalendarsTo() {
    // Given
    var subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));

    // with 3 sets of dates
    subject.registerDatesAndGetServiceId(Set.of(D1));
    subject.registerDatesAndGetServiceId(Set.of(D2));
    subject.registerDatesAndGetServiceId(Set.of(D2, D1));

    // When
    var calendars = TripCalendars.of();
    subject.addServiceCalendarsTo(calendars);
    var tripCalendars = calendars.build();

    // Then
    assertEquals(Set.of(D1), tripCalendars.listServiceDates(EXP_SID_1));
    assertEquals(Set.of(D2), tripCalendars.listServiceDates(EXP_SID_2));
    assertEquals(Set.of(D1, D2), tripCalendars.listServiceDates(EXP_SID_3));
  }

  @Test
  public void addServiceCalendarsToIsLimitedByPeriod() {
    // Given
    var subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));

    // A service running on D1 only, and one running on both D1 and D2
    subject.registerDatesAndGetServiceId(Set.of(D1));
    subject.registerDatesAndGetServiceId(Set.of(D1, D2));

    // When the calendars builder's period limit excludes D1
    var calendarsBuilder = TripCalendars.of(LocalDateRange.ofInclusiveEnd(D2, D2));
    subject.addServiceCalendarsTo(calendarsBuilder);
    var tripCalendars = calendarsBuilder.build();

    // Then the service that only ran on D1 is gone entirely, and the other one is trimmed to D2
    assertEquals(Set.of(), tripCalendars.listServiceIdsOnServiceDate(D1));
    assertEquals(Set.of(D2), tripCalendars.listServiceDates(EXP_SID_2));
    assertEquals(Set.of(EXP_SID_2), tripCalendars.listServiceIds());
  }

  @Test
  public void createServiceId() {
    CalendarServiceBuilder subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));
    assertEquals(FeedScopedIdForTestFactory.id("S000001"), subject.createServiceId());
    assertEquals(FeedScopedIdForTestFactory.id("S000002"), subject.createServiceId());
  }
}
