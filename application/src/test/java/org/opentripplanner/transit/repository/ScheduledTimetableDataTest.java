package org.opentripplanner.transit.repository;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;

class ScheduledTimetableDataTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop STOP_1 = TEST_MODEL.stop("S1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("S2").build();
  private static final Route ROUTE = TransitRepositoryForTest.route("R1").build();
  private static final LocalDate SERVICE_DATE = LocalDate.of(2025, 2, 28);

  @Test
  void empty() {
    var subject = ScheduledTimetableData.empty();
    assertNull(subject.getTripForId(id("T1")));
    assertThat(subject.getAllRoutes()).isEmpty();
    assertTrue(subject.getTripCalendars().isEmpty());
  }

  @Test
  void lookupsOverTheBuildData() {
    var buildRepository = new TimetableBuildRepository();
    var trip1 = trip("T1");
    var trip2 = trip("T2");
    var pattern1 = pattern("P1", trip1);
    var pattern2 = pattern("P2", trip2);
    buildRepository.addTripPattern(pattern1.getId(), pattern1);
    buildRepository.addTripPattern(pattern2.getId(), pattern2);

    var subject = buildRepository.toScheduledTimetableData();

    assertThat(subject.getAllTrips()).containsExactly(trip1, trip2);
    assertThat(subject.getTripForId(trip2.getId())).isEqualTo(trip2);
    assertThat(subject.getPatternForTrip(trip2)).isEqualTo(pattern2);
    assertThat(subject.getTripPatternForId(pattern1.getId())).isEqualTo(pattern1);
    assertThat(subject.getPatternsForRoute(ROUTE)).containsExactly(pattern1, pattern2);
    assertThat(subject.getPatternsForStop(STOP_1)).containsExactly(pattern1, pattern2);
    assertThat(subject.getRoutesForStop(STOP_2)).containsExactly(ROUTE);
    assertThat(subject.getRouteForId(ROUTE.getId())).isEqualTo(ROUTE);
  }

  @Test
  void laterChangesToTheBuildDataAreNotReflected() {
    var buildRepository = new TimetableBuildRepository();
    var pattern1 = pattern("P1", trip("T1"));
    buildRepository.addTripPattern(pattern1.getId(), pattern1);

    var subject = buildRepository.toScheduledTimetableData();

    var trip2 = trip("T2");
    var pattern2 = pattern("P2", trip2);
    buildRepository.addTripPattern(pattern2.getId(), pattern2);
    buildRepository.updateCalendarServiceData(calendarData());

    assertThat(subject.getAllTripPatterns()).containsExactly(pattern1);
    assertNull(subject.getTripForId(trip2.getId()));
    assertTrue(subject.getTripCalendars().isEmpty());
  }

  @Test
  void calendars() {
    var buildRepository = new TimetableBuildRepository();
    var pattern = pattern("P1", trip("T1"));
    buildRepository.addTripPattern(pattern.getId(), pattern);
    buildRepository.updateCalendarServiceData(calendarData());
    buildRepository.putServiceCode(id("SID"), 7);

    var subject = buildRepository.toScheduledTimetableData();

    var codes = subject.getTripCalendars().getServiceCodesRunningForDate().get(SERVICE_DATE);
    assertThat(codes.toArray()).asList().containsExactly(7);
    assertTrue(subject.hasScheduledServicesAfter(SERVICE_DATE, STOP_1));
    assertFalse(subject.hasScheduledServicesAfter(SERVICE_DATE.plusDays(1), STOP_1));
  }

  @Test
  void flexTripsAreOnlyIndexedWhenFlexRoutingIsOn() {
    var flexTrip = TEST_MODEL.unscheduledTrip("FT1", STOP_1, STOP_2);
    var flexTripId = flexTrip.getId();
    var flexRouteId = flexTrip.getTrip().getRoute().getId();
    var buildRepository = new TimetableBuildRepository();
    buildRepository.addFlexTrip(flexTrip.getId(), flexTrip);

    OTPFeature.FlexRouting.testOn(() -> {
      var subject = buildRepository.toScheduledTimetableData();
      assertThat(subject.getFlexIndex()).isNotNull();
      assertTrue(subject.containsTrip(flexTripId));
      assertThat(subject.getRouteForId(flexRouteId)).isNotNull();
    });

    OTPFeature.FlexRouting.testOff(() -> {
      var subject = buildRepository.toScheduledTimetableData();
      assertNull(subject.getFlexIndex());
      assertFalse(subject.containsTrip(flexTripId));
      assertNull(subject.getRouteForId(flexRouteId));
    });
  }

  private static Trip trip(String id) {
    return TransitRepositoryForTest.trip(id).withRoute(ROUTE).withServiceId(id("SID")).build();
  }

  private static TripPattern pattern(String id, Trip trip) {
    var tripTimes = ScheduledTripTimes.of().withArrivalTimes("10:00 10:05").withTrip(trip).build();
    return TransitRepositoryForTest.tripPattern(id, ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_1, STOP_2))
      .withScheduledTimeTableBuilder(ttb -> ttb.addTripTimes(tripTimes))
      .build();
  }

  private static CalendarServiceData calendarData() {
    var data = new CalendarServiceData();
    data.putServiceDatesForServiceId(id("SID"), List.of(SERVICE_DATE));
    return data;
  }
}
