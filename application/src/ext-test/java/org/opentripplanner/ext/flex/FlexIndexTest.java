package org.opentripplanner.ext.flex;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.model.FlexStopTimesFactory.area;
import static org.opentripplanner.model.FlexStopTimesFactory.groupStop;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.trip;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TimetableRepository;

class FlexIndexTest {

  public static final Route ROUTE_2 = TimetableRepositoryForTest.route("r2").build();

  @Test
  void testFlexTripSpanningMidnight() {
    TimetableRepository repo = new TimetableRepository();

    FeedScopedId serviceId = id("S1");
    Trip trip = trip("T1").withServiceId(serviceId).build();

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
    assertEquals(nextDay, ftfd1.endOfRunningPeriod());
    assertEquals(flexTrip, ftfd1.flexTrip());

    Collection<FlexTripForDate> tripsOnNextDay = index.getFlexTripsForRunningDate(nextDay);
    assertEquals(1, tripsOnNextDay.size(), "Should have 1 trip on next day");
    FlexTripForDate ftfd2 = tripsOnNextDay.iterator().next();
    assertEquals(serviceDate, ftfd2.serviceDate());
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
    Trip trip = trip("T2").withServiceId(serviceId).build();

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

    Collection<FlexTripForDate> tripsOnServiceDay = index.getFlexTripsForRunningDate(serviceDate);
    assertEquals(1, tripsOnServiceDay.size(), "Should have 1 trip on service day");
    assertEquals(serviceDate, tripsOnServiceDay.iterator().next().serviceDate());

    LocalDate nextDay = serviceDate.plusDays(1);
    Collection<FlexTripForDate> tripsOnNextDay = index.getFlexTripsForRunningDate(nextDay);
    assertEquals(1, tripsOnNextDay.size(), "Should have 1 trip on next day");
    assertEquals(serviceDate, tripsOnNextDay.iterator().next().serviceDate());
  }

  @Test
  void routesAtArea() {
    var repo = new TimetableRepository();

    var st1 = area("10:00", "12:00");
    var st2 = area("14:00", "16:00");

    var flexTrip = UnscheduledTrip.of(id("T2"))
      .withTrip(trip("T2").withRoute(ROUTE_2).build())
      .withStopTimes(List.of(st1, st2))
      .build();

    repo.addFlexTrip(flexTrip.getId(), flexTrip);

    var index = new FlexIndex(repo);

    assertThat(index.findRoutes(st1.getStop())).containsExactly(ROUTE_2);
    assertThat(index.findRoutes(st2.getStop())).containsExactly(ROUTE_2);
  }

  @Test
  void routesAtGroup() {
    var repo = new TimetableRepository();

    var st1 = groupStop("10:00", "12:00");
    var st2 = groupStop("14:00", "16:00");

    var flexTrip = UnscheduledTrip.of(id("T2"))
      .withTrip(trip("T2").withRoute(ROUTE_2).build())
      .withStopTimes(List.of(st1, st2))
      .build();

    repo.addFlexTrip(flexTrip.getId(), flexTrip);

    var index = new FlexIndex(repo);

    var groupStop = (GroupStop) st1.getStop();
    assertThat(groupStop.getChildLocations()).isNotEmpty();
    groupStop
      .getChildLocations()
      .forEach(child -> {
        assertThat(index.findRoutes(child)).containsExactly(ROUTE_2);
        assertThat(index.findRoutes(child)).containsExactly(ROUTE_2);
      });
  }
}
