package org.opentripplanner.ext.siri;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opentripplanner.DateTimeHelper;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
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
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.TimetableSnapshotSource;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

public class RealtimeTestEnvironment {

  private static final FeedScopedId CAL_ID = TransitModelForTest.id("CAL_1");
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
  public SiriTimetableSnapshotSource siriSource;
  public final TimetableSnapshotSource gtfsSource;

  public final FeedScopedId operator1Id = TransitModelForTest.id("TestOperator1");
  public final FeedScopedId route1Id = TransitModelForTest.id("TestRoute1");
  public final Trip trip1;
  public final Trip trip2;

  public final DateTimeHelper dateTimeHelper = new DateTimeHelper(timeZone, serviceDate);

  public RealtimeTestEnvironment() {
    transitModel = new TransitModel(stopModel, new Deduplicator());
    transitModel.initTimeZone(timeZone);
    transitModel.addAgency(TransitModelForTest.AGENCY);

    Route route1 = TransitModelForTest.route(route1Id).build();

    trip1 =
      createTrip("TestTrip1", route1, List.of(new Stop(stopA1, 10, 11), new Stop(stopB1, 20, 21)));
    trip2 =
      createTrip(
        "TestTrip2",
        route1,
        List.of(new Stop(stopA1, 60, 61), new Stop(stopB1, 70, 71), new Stop(stopC1, 80, 81))
      );

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      CAL_ID,
      List.of(serviceDate.minusDays(1), serviceDate, serviceDate.plusDays(1))
    );
    transitModel.getServiceCodes().put(CAL_ID, 0);
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    transitModel.index();

    var parameters = new TimetableSnapshotSourceParameters(Duration.ZERO, false);
    //siriSource = new SiriTimetableSnapshotSource(parameters, transitModel);
    gtfsSource = new TimetableSnapshotSource(parameters, transitModel);
  }

  private record Stop(RegularStop stop, int arrivalTime, int departureTime) {}

  private Trip createTrip(String id, Route route, List<Stop> stops) {
    var trip = Trip.of(id(id)).withRoute(route).withServiceId(CAL_ID).build();

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
    return this.siriSource.applyEstimatedTimetable(
        null,
        getEntityResolver(),
        getFeedId(),
        false,
        updates
      );
  }

  public UpdateResult applyTripUpdates(List<GtfsRealtime.TripUpdate> updates) {
    return gtfsSource.applyTripUpdates(
      null,
      BackwardsDelayPropagationType.REQUIRED,
      true,
      updates,
      getFeedId()
    );
  }
}
