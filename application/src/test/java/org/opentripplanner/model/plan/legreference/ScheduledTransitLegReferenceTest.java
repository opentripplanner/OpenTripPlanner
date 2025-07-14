package org.opentripplanner.model.plan.legreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.util.List;
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
  private static final FeedScopedId TRIP_ON_SERVICE_DATE_ID = id("TRIP_ON_SERVICE_DATE_ID");
  public static FeedScopedId stopIdAtPosition0;
  public static FeedScopedId stopIdAtPosition1;

  public static FeedScopedId stopIdAtPosition2;
  private static TransitService transitService;
  private static FeedScopedId tripId;
  private static RegularStop stop4;

  @BeforeAll
  static void buildTransitService() {
    Station parentStation = TEST_MODEL.station("PARENT_STATION").build();

    RegularStop stop1 = TEST_MODEL.stop("STOP1", 0, 0).build();
    RegularStop stop2 = TEST_MODEL.stop("STOP2", 0, 0).build();
    RegularStop stop3 = TEST_MODEL.stop("STOP3", 0, 0).withParentStation(parentStation).build();
    stop4 = TEST_MODEL.stop("STOP4", 0, 0).withParentStation(parentStation).build();

    // build transit data
    Trip trip = TimetableRepositoryForTest.trip("1").build();
    var tripTimes = TripTimesFactory.tripTimes(
      trip,
      TEST_MODEL.stopTimesEvery5Minutes(5, trip, "11:00"),
      new Deduplicator()
    ).withServiceCode(SERVICE_CODE);
    TripPattern tripPattern = TimetableRepositoryForTest.tripPattern(
      "1",
      TimetableRepositoryForTest.route(id("1")).build()
    )
      .withStopPattern(TimetableRepositoryForTest.stopPattern(stop1, stop2, stop3))
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    tripId = trip.getId();
    stopIdAtPosition0 = tripPattern.getStop(0).getId();
    stopIdAtPosition1 = tripPattern.getStop(1).getId();
    stopIdAtPosition2 = tripPattern.getStop(2).getId();

    // build transit model
    SiteRepository siteRepository = TEST_MODEL.siteRepositoryBuilder()
      .withRegularStop(stop1)
      .withRegularStop(stop2)
      .withRegularStop(stop3)
      .withRegularStop(stop4)
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
    timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
    timetableRepository.getServiceCodes().put(tripPattern.getId(), SERVICE_CODE);
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(tripPattern.getId(), List.of(SERVICE_DATE));
    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );

    timetableRepository.addTripOnServiceDate(
      TripOnServiceDate.of(TRIP_ON_SERVICE_DATE_ID)
        .withTrip(trip)
        .withServiceDate(SERVICE_DATE)
        .build()
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
      tripId,
      SERVICE_DATE,
      boardAtStopPos,
      alightAtStopPos,
      stopIdAtPosition0,
      stopIdAtPosition1,
      null
    );
    ScheduledTransitLeg leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(tripId, leg.trip().getId());
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
      stopIdAtPosition0,
      stopIdAtPosition1,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceInvalidServiceDate() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      LocalDate.EPOCH,
      0,
      1,
      stopIdAtPosition0,
      stopIdAtPosition1,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceOutOfRangeBoardingStop() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      SERVICE_DATE,
      NUMBER_OF_STOPS,
      1,
      stopIdAtPosition0,
      stopIdAtPosition1,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceMismatchOnBoardingStop() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      SERVICE_DATE,
      0,
      1,
      TimetableRepositoryForTest.id("invalid stop id"),
      stopIdAtPosition1,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceMismatchOnAlightingStopSameParentStation() {
    // this tests substitutes the actual alighting stop (stop3) by another stop (stop4) that
    // belongs to the same parent station
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      SERVICE_DATE,
      0,
      2,
      stopIdAtPosition0,
      stop4.getId(),
      null
    );
    assertNotNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceOutOfRangeAlightingStop() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      SERVICE_DATE,
      0,
      NUMBER_OF_STOPS,
      stopIdAtPosition0,
      stopIdAtPosition1,
      null
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void legReferenceCannotReferToBothTripAndTripOnServiceDate() {
    assertThrows(IllegalArgumentException.class, () ->
      new ScheduledTransitLegReference(
        tripId,
        SERVICE_DATE,
        0,
        NUMBER_OF_STOPS,
        stopIdAtPosition0,
        stopIdAtPosition1,
        TimetableRepositoryForTest.id("trip on date id")
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
      stopIdAtPosition0,
      stopIdAtPosition1,
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
      stopIdAtPosition0,
      stopIdAtPosition1,
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
      stopIdAtPosition0,
      stopIdAtPosition1,
      TRIP_ON_SERVICE_DATE_ID
    );
    ScheduledTransitLeg leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(tripId, leg.trip().getId());
    assertEquals(SERVICE_DATE, leg.serviceDate());
  }
}
