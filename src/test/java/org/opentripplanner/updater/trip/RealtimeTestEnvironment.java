package org.opentripplanner.updater.trip;

import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opentripplanner.DateTimeHelper;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.ext.siri.updater.EstimatedTimetableHandler;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TimetableSnapshot;
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
import org.opentripplanner.transit.model.timetable.TripTimesStringBuilder;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

/**
 * This class exists so that you can share the data building logic for GTFS and Siri tests.
 * Since it's not possible to add a Siri and GTFS updater to the transit model at the same time,
 * they each have their own test environment.
 * <p>
 * It is however a goal to change that and then these two can be combined.
 */
public final class RealtimeTestEnvironment {

  private static final TimetableSnapshotSourceParameters PARAMETERS = new TimetableSnapshotSourceParameters(
    Duration.ZERO,
    false
  );
  public static final LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 8);
  public static final FeedScopedId SERVICE_ID = TransitModelForTest.id("CAL_1");
  public static final String STOP_A1_ID = "A1";
  public static final String STOP_B1_ID = "B1";
  public static final String STOP_C1_ID = "C1";
  private final TransitModelForTest testModel = TransitModelForTest.of();
  public final ZoneId timeZone = ZoneId.of(TransitModelForTest.TIME_ZONE_ID);
  public final Station stationA = testModel.station("A").build();
  public final Station stationB = testModel.station("B").build();
  public final Station stationC = testModel.station("C").build();
  public final Station stationD = testModel.station("D").build();
  public final RegularStop stopA1 = testModel.stop(STOP_A1_ID).withParentStation(stationA).build();
  public final RegularStop stopB1 = testModel.stop(STOP_B1_ID).withParentStation(stationB).build();
  public final RegularStop stopB2 = testModel.stop("B2").withParentStation(stationB).build();
  public final RegularStop stopC1 = testModel.stop(STOP_C1_ID).withParentStation(stationC).build();
  public final RegularStop stopD1 = testModel.stop("D1").withParentStation(stationD).build();
  public final StopModel stopModel = testModel
    .stopModelBuilder()
    .withRegularStop(stopA1)
    .withRegularStop(stopB1)
    .withRegularStop(stopB2)
    .withRegularStop(stopC1)
    .withRegularStop(stopD1)
    .build();
  public final FeedScopedId operator1Id = TransitModelForTest.id("TestOperator1");
  public final FeedScopedId route1Id = TransitModelForTest.id("TestRoute1");
  public final Trip trip1;
  public final Trip trip2;
  public final TransitModel transitModel;
  private final SiriTimetableSnapshotSource siriSource;
  private final TimetableSnapshotSource gtfsSource;
  private final DateTimeHelper dateTimeHelper;

  private enum SourceType {
    GTFS_RT,
    SIRI,
  }

  /**
   * Siri and GTFS-RT cannot be run at the same time, so you need to decide.
   */
  public static RealtimeTestEnvironment siri() {
    return new RealtimeTestEnvironment(SourceType.SIRI);
  }

  /**
   * Siri and GTFS-RT cannot be run at the same time, so you need to decide.
   */
  public static RealtimeTestEnvironment gtfs() {
    return new RealtimeTestEnvironment(SourceType.GTFS_RT);
  }

  private RealtimeTestEnvironment(SourceType sourceType) {
    transitModel = new TransitModel(stopModel, new Deduplicator());
    transitModel.initTimeZone(timeZone);
    transitModel.addAgency(TransitModelForTest.AGENCY);

    Route route1 = TransitModelForTest.route(route1Id).build();

    trip1 =
      createTrip(
        "TestTrip1",
        route1,
        List.of(new StopCall(stopA1, 10, 11), new StopCall(stopB1, 20, 21))
      );
    trip2 =
      createTrip(
        "TestTrip2",
        route1,
        List.of(
          new StopCall(stopA1, 60, 61),
          new StopCall(stopB1, 70, 71),
          new StopCall(stopC1, 80, 81)
        )
      );

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      SERVICE_ID,
      List.of(SERVICE_DATE.minusDays(1), SERVICE_DATE, SERVICE_DATE.plusDays(1))
    );
    transitModel.getServiceCodes().put(SERVICE_ID, 0);
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    transitModel.index();

    // SIRI and GTFS-RT cannot be registered with the transit model at the same time
    // we are actively refactoring to remove this restriction
    // for the time being you cannot run a SIRI and GTFS-RT test at the same time
    if (sourceType == SourceType.SIRI) {
      siriSource = new SiriTimetableSnapshotSource(PARAMETERS, transitModel);
      gtfsSource = null;
    } else {
      gtfsSource = new TimetableSnapshotSource(PARAMETERS, transitModel);
      siriSource = null;
    }
    dateTimeHelper = new DateTimeHelper(timeZone, RealtimeTestEnvironment.SERVICE_DATE);
  }

  public static FeedScopedId id(String id) {
    return TransitModelForTest.id(id);
  }

  /**
   * Returns a new fresh TransitService
   */
  public TransitService getTransitService() {
    return new DefaultTransitService(transitModel);
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

  public String getFeedId() {
    return TransitModelForTest.FEED_ID;
  }

  private EstimatedTimetableHandler getEstimatedTimetableHandler(boolean fuzzyMatching) {
    return new EstimatedTimetableHandler(
      siriSource,
      fuzzyMatching ? new SiriFuzzyTripMatcher(getTransitService()) : null,
      getTransitService(),
      getFeedId()
    );
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId) {
    return getPatternForTrip(tripId, RealtimeTestEnvironment.SERVICE_DATE);
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
    return getTripTimesForTrip(trip.getId(), SERVICE_DATE);
  }

  /**
   * Find the current TripTimes for a trip id on the default serviceDate
   */
  public TripTimes getTripTimesForTrip(String id) {
    return getTripTimesForTrip(id(id), SERVICE_DATE);
  }

  public DateTimeHelper getDateTimeHelper() {
    return dateTimeHelper;
  }

  public TripPattern getPatternForTrip(Trip trip) {
    return transitModel.getTransitModelIndex().getPatternForTrip().get(trip);
  }

  public TimetableSnapshot getTimetableSnapshot() {
    if (siriSource != null) {
      return siriSource.getTimetableSnapshot();
    } else {
      return gtfsSource.getTimetableSnapshot();
    }
  }

  public String getRealtimeTimetable(String tripId) {
    return getRealtimeTimetable(id(tripId), SERVICE_DATE);
  }

  public String getRealtimeTimetable(Trip trip) {
    return getRealtimeTimetable(trip.getId(), SERVICE_DATE);
  }

  public String getRealtimeTimetable(FeedScopedId tripId, LocalDate serviceDate) {
    var tt = getTripTimesForTrip(tripId, serviceDate);
    var pattern = getPatternForTrip(tripId);

    return TripTimesStringBuilder.encodeTripTimes(tt, pattern);
  }

  public String getScheduledTimetable(String tripId) {
    return getScheduledTimetable(id(tripId));
  }

  public String getScheduledTimetable(FeedScopedId tripId) {
    var pattern = getPatternForTrip(tripId);
    var tt = pattern.getScheduledTimetable().getTripTimes(tripId);

    return TripTimesStringBuilder.encodeTripTimes(tt, pattern);
  }

  // SIRI updates

  public UpdateResult applyEstimatedTimetableWithFuzzyMatcher(
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    return applyEstimatedTimetable(updates, true);
  }

  public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
    return applyEstimatedTimetable(updates, false);
  }

  // GTFS-RT updates

  public UpdateResult applyTripUpdate(GtfsRealtime.TripUpdate update) {
    return applyTripUpdates(List.of(update), FULL_DATASET);
  }

  public UpdateResult applyTripUpdate(
    GtfsRealtime.TripUpdate update,
    UpdateIncrementality incrementality
  ) {
    return applyTripUpdates(List.of(update), incrementality);
  }

  public UpdateResult applyTripUpdates(
    List<GtfsRealtime.TripUpdate> updates,
    UpdateIncrementality incrementality
  ) {
    Objects.requireNonNull(gtfsSource, "Test environment is configured for SIRI only");
    return gtfsSource.applyTripUpdates(
      null,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA,
      incrementality,
      updates,
      getFeedId()
    );
  }

  // private methods

  private UpdateResult applyEstimatedTimetable(
    List<EstimatedTimetableDeliveryStructure> updates,
    boolean fuzzyMatching
  ) {
    Objects.requireNonNull(siriSource, "Test environment is configured for GTFS-RT only");
    return getEstimatedTimetableHandler(fuzzyMatching).applyUpdate(updates, DIFFERENTIAL);
  }

  private Trip createTrip(String id, Route route, List<StopCall> stops) {
    var trip = Trip
      .of(id(id))
      .withRoute(route)
      .withHeadsign(I18NString.of("Headsign of %s".formatted(id)))
      .withServiceId(SERVICE_ID)
      .build();

    var tripOnServiceDate = TripOnServiceDate
      .of(trip.getId())
      .withTrip(trip)
      .withServiceDate(SERVICE_DATE)
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
      .withStopPattern(TransitModelForTest.stopPattern(stops.stream().map(StopCall::stop).toList()))
      .build();
    pattern.add(tripTimes);

    transitModel.addTripPattern(pattern.getId(), pattern);

    return trip;
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

  private record StopCall(RegularStop stop, int arrivalTime, int departureTime) {}
}
