package org.opentripplanner.transit.model._data;

import static org.opentripplanner.transit.model._data.TransitTestEnvironment.id;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class TransitTestEnvironmentBuilder {

  private final SiteRepository siteRepository;
  private final List<TripInput> tripInputs = new ArrayList<>();
  private final List<FlexTripInput> flexTripInputs = new ArrayList<>();
  private final Map<FeedScopedId, RegularStop> scheduledStopPointMapping = new HashMap<>();
  private final AtomicInteger serviceCodeCounter = new AtomicInteger();

  private final ZoneId timeZone;
  private final LocalDate defaultServiceDate;

  TransitTestEnvironmentBuilder(
    SiteRepository siteRepository,
    ZoneId timeZone,
    LocalDate defaultServiceDate
  ) {
    this.siteRepository = siteRepository;
    this.timeZone = timeZone;
    this.defaultServiceDate = defaultServiceDate;
  }

  public TransitTestEnvironmentBuilder addTrip(TripInput trip) {
    this.tripInputs.add(trip);
    return this;
  }

  public TransitTestEnvironment build() {
    var timetableRepository = new TimetableRepository(siteRepository, new Deduplicator());
    timetableRepository.initTimeZone(timeZone);

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    for (TripInput tripInput : tripInputs) {
      var t = createTrip(tripInput, timetableRepository);
      var serviceDates = Optional.ofNullable(tripInput.serviceDates()).orElseGet(() ->
        List.of(defaultServiceDate)
      );
      calendarServiceData.putServiceDatesForServiceId(t.getServiceId(), serviceDates);
    }
    for (FlexTripInput tripInput : flexTripInputs) {
      createFlexTrip(tripInput, timetableRepository);
    }

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
    return new TransitTestEnvironment(timetableRepository, defaultServiceDate);
  }

  public TransitTestEnvironmentBuilder addFlexTrip(FlexTripInput tripInput) {
    flexTripInputs.add(tripInput);
    return this;
  }

  public TransitTestEnvironmentBuilder addScheduledStopPointMapping(
    String scheduledStopPointId,
    String stopId
  ) {
    var stop = Objects.requireNonNull(siteRepository.getRegularStop(id(stopId)));
    scheduledStopPointMapping.put(id(scheduledStopPointId), stop);
    return this;
  }

  /**
   * The default service date is used when creating trips without a specified service date
   */
  public LocalDate defaultServiceDate() {
    return defaultServiceDate;
  }

  private Trip createTrip(TripInput tripInput, TimetableRepository timetableRepository) {
    var serviceDates = Optional.ofNullable(tripInput.serviceDates()).orElseGet(() ->
      List.of(defaultServiceDate)
    );
    var serviceId = generateServiceId(timetableRepository, serviceDates);
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
        return fixedStopTime(trip, i, stop.stopId(), stop.arrivalTime(), stop.departureTime());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    var stopPattern = TimetableRepositoryForTest.stopPattern(
      tripInput.stops().stream().map(call -> getStop(call.stopId())).toList()
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
    var serviceId = generateServiceId(timetableRepository, List.of(defaultServiceDate));
    final var trip = Trip.of(TimetableRepositoryForTest.id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(I18NString.of("Headsign of %s".formatted(tripInput.id())))
      .withServiceId(serviceId)
      .build();
    addTripOnServiceDate(timetableRepository, trip);

    var stopTimes = IntStream.range(0, tripInput.stops().size())
      .mapToObj(i -> {
        var stop = tripInput.stops().get(i);
        return flexStopTime(trip, i, stop.stopId(), stop.windowStart(), stop.windowEnd());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    var stopPattern = TimetableRepositoryForTest.stopPattern(
      tripInput.stops().stream().map(flexStop -> getAreaStop(flexStop.stopId())).toList()
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

  private void addTripOnServiceDate(TimetableRepository timetableRepository, Trip trip) {
    var tripOnServiceDate = TripOnServiceDate.of(trip.getId())
      .withTrip(trip)
      .withServiceDate(defaultServiceDate)
      .build();

    timetableRepository.addTripOnServiceDate(tripOnServiceDate);
  }

  private StopTime fixedStopTime(
    Trip trip,
    int stopSequence,
    String stopId,
    int arrivalTime,
    int departureTime
  ) {
    var st = new StopTime();
    st.setTrip(trip);
    st.setStopSequence(stopSequence);
    st.setStop(getStop(stopId));
    st.setArrivalTime(arrivalTime);
    st.setDepartureTime(departureTime);
    return st;
  }

  private StopTime flexStopTime(
    Trip trip,
    int stopSequence,
    String stopId,
    int windowStart,
    int windowEnd
  ) {
    var st = new StopTime();
    st.setTrip(trip);
    st.setStopSequence(stopSequence);
    st.setStop(getAreaStop(stopId));
    st.setFlexWindowStart(windowStart);
    st.setFlexWindowEnd(windowEnd);
    return st;
  }

  private StopLocation getStop(String stopId) {
    var stop = siteRepository.getRegularStop(id(stopId));
    Objects.requireNonNull(stop, "No stop found for id: " + stopId);
    return stop;
  }

  private StopLocation getAreaStop(String stopId) {
    var stop = siteRepository.getAreaStop(id(stopId));
    Objects.requireNonNull(stop, "No area stop found for id: " + stopId);
    return stop;
  }
}
