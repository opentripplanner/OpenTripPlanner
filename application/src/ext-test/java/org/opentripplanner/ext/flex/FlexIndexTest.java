package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.area;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TimetableRepository;

class FlexIndexTest {

  @Test
  void testFlexTripSpanningMidnight() {
    TimetableRepository repo = new TimetableRepository();

    FeedScopedId serviceId = id("S1");
    Trip trip = TimetableRepositoryForTest.trip("T1").withServiceId(serviceId).build();

    UnscheduledTrip flexTrip = UnscheduledTrip.of(id("FT1"))
      .withTrip(trip)
      .withStopTimes(List.of(area("22:00", "26:00"), area("22:00", "26:00")))
      .build();

    repo.addFlexTrip(flexTrip.getId(), flexTrip);

    LocalDate serviceDate = LocalDate.of(2025, 2, 28);
    LocalDate nextDay = serviceDate.plusDays(1);
    CalendarServiceData calendarData = new CalendarServiceData();
    calendarData.putServiceDatesForServiceId(serviceId, List.of(serviceDate));
    repo.updateCalendarServiceData(calendarData);

    FlexIndex index = new FlexIndex(repo);

    Collection<FlexTripForDate> tripsOnServiceDate = index.getFlexTripsForRunningDate(serviceDate);
    assertEquals(1, tripsOnServiceDate.size(), "Should have 1 trip on service date");
    FlexTripForDate ftfd1 = tripsOnServiceDate.iterator().next();
    assertEquals(serviceDate, ftfd1.serviceDate());
    assertEquals(serviceDate, ftfd1.startOfRunningPeriod());
    assertEquals(nextDay, ftfd1.endOfRunningPeriod());
    assertEquals(flexTrip, ftfd1.flexTrip());

    Collection<FlexTripForDate> tripsOnNextDay = index.getFlexTripsForRunningDate(nextDay);
    assertEquals(1, tripsOnNextDay.size(), "Should have 1 trip on next day");
    FlexTripForDate ftfd2 = tripsOnNextDay.iterator().next();
    assertEquals(serviceDate, ftfd2.serviceDate());
    assertEquals(serviceDate, ftfd2.startOfRunningPeriod());
    assertEquals(nextDay, ftfd2.endOfRunningPeriod());
    assertEquals(flexTrip, ftfd2.flexTrip());

    LocalDate dayAfterNext = serviceDate.plusDays(2);
    assertTrue(
      index.getFlexTripsForRunningDate(dayAfterNext).isEmpty(),
      "Should have no trips on day after next"
    );
  }

  @Test
  void testFlexTripStartingAfterMidnight() {
    TimetableRepository repo = new TimetableRepository();
    FeedScopedId serviceId = id("S2");
    Trip trip = TimetableRepositoryForTest.trip("T2").withServiceId(serviceId).build();

    UnscheduledTrip flexTrip = UnscheduledTrip.of(id("FT2"))
      .withTrip(trip)
      .withStopTimes(List.of(area("25:00", "27:00"), area("25:00", "27:00")))
      .build();

    repo.addFlexTrip(flexTrip.getId(), flexTrip);

    LocalDate serviceDate = LocalDate.of(2025, 2, 28);
    CalendarServiceData calendarData = new CalendarServiceData();
    calendarData.putServiceDatesForServiceId(serviceId, List.of(serviceDate));
    repo.updateCalendarServiceData(calendarData);

    FlexIndex index = new FlexIndex(repo);

    assertTrue(
      index.getFlexTripsForRunningDate(serviceDate).isEmpty(),
      "Should have no trips on service date"
    );

    LocalDate nextDay = serviceDate.plusDays(1);
    Collection<FlexTripForDate> tripsOnNextDay = index.getFlexTripsForRunningDate(nextDay);
    assertEquals(1, tripsOnNextDay.size(), "Should have 1 trip on next day");
    assertEquals(serviceDate, tripsOnNextDay.iterator().next().serviceDate());
  }
}
