package org.opentripplanner.model.plan.legreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

class ScheduledTransitLegReferenceTest {

  private static final int SERVICE_CODE = 555;
  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 1, 1);
  private static final int NUMBER_OF_STOPS = 3;
  private static TransitService transitService;
  private static FeedScopedId tripId;

  @BeforeAll
  static void buildTransitService() {
    // build transit data
    TripPattern tripPattern = TransitModelForTest
      .tripPattern("1", TransitModelForTest.route(id("1")).build())
      .withStopPattern(TransitModelForTest.stopPattern(NUMBER_OF_STOPS))
      .build();
    Timetable timetable = tripPattern.getScheduledTimetable();
    Trip trip = TransitModelForTest.trip("1").build();
    tripId = trip.getId();
    TripTimes tripTimes = new TripTimes(
      trip,
      TransitModelForTest.stopTimesEvery5Minutes(5, trip, PlanTestConstants.T11_00),
      new Deduplicator()
    );
    tripTimes.setServiceCode(SERVICE_CODE);
    timetable.addTripTimes(tripTimes);

    // build transit model
    TransitModel transitModel = new TransitModel(StopModel.of().build(), new Deduplicator());
    transitModel.addTripPattern(tripPattern.getId(), tripPattern);
    transitModel.getServiceCodes().put(tripPattern.getId(), SERVICE_CODE);
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(tripPattern.getId(), List.of(SERVICE_DATE));
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);
    transitModel.index();

    // build transit service
    transitService = new DefaultTransitService(transitModel);
  }

  @Test
  void getLegFromReference() {
    int boardAtStop = 0;
    int alightAtStop = 1;
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      SERVICE_DATE,
      boardAtStop,
      alightAtStop
    );
    ScheduledTransitLeg leg = scheduledTransitLegReference.getLeg(transitService);
    assertNotNull(leg);
    assertEquals(tripId, leg.getTrip().getId());
    assertEquals(SERVICE_DATE, leg.getServiceDate());
    assertEquals(boardAtStop, leg.getBoardStopPosInPattern());
    assertEquals(alightAtStop, leg.getAlightStopPosInPattern());
  }

  @Test
  void getLegFromReferenceUnknownTrip() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      FeedScopedId.ofNullable("XXX", "YYY"),
      SERVICE_DATE,
      0,
      1
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceInvalidServiceDate() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      LocalDate.EPOCH,
      0,
      1
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceInvalidBoardingStop() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      SERVICE_DATE,
      NUMBER_OF_STOPS,
      1
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }

  @Test
  void getLegFromReferenceInvalidAlightingStop() {
    ScheduledTransitLegReference scheduledTransitLegReference = new ScheduledTransitLegReference(
      tripId,
      SERVICE_DATE,
      0,
      NUMBER_OF_STOPS
    );
    assertNull(scheduledTransitLegReference.getLeg(transitService));
  }
}
