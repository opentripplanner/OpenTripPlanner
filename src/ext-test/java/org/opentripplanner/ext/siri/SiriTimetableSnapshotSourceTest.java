package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.DateTimeHelper;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

class SiriTimetableSnapshotSourceTest {

  @Test
  void testCancelTrip() {
    var env = new RealtimeTestEnvironment();

    assertEquals(RealTimeState.SCHEDULED, env.getTripTimesForTrip(env.trip1).getRealTimeState());

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(RealTimeState.CANCELED, env.getTripTimesForTrip(env.trip1).getRealTimeState());
  }

  @Test
  void testAddJourney() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      .withOperatorRef(env.operator1Id.getId())
      .withLineRef(env.route1Id.getId())
      .withRecordedCalls(builder -> builder.call(env.stopC1).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(env.stopD1).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    var tripTimes = env.getTripTimesForTrip("newJourney");
    assertEquals(RealTimeState.ADDED, tripTimes.getRealTimeState());
    assertEquals(2 * 60, tripTimes.getDepartureTime(0));
    assertEquals(4 * 60, tripTimes.getDepartureTime(1));
  }

  @Test
  void testReplaceJourney() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      // replace trip1
      .withVehicleJourneyRef(env.trip1.getId().getId())
      .withOperatorRef(env.operator1Id.getId())
      .withLineRef(env.route1Id.getId())
      .withRecordedCalls(builder -> builder.call(env.stopA1).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(env.stopC1).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var tripTimes = env.getTripTimesForTrip("newJourney");
    var pattern = env.getPatternForTrip(env.id("newJourney"));
    assertEquals(RealTimeState.ADDED, tripTimes.getRealTimeState());
    assertEquals(2 * 60, tripTimes.getDepartureTime(0));
    assertEquals(4 * 60, tripTimes.getDepartureTime(1));
    assertEquals(env.stopA1.getId(), pattern.getStop(0).getId());
    assertEquals(env.stopC1.getId(), pattern.getStop(1).getId());

    var originalTripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
  }

