package org.opentripplanner.transit.model._data;

import static org.opentripplanner.transit.model._data.TransitTestEnvironment.id;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class TimetableRepositoryTestBuilder {

  private final List<TripInput> tripInputs = new ArrayList<>();
  private final List<FlexTripInput> flexTripInputs = new ArrayList<>();
  private final Map<FeedScopedId, RegularStop> scheduledStopPointMapping = new HashMap<>();
  private final AtomicInteger serviceCodeCounter = new AtomicInteger();

  private final LocalDate defaultServiceDate;
  private final ZoneId timeZone;

  TimetableRepositoryTestBuilder(ZoneId timeZone, LocalDate defaultServiceDate) {
    this.timeZone = timeZone;
    this.defaultServiceDate = defaultServiceDate;
  }

  public TimetableRepository build(SiteRepository siteRepository) {
    var timetableRepository = new TimetableRepository(siteRepository, new Deduplicator());
    timetableRepository.initTimeZone(timeZone);

    for (TripInput tripInput : tripInputs) {
      createTrip(tripInput, timetableRepository);
    }
    for (FlexTripInput tripInput : flexTripInputs) {
      createFlexTrip(tripInput, timetableRepository);
    }

    timetableRepository.addAgency(TimetableRepositoryForTest.AGENCY);

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
    return timetableRepository;
  }

  public TimetableRepositoryTestBuilder withTrip(TripInput trip) {
    this.tripInputs.add(trip);
    return this;
  }

  public TimetableRepositoryTestBuilder withFlexTrip(FlexTripInput tripInput) {
    flexTripInputs.add(tripInput);
    return this;
  }

  public TimetableRepositoryTestBuilder withScheduledStopPointMapping(
    FeedScopedId scheduledStopPointId,
    RegularStop stop
  ) {
    scheduledStopPointMapping.putAll(Map.of(scheduledStopPointId, stop));
    return this;
  }

  private Trip createTrip(TripInput tripInput, TimetableRepository timetableRepository
  ) {
    var serviceDates = Optional.ofNullable(tripInput.serviceDates())
      .orElse(List.of(defaultServiceDate));

    var serviceId = createServiceId(timetableRepository, serviceDates);
    var trip = Trip.of(id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(tripInput.headsign() == null ? null : tripInput.headsign())
      .withServiceId(serviceId)
      .build();

    if (tripInput.tripOnServiceDateId() != null) {
      if (serviceDates.size() != 1) {
        throw new IllegalArgumentException("Multiple service dates can't be used with TripOnServiceDate");
      }
      addTripOnServiceDate(timetableRepository, trip, serviceDates.getFirst(), tripInput.tripOnServiceDateId());
    }

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

  private FeedScopedId createServiceId(
    TimetableRepository timetableRepository,
    List<LocalDate> serviceDates
  ) {
    var serviceId = id(
      serviceDates.stream().map(LocalDate::toString).collect(Collectors.joining("|"))
    );
    timetableRepository.getServiceCodes().put(serviceId, serviceCodeCounter.getAndIncrement());

    var calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(serviceId, serviceDates);
    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );
    return serviceId;
  }

  private Trip createFlexTrip(FlexTripInput tripInput, TimetableRepository timetableRepository
  ) {
    var serviceDates = List.of(defaultServiceDate);
    var serviceId = createServiceId(timetableRepository, serviceDates);
    final var trip = Trip.of(TimetableRepositoryForTest.id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(I18NString.of("Headsign of %s".formatted(tripInput.id())))
      .withServiceId(serviceId)
      .build();

    var stopTimes = IntStream.range(0, tripInput.stops().size())
      .mapToObj(i -> {
        var stop = tripInput.stops().get(i);
        return flexStopTime(trip, i, stop.stop(), stop.windowStart(), stop.windowEnd());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    var stopPattern = TimetableRepositoryForTest.stopPattern(
      tripInput.stops().stream().map(FlexTripInput.FlexStop::stop).toList()
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

  private void addTripOnServiceDate(TimetableRepository timetableRepository, Trip trip, LocalDate serviceDate, String id) {
    var tripOnServiceDate = TripOnServiceDate.of(id(id))
      .withTrip(trip)
      .withServiceDate(serviceDate)
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
