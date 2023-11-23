package org.opentripplanner.model.plan.legreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

class ScheduledTransitLegReferenceTest {

  private static TransitModelForTest TEST_MODEL = TransitModelForTest.of();
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
    TripPattern tripPattern = TransitModelForTest
      .tripPattern("1", TransitModelForTest.route(id("1")).build())
      .withStopPattern(TransitModelForTest.stopPattern(stop1, stop2, stop3))
      .build();
    Timetable timetable = tripPattern.getScheduledTimetable();
    Trip trip = TransitModelForTest.trip("1").build();
    tripId = trip.getId();
    stopIdAtPosition0 = tripPattern.getStop(0).getId();
    stopIdAtPosition1 = tripPattern.getStop(1).getId();
    stopIdAtPosition2 = tripPattern.getStop(2).getId();
    var tripTimes = TripTimesFactory.tripTimes(
      trip,
      TEST_MODEL.stopTimesEvery5Minutes(5, trip, PlanTestConstants.T11_00),
      new Deduplicator()
    );
    tripTimes.setServiceCode(SERVICE_CODE);
    timetable.addTripTimes(tripTimes);

    // build transit model
    StopModel stopModel = TEST_MODEL
      .stopModelBuilder()
      .withRegularStop(stop1)
      .withRegularStop(stop2)
      .withRegularStop(stop3)
      .withRegularStop(stop4)
      .build();
    TransitModel transitModel = new TransitModel(stopModel, new Deduplicator());
    transitModel.addTripPattern(tripPattern.getId(), tripPattern);
    transitModel.getServiceCodes().put(tripPattern.getId(), SERVICE_CODE);
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(tripPattern.getId(), List.of(SERVICE_DATE));
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    transitModel
      .getTripOnServiceDates()
      .put(
        TRIP_ON_SERVICE_DATE_ID,
        TripOnServiceDate
          .of(TRIP_ON_SERVICE_DATE_ID)
          .withTrip(trip)
          .withServiceDate(SERVICE_DATE)
          .build()
      );

    transitModel.index();

    // build transit service
    transitService = new DefaultTransitService(transitModel);
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
    assertEquals(tripId, leg.getTrip().getId());
    assertEquals(SERVICE_DATE, leg.getServiceDate());
    assertEquals(boardAtStopPos, leg.getBoardStopPosInPattern());
    assertEquals(alightAtStopPos, leg.getAlightStopPosInPattern());
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
      TransitModelForTest.id("invalid stop id"),
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
    assertThrows(
      IllegalArgumentException.class,
      () ->
        new ScheduledTransitLegReference(
          tripId,
          SERVICE_DATE,
          0,
          NUMBER_OF_STOPS,
          stopIdAtPosition0,
          stopIdAtPosition1,
          TransitModelForTest.id("trip on date id")
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
      TransitModelForTest.id("unknown trip on date id")
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
    assertEquals(tripId, leg.getTrip().getId());
    assertEquals(SERVICE_DATE, leg.getServiceDate());
  }
}
