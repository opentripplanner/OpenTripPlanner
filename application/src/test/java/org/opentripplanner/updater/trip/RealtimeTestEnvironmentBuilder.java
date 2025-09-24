package org.opentripplanner.updater.trip;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
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
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.trip.FlexTripInput.FlexStop;

public class RealtimeTestEnvironmentBuilder {

  private static final WgsCoordinate ANY_COORDINATE = new WgsCoordinate(60.0, 10.0);
  private static final Polygon ANY_POLYGON = GeometryUtils.getGeometryFactory()
    .createPolygon(
      new Coordinate[] {
        Coordinates.of(61.0, 10.0),
        Coordinates.of(61.0, 12.0),
        Coordinates.of(60.0, 11.0),
        Coordinates.of(61.0, 10.0),
      }
    );

  private final SiteRepositoryBuilder siteRepositoryBuilder = SiteRepository.of();
  private final List<StopLocation> stops = new ArrayList<>();
  private final HashMap<String, Station> stations = new HashMap<>();
  private final List<TripInput> tripInputs = new ArrayList<>();
  private final List<FlexTripInput> flexTripInputs = new ArrayList<>();
  private final Map<FeedScopedId, RegularStop> scheduledStopPointMapping = new HashMap<>();

  private final String defaultFeedId;
  private final FeedScopedId defaultServiceId;
  private final ZoneId timeZone;
  private final LocalDate defaultServiceDate;

  RealtimeTestEnvironmentBuilder(String defaultFeedId, ZoneId timeZone, LocalDate defaultServiceDate) {
    this.defaultFeedId = defaultFeedId;
    this.timeZone = timeZone;
    this.defaultServiceId = id("CAL_1");
    this.defaultServiceDate = defaultServiceDate;
  }

  public RealtimeTestEnvironmentBuilder addTrip(TripInput trip) {
    this.tripInputs.add(trip);
    return this;
  }

  public RealtimeTestEnvironment build() {
    for (var stop : stops) {
      switch (stop) {
        case RegularStop rs -> siteRepositoryBuilder.withRegularStop(rs);
        case AreaStop as -> siteRepositoryBuilder.withAreaStop(as);
        default -> throw new IllegalStateException("Unexpected value: " + stop);
      }
    }
    for (var station : stations.values()) {
      siteRepositoryBuilder.withStation(station);
    }
    var timetableRepository = new TimetableRepository(
      siteRepositoryBuilder.build(),
      new Deduplicator()
    );

    for (TripInput tripInput : tripInputs) {
      createTrip(tripInput, timetableRepository);
    }
    for (FlexTripInput tripInput : flexTripInputs) {
      createFlexTrip(tripInput, timetableRepository);
    }

    timetableRepository.initTimeZone(timeZone);
    timetableRepository.addAgency(TimetableRepositoryForTest.AGENCY);

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      defaultServiceId,
      List.of(defaultServiceDate.minusDays(1), defaultServiceDate, defaultServiceDate.plusDays(1))
    );

    timetableRepository.getServiceCodes().put(defaultServiceId, 0);
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
    return new RealtimeTestEnvironment(timetableRepository, defaultServiceDate, timeZone);
  }

  public RealtimeTestEnvironmentBuilder withStops(String... stopIds) {
    Arrays.stream(stopIds).forEach(this::stop);
    return this;
  }

  public RegularStop stop(String id) {
    var stop = stopBuilder(id).build();
    stops.add(stop);
    return stop;
  }

  /**
   * Add a stop at a station.  The station will be created if it does not already exist.
   * @param stopId
   * @param stationId
   * @return
   */
  public RegularStop stopAtStation(String stopId, String stationId) {
    var station = stations.get(stationId);
    if (station == null) {
      station = Station.of(id(stationId))
        .withName(new NonLocalizedString(stationId))
        .withCode(stationId)
        .withCoordinate(ANY_COORDINATE)
        .withDescription(new NonLocalizedString("Station " + stationId))
        .withPriority(StopTransferPriority.ALLOWED)
        .build();
    }

    var stop = stopBuilder(stopId).withParentStation(station).build();

    stops.add(stop);
    stations.put(stationId, station);
    return stop;
  }

  public AreaStop areaStop(String id) {
    var stop = siteRepositoryBuilder
      .areaStop(id(id))
      .withName(new NonLocalizedString(id))
      .withGeometry(ANY_POLYGON)
      .build();
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

  private void createTrip(TripInput tripInput, TimetableRepository timetableRepository) {
    var trip = Trip.of(id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(tripInput.headsign() == null ? null : tripInput.headsign())
      .withServiceId(defaultServiceId)
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
  }

  private Trip createFlexTrip(FlexTripInput tripInput, TimetableRepository timetableRepository) {
    final var trip = trip(tripInput.id(), tripInput.route());
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

  private Trip trip(String tripInput, Route tripInput1) {
    return Trip.of(id(tripInput))
      .withRoute(tripInput1)
      .withHeadsign(I18NString.of("Headsign of %s".formatted(tripInput)))
      .withServiceId(defaultServiceId)
      .build();
  }

  private void addTripOnServiceDate(TimetableRepository timetableRepository, Trip trip) {
    var tripOnServiceDate = TripOnServiceDate.of(trip.getId())
      .withTrip(trip)
      .withServiceDate(defaultServiceDate)
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

  private RegularStopBuilder stopBuilder(String id) {
    return siteRepositoryBuilder
      .regularStop(id(id))
      .withName(new NonLocalizedString(id))
      .withCode(id)
      .withCoordinate(ANY_COORDINATE);
  }

  private FeedScopedId id(String id) {
    return new FeedScopedId(defaultFeedId, id);
  }
}
