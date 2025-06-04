package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.SERVICE_DATE;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.TIME_ZONE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TimetableRepository;

public class RealtimeTestEnvironmentBuilder {

  private static final FeedScopedId SERVICE_ID = TimetableRepositoryForTest.id("CAL_1");
  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private final List<RegularStop> stops = new ArrayList<>();
  private final HashMap<String, Station> stations = new HashMap<>();
  private final List<TripInput> tripInputs = new ArrayList<>();

  public RealtimeTestEnvironmentBuilder addTrip(TripInput trip) {
    this.tripInputs.add(trip);
    return this;
  }

  public RealtimeTestEnvironment build() {
    for (var stop : stops) {
      testModel.siteRepositoryBuilder().withRegularStop(stop);
    }
    for (var station : stations.values()) {
      testModel.siteRepositoryBuilder().withStation(station);
    }
    var timetableRepository = new TimetableRepository(
      testModel.siteRepositoryBuilder().build(),
      new Deduplicator()
    );

    for (TripInput tripInput : tripInputs) {
      createTrip(tripInput, timetableRepository);
    }

    timetableRepository.initTimeZone(TIME_ZONE);
    timetableRepository.addAgency(TimetableRepositoryForTest.AGENCY);

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      SERVICE_ID,
      List.of(SERVICE_DATE.minusDays(1), SERVICE_DATE, SERVICE_DATE.plusDays(1))
    );
    timetableRepository.getServiceCodes().put(SERVICE_ID, 0);
    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );

    timetableRepository.index();
    return new RealtimeTestEnvironment(timetableRepository, SERVICE_DATE, TIME_ZONE);
  }

  public RealtimeTestEnvironmentBuilder withStops(String... stopIds) {
    this.stops.addAll(Arrays.stream(stopIds).map(id -> testModel.stop(id).build()).toList());
    return this;
  }

  public RegularStop stop(String id) {
    var stop = testModel.stop(id).build();
    stops.add(stop);
    return stop;
  }

  public RegularStop stopAtStation(String stopId, String stationId) {
    var dflt = testModel.station(stationId).build();
    var station = stations.getOrDefault(stationId, dflt);
    var stop = testModel.stop(stopId).withParentStation(station).build();

    stops.add(stop);
    stations.put(stationId, station);
    return stop;
  }

  static void createTrip(TripInput tripInput, TimetableRepository timetableRepository) {
    var trip = Trip.of(id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(tripInput.headsign() == null ? null : I18NString.of(tripInput.headsign()))
      .withServiceId(SERVICE_ID)
      .build();

    var tripOnServiceDate = TripOnServiceDate.of(trip.getId())
      .withTrip(trip)
      .withServiceDate(SERVICE_DATE)
      .build();

    timetableRepository.addTripOnServiceDate(tripOnServiceDate);

    if (tripInput.route().getOperator() != null) {
      timetableRepository.addOperators(List.of(tripInput.route().getOperator()));
    }

    var stopTimes = IntStream.range(0, tripInput.stops().size())
      .mapToObj(i -> {
        var stop = tripInput.stops().get(i);
        return createStopTime(trip, i, stop.stop(), stop.arrivalTime(), stop.departureTime());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    final TripPattern pattern = TimetableRepositoryForTest.tripPattern(
      tripInput.id() + "Pattern",
      tripInput.route()
    )
      .withStopPattern(
        TimetableRepositoryForTest.stopPattern(
          tripInput.stops().stream().map(TripInput.StopCall::stop).toList()
        )
      )
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    timetableRepository.addTripPattern(pattern.getId(), pattern);
  }

  private static StopTime createStopTime(
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
}
