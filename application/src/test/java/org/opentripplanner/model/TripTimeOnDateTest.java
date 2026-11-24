package org.opentripplanner.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitDataTestFactory;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.repository.ScheduledTimetableData;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.utils.time.ServiceDateUtils;

class TripTimeOnDateTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final LocalDate DATE = LocalDate.of(2025, 3, 18);
  private static final Instant MIDNIGHT = ServiceDateUtils.asStartOfService(
    DATE,
    ZoneIds.BERLIN
  ).toInstant();

  @Test
  void gtfsSequence() {
    var pattern = TEST_MODEL.pattern(TransitMode.BUS).build();
    var trip = TransitRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, trip, "11:00");

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

    var subject = new TripTimeOnDate(tripTimes, 2, pattern);

    var seq = subject.getGtfsSequence();
    assertEquals(30, seq);

    var departure = LocalTime.ofSecondOfDay(subject.getScheduledDeparture());
    assertEquals(LocalTime.of(11, 10), departure);
  }

  @Test
  void hasArrivedStop() {
    var pattern = TEST_MODEL.pattern(TransitMode.BUS).build();
    var trip = TransitRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, trip, "11:00");

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator())
      .createRealTimeFromScheduledTimes()
      .withHasArrived(1, true)
      .build();

    var subject = new TripTimeOnDate(tripTimes, 0, pattern);
    assertFalse(subject.hasArrived());

    subject = new TripTimeOnDate(tripTimes, 1, pattern);
    assertTrue(subject.hasArrived());
  }

  @Test
  void hasDepartedStop() {
    var pattern = TEST_MODEL.pattern(TransitMode.BUS).build();
    var trip = TransitRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, trip, "11:00");

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator())
      .createRealTimeFromScheduledTimes()
      .withHasDeparted(1, true)
      .build();

    var subject = new TripTimeOnDate(tripTimes, 0, pattern);
    assertFalse(subject.hasDeparted());

    subject = new TripTimeOnDate(tripTimes, 1, pattern);
    assertTrue(subject.hasDeparted());
  }

  @Test
  void previousTimes() {
    var subject = tripTimeOnDate();

    var ids = subject
      .previousTimes()
      .stream()
      .map(t -> t.getStop().getId().toString())
      .toList();
    assertEquals(List.of("F:stop-10", "F:stop-20"), ids);
    assertThat(subject.previousTimes().getFirst().previousTimes()).isEmpty();
  }

  @Test
  void previousTimesWithCount() {
    var subject = tripTimeOnDate();
    assertTrue(mapTripTimeOnDateToStopId(subject.previousTimes(0)).isEmpty());
    assertEquals(List.of("F:stop-20"), mapTripTimeOnDateToStopId(subject.previousTimes(1)));
    assertEquals(
      List.of("F:stop-10", "F:stop-20"),
      mapTripTimeOnDateToStopId(subject.previousTimes(2))
    );
    assertEquals(
      List.of("F:stop-10", "F:stop-20"),
      mapTripTimeOnDateToStopId(subject.previousTimes(3))
    );
  }

  @Test
  void nextTimes() {
    var subject = tripTimeOnDate();
    var ids = subject
      .nextTimes()
      .stream()
      .map(t -> t.getStop().getId().toString())
      .toList();
    assertEquals(List.of("F:stop-40", "F:stop-50"), ids);
    var secondLast = subject.nextTimes().getFirst();
    var lastStop = secondLast
      .nextTimes()
      .stream()
      .map(t -> t.getStop().getId().toString())
      .toList();
    assertEquals(List.of("F:stop-50"), lastStop);
    assertThat(secondLast.nextTimes().getFirst().nextTimes()).isEmpty();
  }

  @Test
  void nextTimesWithCount() {
    var subject = tripTimeOnDate();
    assertTrue(mapTripTimeOnDateToStopId(subject.nextTimes(0)).isEmpty());
    assertEquals(List.of("F:stop-40"), mapTripTimeOnDateToStopId(subject.nextTimes(1)));
    assertEquals(
      List.of("F:stop-40", "F:stop-50"),
      mapTripTimeOnDateToStopId(subject.nextTimes(2))
    );
    assertEquals(
      List.of("F:stop-40", "F:stop-50"),
      mapTripTimeOnDateToStopId(subject.nextTimes(3))
    );
  }

  @Test
  void atZone() {
    var subject = tripTimeOnDate();
    var departure = subject.scheduledDeparture();
    assertEquals(
      "2025-03-18T11:10+01:00",
      departure.atZone(ZoneIds.BERLIN).toOffsetDateTime().toString()
    );
  }

  private static TripTimeOnDate tripTimeOnDate() {
    var trip = TransitRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(5, trip, "11:00");
    var stops = stopTimes.stream().map(StopTime::getStop).toList();
    var pattern = TEST_MODEL.pattern(TransitMode.BUS)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stops))
      .build();
    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    return new TripTimeOnDate(tripTimes, 2, pattern, DATE, MIDNIGHT);
  }

  @Test
  void testFromTripTimesWithScheduleFallback() {
    var testModel = TransitRepositoryForTest.of();
    var trip = TransitRepositoryForTest.trip("123").build();
    var siteRepository = testModel.siteRepositoryBuilder().build();
    var transitRepository = new TransitRepository(siteRepository);
    var timetableDataImport = new TimetableDataImport();
    var tripTimes = ScheduledTripTimes.of()
      .withTrip(trip)
      .withDepartureTimes(new int[] { 0, 1 })
      .build();
    var tripPattern = testModel
      .pattern(TransitMode.BUS)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();
    timetableDataImport.addTripPattern(tripPattern.getId(), tripPattern);
    transitRepository.index();
    var timetableSnapshot = new DefaultTimetableRepository(
      RaptorTransitDataTestFactory.empty(),
      ScheduledTimetableData.empty()
    );
    timetableSnapshot.commit();
    var serviceDate = LocalDate.of(2025, 1, 1);
    // Construct a timetable which definitely does not contain this trip, because it is empty.
    Timetable timetable = Timetable.of()
      .withTripPattern(tripPattern)
      .withServiceDate(serviceDate)
      .build();
    Instant midnight = ServiceDateUtils.asStartOfService(serviceDate, ZoneIds.HELSINKI).toInstant();
    var tripTimeOnDates = TripTimeOnDate.fromTripTimesWithScheduleFallback(
      timetable,
      trip,
      serviceDate,
      midnight
    );
    int i = 0;
    for (var tripTimeOnDate : tripTimeOnDates) {
      assertNull(tripTimeOnDate.getServiceDay());
      assertEquals(tripTimeOnDate.getServiceDayMidnight(), TripTimeOnDate.UNDEFINED);
      assertEquals(tripTimeOnDate.getTripTimes(), tripTimes);
      assertEquals(tripTimeOnDate.getStopPosition(), i);
      i++;
    }
  }

  @Test
  void scheduledStop() {
    var route = TransitRepositoryForTest.route("R1").build();
    var stopA = TEST_MODEL.stop("A").build();
    var scheduledStop = TEST_MODEL.stop("scheduled").build();
    var realTimeStop = TEST_MODEL.stop("realtime").build();
    var stopC = TEST_MODEL.stop("C").build();

    var scheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, scheduledStop, stopC))
      .build();
    var realTimePattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, realTimeStop, stopC))
      .build();

    var subject = new TripTimeOnDate(tripTimesFor(3), 1, realTimePattern);

    assertEquals(realTimeStop, subject.getStop());
    assertEquals(scheduledStop, subject.getScheduledStop(scheduledPattern));
  }

  @Test
  void testScheduledStopForExtraCall() {
    // The scheduled stop should fall back to the same stop as realtime
    var route = TransitRepositoryForTest.route("R1").build();
    var stopA = TEST_MODEL.stop("A").build();
    var realTimeStop = TEST_MODEL.stop("realtime").build();
    var stopC = TEST_MODEL.stop("C").build();

    var shorterScheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, stopC))
      .build();
    var realTimePattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, realTimeStop, stopC))
      .build();
    var tripTimes = tripTimesFor(3)
      .createRealTimeFromScheduledTimes()
      .withExtraCall(1, true)
      .build();

    var first = new TripTimeOnDate(tripTimes, 0, realTimePattern);
    var second = new TripTimeOnDate(tripTimes, 1, realTimePattern);
    var third = new TripTimeOnDate(tripTimes, 2, realTimePattern);

    assertEquals(stopA, first.getScheduledStop(shorterScheduledPattern));
    assertEquals(realTimeStop, second.getScheduledStop(shorterScheduledPattern));
    assertEquals(stopC, third.getScheduledStop(shorterScheduledPattern));
  }

  @Test
  void scheduledPickupDropoffIgnoresCancellation() {
    var route = TransitRepositoryForTest.route("R1").build();
    var stopPattern = TransitRepositoryForTest.stopPattern(
      TEST_MODEL.stop("A").build(),
      TEST_MODEL.stop("B").build(),
      TEST_MODEL.stop("C").build()
    );
    var scheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(stopPattern)
      .build();
    var tripTimes = tripTimesFor(3).createRealTimeFromScheduledTimes().withCanceled().build();

    var subject = new TripTimeOnDate(tripTimes, 1, scheduledPattern);

    assertEquals(PickDrop.CANCELLED, subject.getPickupType());
    assertEquals(PickDrop.CANCELLED, subject.getDropoffType());
    assertEquals(PickDrop.SCHEDULED, subject.getScheduledPickupType(scheduledPattern));
    assertEquals(PickDrop.SCHEDULED, subject.getScheduledDropoffType(scheduledPattern));
  }

  @Test
  void scheduledPickupDropoffWithExtraCall() {
    var route = TransitRepositoryForTest.route("R1").build();
    var stopA = TEST_MODEL.stop("A").build();
    var stopC = TEST_MODEL.stop("C").build();

    var scheduledStopPattern = StopPattern.create(2);
    scheduledStopPattern.stops.with(0, stopA);
    scheduledStopPattern.pickups.with(0, PickDrop.SCHEDULED);
    scheduledStopPattern.dropoffs.with(0, PickDrop.NONE);
    scheduledStopPattern.stops.with(1, stopC);
    scheduledStopPattern.pickups.with(1, PickDrop.NONE);
    scheduledStopPattern.dropoffs.with(1, PickDrop.CALL_AGENCY);
    var scheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(scheduledStopPattern.build())
      .build();
    var realTimePattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(
        TransitRepositoryForTest.stopPattern(stopA, TEST_MODEL.stop("extra").build(), stopC)
      )
      .build();
    var tripTimes = tripTimesFor(3)
      .createRealTimeFromScheduledTimes()
      .withExtraCall(1, true)
      .build();

    var first = new TripTimeOnDate(tripTimes, 0, realTimePattern);
    var extra = new TripTimeOnDate(tripTimes, 1, realTimePattern);
    var last = new TripTimeOnDate(tripTimes, 2, realTimePattern);

    assertEquals(PickDrop.SCHEDULED, first.getScheduledPickupType(scheduledPattern));
    assertEquals(PickDrop.NONE, first.getScheduledDropoffType(scheduledPattern));

    assertNull(extra.getScheduledPickupType(scheduledPattern));
    assertNull(extra.getScheduledDropoffType(scheduledPattern));

    assertEquals(PickDrop.NONE, last.getScheduledPickupType(scheduledPattern));
    assertEquals(PickDrop.CALL_AGENCY, last.getScheduledDropoffType(scheduledPattern));
  }

  @Test
  void findScheduledStopWhenStopIsChanged() {
    var route = TransitRepositoryForTest.route("R1").build();
    var stopA = TEST_MODEL.stop("A").build();
    var scheduledStop = TEST_MODEL.stop("scheduled").build();
    var realTimeStop = TEST_MODEL.stop("realtime").build();
    var stopC = TEST_MODEL.stop("C").build();

    var scheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, scheduledStop, stopC))
      .build();
    var realTimePattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, realTimeStop, stopC))
      .build();
    var tripTimes = tripTimesFor(3);

    assertEquals(
      stopA,
      new TripTimeOnDate(tripTimes, 0, realTimePattern).findScheduledStop(scheduledPattern)
    );
    assertEquals(
      scheduledStop,
      new TripTimeOnDate(tripTimes, 1, realTimePattern).findScheduledStop(scheduledPattern)
    );
    assertEquals(
      stopC,
      new TripTimeOnDate(tripTimes, 2, realTimePattern).findScheduledStop(scheduledPattern)
    );
  }

  @Test
  void findScheduledStopWithExtraCall() {
    var route = TransitRepositoryForTest.route("R1").build();
    var stopA = TEST_MODEL.stop("A").build();
    var extraStop = TEST_MODEL.stop("extra").build();
    var stopC = TEST_MODEL.stop("C").build();

    var scheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, stopC))
      .build();
    var realTimePattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, extraStop, stopC))
      .build();
    var tripTimes = tripTimesFor(3)
      .createRealTimeFromScheduledTimes()
      .withExtraCall(1, true)
      .build();

    assertEquals(
      stopA,
      new TripTimeOnDate(tripTimes, 0, realTimePattern).findScheduledStop(scheduledPattern)
    );
    // The extra call has no scheduled stop
    assertNull(
      new TripTimeOnDate(tripTimes, 1, realTimePattern).findScheduledStop(scheduledPattern)
    );
    assertEquals(
      stopC,
      new TripTimeOnDate(tripTimes, 2, realTimePattern).findScheduledStop(scheduledPattern)
    );
  }

  @Test
  void findScheduledStopForAddedTrip() {
    // An added trip has no scheduled pattern; the pattern it was added with stands in for it
    var pattern = TEST_MODEL.pattern(TransitMode.BUS).build();
    var tripTimes = tripTimesFor(3).createRealTimeFromScheduledTimes().withAdded().build();

    for (int stopPos = 0; stopPos < 3; stopPos++) {
      assertNull(new TripTimeOnDate(tripTimes, stopPos, pattern).findScheduledStop(pattern));
    }
  }

  /**
   * The real-time stops are not the scheduled stops plus extra calls, so they can't be matched to
   * the scheduled stops, for example when a replacement trip calls at more or fewer stops.
   */
  @Test
  void findScheduledStopWhenStopsCannotBeMatched() {
    var route = TransitRepositoryForTest.route("R1").build();
    var stopA = TEST_MODEL.stop("A").build();
    var stopB = TEST_MODEL.stop("B").build();
    var stopC = TEST_MODEL.stop("C").build();
    var stopD = TEST_MODEL.stop("D").build();

    var scheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, stopB, stopC))
      .build();
    var morePattern = TransitRepositoryForTest.tripPattern("P2", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, stopB, stopC, stopD))
      .build();
    var fewerPattern = TransitRepositoryForTest.tripPattern("P3", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, stopC))
      .build();

    var moreStops = tripTimesFor(4);
    for (int stopPos = 0; stopPos < 4; stopPos++) {
      assertNull(
        new TripTimeOnDate(moreStops, stopPos, morePattern).findScheduledStop(scheduledPattern)
      );
    }
    var fewerStops = tripTimesFor(2);
    for (int stopPos = 0; stopPos < 2; stopPos++) {
      assertNull(
        new TripTimeOnDate(fewerStops, stopPos, fewerPattern).findScheduledStop(scheduledPattern)
      );
    }
  }

  /**
   * If the real-time stops can't be matched to the scheduled stops, there is no scheduled pickup
   * and drop off type, and the scheduled stop falls back to the real-time stop.
   */
  @Test
  void scheduledPickupDropoffAndStopWhenStopsCannotBeMatched() {
    var route = TransitRepositoryForTest.route("R1").build();
    var stopA = TEST_MODEL.stop("A").build();
    var stopB = TEST_MODEL.stop("B").build();
    var stopC = TEST_MODEL.stop("C").build();
    var stopD = TEST_MODEL.stop("D").build();

    var scheduledPattern = TransitRepositoryForTest.tripPattern("P1", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, stopB, stopC))
      .build();
    // A replacement trip that calls at one more stop than scheduled
    var realTimePattern = TransitRepositoryForTest.tripPattern("P2", route)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopA, stopB, stopC, stopD))
      .build();
    var tripTimes = tripTimesFor(4);

    for (int stopPos = 0; stopPos < 4; stopPos++) {
      var subject = new TripTimeOnDate(tripTimes, stopPos, realTimePattern);
      assertNull(subject.getScheduledPickupType(scheduledPattern));
      assertNull(subject.getScheduledDropoffType(scheduledPattern));
      assertEquals(subject.getStop(), subject.getScheduledStop(scheduledPattern));
    }
  }

  private static TripTimes<?> tripTimesFor(int stops) {
    var trip = TransitRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(stops, trip, "11:00");
    return TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
  }

  private List<String> mapTripTimeOnDateToStopId(List<TripTimeOnDate> tripTimes) {
    return tripTimes
      .stream()
      .map(t -> t.getStop().getId().toString())
      .toList();
  }
}