  /**
   * Update calls without changing the pattern
   */
  @Test
  void testUpdateJourney() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder.call(env.stopB1).arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var tripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.UPDATED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));
    assertEquals(20, tripTimes.getScheduledArrivalTime(1));
    assertEquals(25, tripTimes.getArrivalTime(1));
  }

  /**
   * Change quay on a trip
   */
  @Test
  void testChangeQuay() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder.call(env.stopB2).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var pattern = env.getPatternForTrip(env.trip1.getId());
    var tripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));
    assertEquals(20, tripTimes.getScheduledArrivalTime(1));
    assertEquals(33, tripTimes.getArrivalTime(1));
    assertEquals(env.stopB2, pattern.getStop(1));
  }

  @Test
  void testCancelStop() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip2.getId().getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(env.stopB1)
          .withIsCancellation(true)
          .call(env.stopC1)
          .arriveAimedExpected("00:01:30", "00:01:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var pattern = env.getPatternForTrip(env.trip2.getId());

    assertEquals(PickDrop.SCHEDULED, pattern.getAlightType(0));
    assertEquals(PickDrop.CANCELLED, pattern.getAlightType(1));
    assertEquals(PickDrop.SCHEDULED, pattern.getAlightType(2));
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testAddStop() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopD1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(env.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var pattern = env.getPatternForTrip(env.trip1.getId());
    var tripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));

    // Should it work to get the scheduled times from an extra call?
    assertEquals(19, tripTimes.getScheduledArrivalTime(1));
    assertEquals(24, tripTimes.getScheduledDepartureTime(1));

    assertEquals(20, tripTimes.getDepartureTime(1));
    assertEquals(25, tripTimes.getDepartureTime(1));

    assertEquals(20, tripTimes.getScheduledArrivalTime(2));
    assertEquals(33, tripTimes.getArrivalTime(2));
    assertEquals(List.of(env.stopA1, env.stopD1, env.stopB1), pattern.getStops());
  }

  /////////////////
  // Error cases //
  /////////////////

  @Test
  void testNotMonitored() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withMonitored(false)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.NOT_MONITORED);
  }

  @Test
  void testReplaceJourneyWithoutEstimatedVehicleJourneyCode() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef("newJourney")
      .withIsExtraJourney(true)
      .withVehicleJourneyRef(env.trip1.getId().getId())
      .withOperatorRef(env.operator1Id.getId())
      .withLineRef(env.route1Id.getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("00:01", "00:02")
          .call(env.stopC1)
          .arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    // TODO: this should have a more specific error type
    assertFailure(result, UpdateError.UpdateErrorType.UNKNOWN);
  }

  @Test
  void testNegativeHopTime() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedActual("00:00:11", "00:00:15")
          .call(env.stopB1)
          .arriveAimedActual("00:00:20", "00:00:14")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME);
  }

  @Test
  void testNegativeDwellTime() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip2.getId().getId())
      .withRecordedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedActual("00:01:01", "00:01:01")
          .call(env.stopB1)
          .arriveAimedActual("00:01:10", "00:01:13")
          .departAimedActual("00:01:11", "00:01:12")
          .call(env.stopB1)
          .arriveAimedActual("00:01:20", "00:01:20")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME);
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testExtraUnknownStop() {
    var env = new RealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(env.stopD1)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(env.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE);
  }

  private void assertFailure(UpdateResult result, UpdateError.UpdateErrorType errorType) {
    assertEquals(result.failures().keySet(), Set.of(errorType));
  }

  private static class RealtimeTestEnvironment {

    private final TransitModelForTest testModel = TransitModelForTest.of();
    public final ZoneId timeZone = ZoneId.of(TransitModelForTest.TIME_ZONE_ID);
    public final Station stationA = testModel.station("A").build();
    public final Station stationB = testModel.station("B").build();
    public final Station stationC = testModel.station("C").build();
    public final Station stationD = testModel.station("D").build();
    public final RegularStop stopA1 = testModel.stop("A1").withParentStation(stationA).build();
    public final RegularStop stopB1 = testModel.stop("B1").withParentStation(stationB).build();
    public final RegularStop stopB2 = testModel.stop("B2").withParentStation(stationB).build();
    public final RegularStop stopC1 = testModel.stop("C1").withParentStation(stationC).build();
    public final RegularStop stopD1 = testModel.stop("D1").withParentStation(stationD).build();
    public final StopModel stopModel = testModel
      .stopModelBuilder()
      .withRegularStop(stopA1)
      .withRegularStop(stopB1)
      .withRegularStop(stopB2)
      .withRegularStop(stopC1)
      .withRegularStop(stopD1)
      .build();

    public final LocalDate serviceDate = LocalDate.of(2024, 5, 8);
    public TransitModel transitModel;
    public SiriTimetableSnapshotSource snapshotSource;

    public final FeedScopedId operator1Id = TransitModelForTest.id("TestOperator1");
    public final FeedScopedId route1Id = TransitModelForTest.id("TestRoute1");
    public Trip trip1;
    public Trip trip2;

    public final DateTimeHelper dateTimeHelper = new DateTimeHelper(timeZone, serviceDate);

    public RealtimeTestEnvironment() {
      transitModel = new TransitModel(stopModel, new Deduplicator());
      transitModel.initTimeZone(timeZone);
      transitModel.addAgency(TransitModelForTest.AGENCY);

      Route route1 = TransitModelForTest.route(route1Id).build();

      trip1 =
        createTrip(
          "TestTrip1",
          route1,
          List.of(new Stop(stopA1, 10, 11), new Stop(stopB1, 20, 21))
        );
      trip2 =
        createTrip(
          "TestTrip2",
          route1,
          List.of(new Stop(stopA1, 60, 61), new Stop(stopB1, 70, 71), new Stop(stopC1, 80, 81))
        );

      CalendarServiceData calendarServiceData = new CalendarServiceData();
      var cal_id = TransitModelForTest.id("CAL_1");
      calendarServiceData.putServiceDatesForServiceId(
        cal_id,
        List.of(serviceDate.minusDays(1), serviceDate, serviceDate.plusDays(1))
      );
      transitModel.getServiceCodes().put(cal_id, 0);
      transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

      transitModel.index();

      var parameters = new TimetableSnapshotSourceParameters(Duration.ZERO, false);
      snapshotSource = new SiriTimetableSnapshotSource(parameters, transitModel);
    }

    private record Stop(RegularStop stop, int arrivalTime, int departureTime) {}

    private Trip createTrip(String id, Route route, List<Stop> stops) {
      var trip = Trip.of(id(id)).withRoute(route).build();

      var tripOnServiceDate = TripOnServiceDate
        .of(trip.getId())
        .withTrip(trip)
        .withServiceDate(serviceDate)
        .build();

      transitModel.addTripOnServiceDate(tripOnServiceDate.getId(), tripOnServiceDate);

      var stopTimes = IntStream
        .range(0, stops.size())
        .mapToObj(i -> {
          var stop = stops.get(i);
          return createStopTime(trip, i, stop.stop(), stop.arrivalTime(), stop.departureTime());
        })
        .collect(Collectors.toList());

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

      final TripPattern pattern = TransitModelForTest
        .tripPattern(id + "Pattern", route)
        .withStopPattern(TransitModelForTest.stopPattern(stops.stream().map(Stop::stop).toList()))
        .build();
      pattern.add(tripTimes);

      transitModel.addTripPattern(pattern.getId(), pattern);

      return trip;
    }

    public FeedScopedId id(String id) {
      return TransitModelForTest.id(id);
    }

    /**
     * Returns a new fresh TransitService
     */
    public TransitService getTransitService() {
      return new DefaultTransitService(transitModel);
    }

    public EntityResolver getEntityResolver() {
      return new EntityResolver(getTransitService(), getFeedId());
    }

    public TripPattern getPatternForTrip(FeedScopedId tripId) {
      return getPatternForTrip(tripId, serviceDate);
    }

    public TripPattern getPatternForTrip(FeedScopedId tripId, LocalDate serviceDate) {
      var transitService = getTransitService();
      var trip = transitService.getTripOnServiceDateById(tripId);
      return transitService.getPatternForTrip(trip.getTrip(), serviceDate);
    }

    /**
     * Find the current TripTimes for a trip id on the default serviceDate
     */
    public TripTimes getTripTimesForTrip(Trip trip) {
      return getTripTimesForTrip(trip.getId(), serviceDate);
    }

    /**
     * Find the current TripTimes for a trip id on the default serviceDate
     */
    public TripTimes getTripTimesForTrip(String id) {
      return getTripTimesForTrip(id(id), serviceDate);
    }

    /**
     * Find the current TripTimes for a trip id on a serviceDate
     */
    public TripTimes getTripTimesForTrip(FeedScopedId tripId, LocalDate serviceDate) {
      var transitService = getTransitService();
      var trip = transitService.getTripOnServiceDateById(tripId).getTrip();
      var pattern = transitService.getPatternForTrip(trip, serviceDate);
      var timetable = transitService.getTimetableForTripPattern(pattern, serviceDate);
      return timetable.getTripTimes(trip);
    }

    public DateTimeHelper getDateTimeHelper() {
      return dateTimeHelper;
    }

    private StopTime createStopTime(
      Trip trip,
      int stopSequence,
      StopLocation stop,
      int arrivalTime,
      int departureTime
    ) {
      var st = new StopTime();
      st.setTrip(trip);
      st.setStopSequence(stopSequence);
      st.setStop(stop);
      st.setArrivalTime(arrivalTime);
      st.setDepartureTime(departureTime);
      return st;
    }

    public String getFeedId() {
      return TransitModelForTest.FEED_ID;
    }

    public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
      return this.snapshotSource.applyEstimatedTimetable(
          null,
          getEntityResolver(),
          getFeedId(),
          false,
          updates
        );
    }
  }
}
