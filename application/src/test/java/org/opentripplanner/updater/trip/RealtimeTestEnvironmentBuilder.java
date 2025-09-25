package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.SERVICE_DATE;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.TIME_ZONE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.trip.FlexTripInput.FlexStop;

public class RealtimeTestEnvironmentBuilder {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private final List<StopLocation> stops = new ArrayList<>();
  private final HashMap<String, Station> stations = new HashMap<>();
  private final List<TripInput> tripInputs = new ArrayList<>();
  private final List<FlexTripInput> flexTripInputs = new ArrayList<>();
  private final Map<FeedScopedId, RegularStop> scheduledStopPointMapping = new HashMap<>();
  private final AtomicInteger serviceCodeCounter = new AtomicInteger();

  RealtimeTestEnvironmentBuilder() {}

  public RealtimeTestEnvironmentBuilder addTrip(TripInput trip) {
    this.tripInputs.add(trip);
    return this;
  }

  public RealtimeTestEnvironment build() {
    for (var stop : stops) {
      switch (stop) {
        case RegularStop rs -> testModel.siteRepositoryBuilder().withRegularStop(rs);
        case AreaStop as -> testModel.siteRepositoryBuilder().withAreaStop(as);
        default -> throw new IllegalStateException("Unexpected value: " + stop);
      }
    }
    for (var station : stations.values()) {
      testModel.siteRepositoryBuilder().withStation(station);
    }
    var timetableRepository = new TimetableRepository(
      testModel.siteRepositoryBuilder().build(),
      new Deduplicator()
    );

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    for (TripInput tripInput : tripInputs) {
      var t = createTrip(tripInput, timetableRepository);
      calendarServiceData.putServiceDatesForServiceId(t.getServiceId(), tripInput.serviceDates());
    }
    for (FlexTripInput tripInput : flexTripInputs) {
      createFlexTrip(tripInput, timetableRepository);
    }

    timetableRepository.initTimeZone(TIME_ZONE);
    timetableRepository.addAgency(TimetableRepositoryForTest.AGENCY);

    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );
    timetableRepository
      .getAllTripPatterns()
      .forEach(pattern -> {
        pattern.getScheduledTimetable().setServiceCodes(timetableRepository.getServiceCodes());
      });

    timetableRepository
      .getAllTripPatterns()
      .forEach(pattern -> {
        pattern.getScheduledTimetable().setServiceCodes(timetableRepository.getServiceCodes());
      });

    timetableRepository.addScheduledStopPointMapping(scheduledStopPointMapping);

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

  public AreaStop areaStop(String id) {
    var stop = testModel.areaStop(id).build();
    stops.add(stop);
    return stop;
  }

  public RealtimeTestEnvironmentBuilder addFlexTrip(FlexTripInput tripInput) {
    flexTripInputs.add(tripInput);
    return this;
  }

  public RealtimeTestEnvironmentBuilder addScheduledStopPointMapping(
    Map<FeedScopedId, RegularStop> mapping
  ) {
    scheduledStopPointMapping.putAll(mapping);
    return this;
  }

  private Trip createTrip(TripInput tripInput, TimetableRepository timetableRepository) {
    var serviceId = generateServiceId(timetableRepository, tripInput.serviceDates());
    var trip = Trip.of(id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(tripInput.headsign() == null ? null : tripInput.headsign())
      .withServiceId(serviceId)
      .build();

    addTripOnServiceDate(timetableRepository, trip);

    if (tripInput.route().getOperator() != null) {
      timetableRepository.addOperators(List.of(tripInput.route().getOperator()));
    }

    var stopTimes = IntStream.range(0, tripInput.stops().size())
      .mapToObj(i -> {
        var stop = tripInput.stops().get(i);
        return fixedStopTime(trip, i, stop.stop(), stop.arrivalTime(), stop.departureTime());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    var stopPattern = TimetableRepositoryForTest.stopPattern(
      tripInput.stops().stream().map(TripInput.StopCall::stop).toList()
    );

    var existingPatterns = timetableRepository
      .getAllTripPatterns()
      .stream()
      .filter(p -> p.getStopPattern().equals(stopPattern))
      .toList();

    if (existingPatterns.size() > 1) {
      throw new RuntimeException(
        "Multiple patterns found for stop pattern %s. This indicates an error during test setup.".formatted(
            stopPattern
          )
      );
    } else if (existingPatterns.size() == 1) {
      var pattern = existingPatterns.getFirst();
      var newPattern = pattern
        .copy()
        .withScheduledTimeTableBuilder(b -> b.addTripTimes(tripTimes))
        .build();
      timetableRepository.addTripPattern(pattern.getId(), newPattern);
    } else {
      addNewPattern(tripInput.id(), tripInput.route(), stopPattern, tripTimes, timetableRepository);
    }

    return trip;
  }

  private FeedScopedId generateServiceId(
    TimetableRepository timetableRepository,
    List<LocalDate> localDates
  ) {
    var serviceId = id(
      localDates.stream().map(LocalDate::toString).collect(Collectors.joining("|"))
    );
    timetableRepository.getServiceCodes().put(serviceId, serviceCodeCounter.getAndIncrement());
    return serviceId;
  }

  private Trip createFlexTrip(FlexTripInput tripInput, TimetableRepository timetableRepository) {
    var serviceId = generateServiceId(timetableRepository, List.of(SERVICE_DATE));
    final var trip = Trip.of(TimetableRepositoryForTest.id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(I18NString.of("Headsign of %s".formatted(tripInput.id())))
      .withServiceId(serviceId)
      .build();
    addTripOnServiceDate(timetableRepository, trip);

    var stopTimes = IntStream.range(0, tripInput.stops().size())
      .mapToObj(i -> {
        var stop = tripInput.stops().get(i);
        return flexStopTime(trip, i, stop.stop(), stop.windowStart(), stop.windowEnd());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    var stopPattern = TimetableRepositoryForTest.stopPattern(
      tripInput.stops().stream().map(FlexStop::stop).toList()
    );

    addNewPattern(tripInput.id(), tripInput.route(), stopPattern, tripTimes, timetableRepository);
    var flexTrip = UnscheduledTrip.of(trip.getId()).withTrip(trip).withStopTimes(stopTimes).build();
    timetableRepository.addFlexTrip(flexTrip.getId(), flexTrip);
    return trip;
  }

  private static void addNewPattern(
    String tripInput,
    Route tripInput1,
    StopPattern stopPattern,
    TripTimes tripTimes,
    TimetableRepository timetableRepository
  ) {
    var pattern = TimetableRepositoryForTest.tripPattern(tripInput + "Pattern", tripInput1)
      .withStopPattern(stopPattern)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    timetableRepository.addTripPattern(pattern.getId(), pattern);
  }

  private static void addTripOnServiceDate(TimetableRepository timetableRepository, Trip trip) {
    var tripOnServiceDate = TripOnServiceDate.of(trip.getId())
      .withTrip(trip)
      .withServiceDate(SERVICE_DATE)
      .build();

    timetableRepository.addTripOnServiceDate(tripOnServiceDate);
  }

  private static StopTime fixedStopTime(
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

  private static StopTime flexStopTime(
    Trip trip,
    int stopSequence,
    StopLocation stop,
    int windowStart,
    int windowEnd
  ) {
    var st = new StopTime();
    st.setTrip(trip);
    st.setStopSequence(stopSequence);
    st.setStop(stop);
    st.setFlexWindowStart(windowStart);
    st.setFlexWindowEnd(windowEnd);
    return st;
  }
}
