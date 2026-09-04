package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.calendar.TripCalendars;

public class ServiceCalendarMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final FeedScopedId SERVICE_ID = new FeedScopedId("A", "1");

  private static final Integer ID = 45;

  private static final int MONDAY = 1;

  private static final int TUESDAY = 0;

  private static final int WEDNESDAY = 1;

  private static final int THURSDAY = 0;

  private static final int FRIDAY = 1;

  private static final int SATURDAY = 0;

  private static final int SUNDAY = 1;

  private static final ServiceDate START_DATE = new ServiceDate(2017, 10, 17);

  private static final ServiceDate END_DATE = new ServiceDate(2018, 1, 2);

  private static final LocalDate PERIOD_START = LocalDate.of(2017, 10, 17);

  private static final LocalDate PERIOD_END = LocalDate.of(2018, 1, 2);

  private static final ServiceCalendar CALENDAR = new ServiceCalendar();
  private final ServiceCalendarMapper subject = new ServiceCalendarMapper(new IdFactory("A"));

  static {
    CALENDAR.setId(ID);
    CALENDAR.setServiceId(AGENCY_AND_ID);
    CALENDAR.setMonday(MONDAY);
    CALENDAR.setTuesday(TUESDAY);
    CALENDAR.setWednesday(WEDNESDAY);
    CALENDAR.setThursday(THURSDAY);
    CALENDAR.setFriday(FRIDAY);
    CALENDAR.setSaturday(SATURDAY);
    CALENDAR.setSunday(SUNDAY);
    CALENDAR.setStartDate(START_DATE);
    CALENDAR.setEndDate(END_DATE);
  }

  @Test
  public void testMapCollection() {
    var calendars = TripCalendars.of();
    subject.map((java.util.Collection<ServiceCalendar>) null, calendars);
    subject.map(Collections.emptyList(), calendars);
    assertTrue(calendars.listServiceIds().isEmpty());

    subject.map(Collections.singleton(CALENDAR), calendars);
    assertEquals(1, calendars.listServiceIds().size());
  }

  @Test
  public void testMap() {
    var calendars = TripCalendars.of();
    subject.map(CALENDAR, calendars);
    var built = calendars.build();

    Set<DayOfWeek> expectedDays = Set.of(
      DayOfWeek.MONDAY,
      DayOfWeek.WEDNESDAY,
      DayOfWeek.FRIDAY,
      DayOfWeek.SUNDAY
    );
    List<LocalDate> dates = built.listServiceDates(SERVICE_ID).stream().sorted().toList();
    assertTrue(dates.size() > 0);
    for (LocalDate date : dates) {
      assertTrue(expectedDays.contains(date.getDayOfWeek()), date.toString());
      assertFalse(date.isBefore(PERIOD_START), date.toString());
      assertFalse(date.isAfter(PERIOD_END), date.toString());
    }
  }

  @Test
  public void testMapWithNulls() {
    ServiceCalendar input = new ServiceCalendar();
    input.setServiceId(AGENCY_AND_ID);
    var calendars = TripCalendars.of();
    subject.map(input, calendars);
    var built = calendars.build();

    // No days of week are set, so no dates are active, but the service id is still registered.
    assertTrue(built.listServiceIds().contains(SERVICE_ID));
    assertTrue(built.listServiceDates(SERVICE_ID).isEmpty());
  }

  @Test
  public void testMapTwiceForSameServiceIdThrows() {
    var calendars = TripCalendars.of();
    subject.map(CALENDAR, calendars);
    assertThrows(RuntimeException.class, () -> subject.map(CALENDAR, calendars));
  }
}
