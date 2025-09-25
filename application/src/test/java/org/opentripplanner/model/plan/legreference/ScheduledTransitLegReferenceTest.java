package org.opentripplanner.model.plan.legreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.GraphUpdaterManager;

class ScheduledTransitLegReferenceTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final int SERVICE_CODE = 555;
  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 1, 1);
  private static final int NUMBER_OF_STOPS = 3;
  private static FeedScopedId TRIP_ON_SERVICE_DATE_ID;
  public static final FeedScopedId STOP_1_ID = id("STOP1");
  public static final FeedScopedId STOP_2_ID = id("STOP2");

  public static final FeedScopedId STOP_3A_ID = id("STOP3A");
  public static final FeedScopedId STOP_3B_ID = id("STOP3B");
  private static TransitService transitService;
  private static final FeedScopedId SIMPLE_TRIP_ID = id("trip");
  private static final FeedScopedId TRIP_ID_WITH_MULTIPLE_CALLS = id("multiple_calls");
  private static final FeedScopedId LOOP_TRIP_ID = id("loop");

  @BeforeAll
  static void buildTransitService() {
    Station parentStation = TEST_MODEL.station("PARENT_STATION").build();

    RegularStop stop1 = TEST_MODEL.stop(STOP_1_ID.getId(), 0, 0).build();
    RegularStop stop2 = TEST_MODEL.stop(STOP_2_ID.getId(), 0, 0).build();
    RegularStop stop3a = TEST_MODEL.stop(STOP_3A_ID.getId(), 0, 0)
      .withParentStation(parentStation)
      .build();
    RegularStop stop3b = TEST_MODEL.stop(STOP_3B_ID.getId(), 0, 0)
      .withParentStation(parentStation)
      .build();

    // build transit model
    SiteRepository siteRepository = TEST_MODEL.siteRepositoryBuilder()
      .withRegularStop(stop1)
      .withRegularStop(stop2)
      .withRegularStop(stop3a)
      .withRegularStop(stop3b)
      .build();
    TimetableRepository timetableRepository = new TimetableRepository(
      siteRepository,
      new Deduplicator()
    );
    timetableRepository.setUpdaterManager(
      new GraphUpdaterManager(
        new DefaultRealTimeUpdateContext(new Graph(), timetableRepository, new TimetableSnapshot()),
        List.of()
      )
    );
    // build transit data
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    for (var item : Map.of(
      SIMPLE_TRIP_ID,
      TimetableRepositoryForTest.stopPattern(stop1, stop2, stop3a),
      TRIP_ID_WITH_MULTIPLE_CALLS,
      TimetableRepositoryForTest.stopPattern(stop1, stop2, stop3a, stop3b),
      LOOP_TRIP_ID,
      TimetableRepositoryForTest.stopPattern(stop1, stop2, stop3a, stop1, stop2, stop3b)
    ).entrySet()) {
      Trip trip = TimetableRepositoryForTest.trip(item.getKey().getId()).build();
      var tripTimes = TripTimesFactory.tripTimes(
        trip,
        TEST_MODEL.stopTimesEvery5Minutes(item.getValue().getSize(), trip, "11:00"),
        new Deduplicator()
      ).withServiceCode(SERVICE_CODE);
      TripPattern tripPattern = TimetableRepositoryForTest.tripPattern(
        "TRIP_PATTERN_" + item.getKey().getId(),
        TimetableRepositoryForTest.route(id("1")).build()
      )
        .withStopPattern(item.getValue())
        .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
        .build();
      timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
      timetableRepository.getServiceCodes().put(tripPattern.getId(), SERVICE_CODE);
      FeedScopedId tripOnServiceDateId = id("TRIP_ON_SERVICE_DATE" + item.getKey().getId());
      timetableRepository.addTripOnServiceDate(
        TripOnServiceDate.of(tripOnServiceDateId)
          .withTrip(trip)
          .withServiceDate(SERVICE_DATE)
          .build()
      );
      if (item.getKey() == SIMPLE_TRIP_ID) {
        TRIP_ON_SERVICE_DATE_ID = tripOnServiceDateId;
      }
      calendarServiceData.putServiceDatesForServiceId(tripPattern.getId(), List.of(SERVICE_DATE));
    }

    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );

    timetableRepository.index();

    // build transit service
    transitService = new DefaultTransitService(timetableRepository);
  }

  @Test
  void getLegFromReference() {
    int boardAtStopPos = 0;
    int alightAtStopPos = 1;
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      boardAtStopPos,
      alightAtStopPos,
      STOP_1_ID,
      STOP_2_ID,
      null
    );
    ScheduledTransitLeg leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(SIMPLE_TRIP_ID, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
    assertEquals(boardAtStopPos, leg.boardStopPosInPattern());
    assertEquals(alightAtStopPos, leg.alightStopPosInPattern());
  }

  @Test
  void getLegFromReferenceUnknownTrip() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      FeedScopedId.ofNullable("XXX", "YYY"),
      SERVICE_DATE,
      0,
      1,
      STOP_1_ID,
      STOP_2_ID,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceInvalidServiceDate() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      LocalDate.EPOCH,
      0,
      1,
      STOP_1_ID,
      STOP_2_ID,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceMismatchOnBoardingStop() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      0,
      1,
      TimetableRepositoryForTest.id("invalid stop id"),
      STOP_2_ID,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceMismatchOnAlightingStopSameParentStation() {
    // this tests substitutes the actual alighting stop (stop3a) by another stop (stop3b) that
    // belongs to the same parent station
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      0,
      2,
      STOP_1_ID,
      STOP_3B_ID,
      null
    );
    assertNotNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceMultipleCallsInSameStationConsecutive() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      TRIP_ID_WITH_MULTIPLE_CALLS,
      SERVICE_DATE,
      0,
      2,
      STOP_1_ID,
      STOP_3B_ID,
      null
    );
    var leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(TRIP_ID_WITH_MULTIPLE_CALLS, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
    assertEquals(0, leg.boardStopPosInPattern());
    // a call at stop 3a is inserted into position 2, we prefer the exact match immediately
    // afterward
    assertEquals(3, leg.alightStopPosInPattern());
  }

  @Test
  void getLegFromReferenceMultipleCallsInSameStationNotConsecutive() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      LOOP_TRIP_ID,
      SERVICE_DATE,
      0,
      2,
      STOP_1_ID,
      STOP_3B_ID,
      null
    );
    var leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(LOOP_TRIP_ID, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
    assertEquals(0, leg.boardStopPosInPattern());
    // we assume that it is a platform change from stop 3B to stop 3A, rather than a loop added
    // into the trip
    assertEquals(2, leg.alightStopPosInPattern());
  }

  @Test
  void getLegFromReferenceWithAddedCall() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      0,
      1,
      STOP_1_ID,
      STOP_3A_ID,
      null
    );
    var leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(SIMPLE_TRIP_ID, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
    assertEquals(0, leg.boardStopPosInPattern());
    assertEquals(2, leg.alightStopPosInPattern());
  }

  @Test
  void getLegFromReferenceWithAddedCallAndPlatformChange() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      0,
      1,
      STOP_1_ID,
      STOP_3B_ID,
      null
    );
    var leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(SIMPLE_TRIP_ID, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
    assertEquals(0, leg.boardStopPosInPattern());
    assertEquals(2, leg.alightStopPosInPattern());
  }

  @Test
  void getLegFromReferenceWithRemovedCall() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      0,
      2,
      STOP_1_ID,
      STOP_2_ID,
      null
    );
    var leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(SIMPLE_TRIP_ID, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
    assertEquals(0, leg.boardStopPosInPattern());
    assertEquals(1, leg.alightStopPosInPattern());
  }

  @Test
  void getLegFromReferenceWithReversedCall() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      0,
      2,
      STOP_3A_ID,
      STOP_1_ID,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceOutOfRangeAlightingStop() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      SIMPLE_TRIP_ID,
      SERVICE_DATE,
      0,
      NUMBER_OF_STOPS,
      STOP_1_ID,
      STOP_2_ID,
      null
    );
    var leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(SIMPLE_TRIP_ID, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
    assertEquals(0, leg.boardStopPosInPattern());
    // we assume that the timetable has changed and all intermediate stops have been removed
    assertEquals(1, leg.alightStopPosInPattern());
  }

  @Test
  void legReferenceCannotReferToBothTripAndTripOnServiceDate() {
    assertThrows(IllegalArgumentException.class, () ->
      new ScheduledTransitLegReference(
        SIMPLE_TRIP_ID,
        SERVICE_DATE,
        0,
        NUMBER_OF_STOPS,
        STOP_1_ID,
        STOP_2_ID,
        TimetableRepositoryForTest.id("trip on date id")
      )
    );
  }

  @Test
  void legReferenceMustContainEitherTripOrTripOnServiceDate() {
    assertThrows(IllegalArgumentException.class, () ->
      new ScheduledTransitLegReference(
        null,
        SERVICE_DATE,
        0,
        NUMBER_OF_STOPS,
        STOP_1_ID,
        STOP_2_ID,
        null
      )
    );
  }

  @Test
  void legReferenceCannotReferToInconsistentServiceDateAndTripOnServiceDate() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      null,
      SERVICE_DATE.plusDays(1),
      0,
      1,
      STOP_1_ID,
      STOP_2_ID,
      TRIP_ON_SERVICE_DATE_ID
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceWithUnknownTripOnDate() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      null,
      SERVICE_DATE,
      0,
      NUMBER_OF_STOPS,
      STOP_1_ID,
      STOP_2_ID,
      TimetableRepositoryForTest.id("unknown trip on date id")
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceWithValidTripOnDate() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      null,
      SERVICE_DATE,
      0,
      1,
      STOP_1_ID,
      STOP_2_ID,
      TRIP_ON_SERVICE_DATE_ID
    );
    ScheduledTransitLeg leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(SIMPLE_TRIP_ID, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
  }
}
