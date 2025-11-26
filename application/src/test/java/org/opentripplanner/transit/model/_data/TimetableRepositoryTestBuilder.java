package org.opentripplanner.transit.model._data;

import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.AgencyBuilder;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.organization.OperatorBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripBuilder;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * A builder for timetable repository to simplify setting up timetable entities for tests
 */
public class TimetableRepositoryTestBuilder {

  private final List<Agency> agencies = new ArrayList<>();
  private final List<Operator> operators = new ArrayList<>();

  private final List<TripOnServiceDate> tripOnServiceDates = new ArrayList<>();
  private final List<UnscheduledTrip> flexTrips = new ArrayList<>();

  private final Map<TripPatternKey, TripPattern> tripPatterns = new HashMap<>();
  private final Map<String, ServiceCode> serviceCodes = new HashMap<>();

  private final Map<FeedScopedId, RegularStop> scheduledStopPointMapping = new HashMap<>();

  private final LocalDate defaultServiceDate;
  private final ZoneId timeZone;

  private final Agency defaultAgency;
  private final Route defaultRoute;

  TimetableRepositoryTestBuilder(ZoneId timeZone, LocalDate defaultServiceDate) {
    this.timeZone = timeZone;
    this.defaultServiceDate = defaultServiceDate;

    defaultAgency = agency("Agency1");
    defaultRoute = route("Route1");
  }

  public TimetableRepository build(SiteRepository siteRepository) {
    var timetableRepository = new TimetableRepository(siteRepository, new Deduplicator());
    timetableRepository.initTimeZone(timeZone);

    for (var agency : agencies) {
      timetableRepository.addAgency(agency);
    }

    timetableRepository.addOperators(operators);

    for (TripPattern tripPattern : tripPatterns.values()) {
      timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
    }

    for (var flexTrip : flexTrips) {
      timetableRepository.addFlexTrip(flexTrip.getId(), flexTrip);
    }

    for (TripOnServiceDate tripOnServiceDate : tripOnServiceDates) {
      timetableRepository.addTripOnServiceDate(tripOnServiceDate);
    }

    var calendarServiceData = new CalendarServiceData();
    int serviceCodeCounter = 0;
    for (var serviceCode : serviceCodes.values()) {
      calendarServiceData.putServiceDatesForServiceId(serviceCode.id(), serviceCode.serviceDates());
      timetableRepository.getServiceCodes().put(serviceCode.id(), serviceCodeCounter);
      serviceCodeCounter += 1;
    }
    timetableRepository.updateCalendarServiceData(calendarServiceData);

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

  public Agency agency(String id) {
    return agency(id, b -> {});
  }

  public Agency agency(String id, Consumer<AgencyBuilder> customizer) {
    var builder = Agency.of(id(id))
      .withName("Agency Test")
      .withTimezone(timeZone.getId())
      .withUrl("https://www." + id + ".com");
    if (customizer != null) {
      customizer.accept(builder);
    }
    var agency = builder.build();
    agencies.add(agency);
    return agency;
  }

  public Route route(String id) {
    return route(id, b -> {});
  }

  public Route route(String id, Consumer<RouteBuilder> customizer) {
    var builder = Route.of(id(id))
      .withAgency(defaultAgency)
      .withShortName("R" + id)
      .withMode(TransitMode.BUS);
    if (customizer != null) {
      customizer.accept(builder);
    }
    // Routes aren't stored explicitly in the timetable repository so we don't have a collection for these
    return builder.build();
  }

  public Route route(String id, Operator operator) {
    return route(id, builder -> builder.withOperator(operator));
  }

  public Operator operator(String operatorId) {
    var operator = Operator.of(id(operatorId)).withName(operatorId + " name").build();
    operators.add(operator);
    return operator;
  }

  public Operator operator(String operatorId, Consumer<OperatorBuilder> customizer) {
    var builder = Operator.of(id(operatorId)).withName(operatorId + " name");
    if (customizer != null) {
      customizer.accept(builder);
    }
    var operator = builder.build();
    operators.add(operator);
    return operator;
  }

  public Trip trip(TripInput tripInput) {
    return trip(tripInput, null);
  }

  public Trip trip(TripInput tripInput, Consumer<TripBuilder> customizer) {
    var serviceDates = Optional.ofNullable(tripInput.serviceDates()).orElse(
      List.of(defaultServiceDate)
    );

    var serviceId = getOrCreateServiceId(serviceDates);

    var route = Optional.ofNullable(tripInput.route()).orElse(defaultRoute);

    var tripBuilder = Trip.of(id(tripInput.id()))
      .withRoute(route)
      .withHeadsign(tripInput.headsign())
      .withServiceId(serviceId);
    if (customizer != null) {
      customizer.accept(tripBuilder);
    }
    var trip = tripBuilder.build();

    var stopPattern = stopPattern(tripInput.stopLocations());
    var tripPattern = getOrCreateTripPattern(stopPattern, route);

    var stopTimes = tripInput.stopTimes(trip);
    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    addTripTimesToPattern(tripPattern, tripTimes);

    if (tripInput.tripOnServiceDateId() != null) {
      if (serviceDates.size() != 1) {
        throw new IllegalArgumentException(
          "Multiple service dates can't be used with TripOnServiceDate"
        );
      }
      addTripOnServiceDate(trip, serviceDates.getFirst(), tripInput.tripOnServiceDateId());
    }

    if (tripInput.isFlex()) {
      var flexTrip = UnscheduledTrip.of(trip.getId())
        .withTrip(trip)
        .withStopTimes(stopTimes)
        .build();
      flexTrips.add(flexTrip);
    }

    return trip;
  }

  public TimetableRepositoryTestBuilder addScheduledStopPointMapping(
    FeedScopedId scheduledStopPointId,
    RegularStop stop
  ) {
    scheduledStopPointMapping.putAll(Map.of(scheduledStopPointId, stop));
    return this;
  }

  private void addTripTimesToPattern(TripPattern tripPattern, TripTimes tripTimes) {
    var newPattern = tripPattern
      .copy()
      .withScheduledTimeTableBuilder(b -> b.addTripTimes(tripTimes))
      .build();
    tripPatterns.put(TripPatternKey.of(newPattern), newPattern);
  }

  private TripPattern getOrCreateTripPattern(StopPattern stopPattern, Route route) {
    var key = new TripPatternKey(stopPattern, route);
    var pattern = tripPatterns.get(key);
    if (pattern != null) {
      return pattern;
    }

    var id = "Pattern" + (tripPatterns.size() + 1);
    var newPattern = TripPattern.of(id(id)).withRoute(route).withStopPattern(stopPattern).build();
    tripPatterns.put(key, newPattern);
    return newPattern;
  }

  private FeedScopedId getOrCreateServiceId(List<LocalDate> serviceDates) {
    var key = serviceDates
      .stream()
      .map(LocalDate::toString)
      .sorted()
      .collect(Collectors.joining("|"));
    var serviceCode = serviceCodes.get(key);
    if (serviceCode != null) {
      return serviceCode.id();
    }

    var newServiceCode = new ServiceCode(serviceDates, id(key));
    serviceCodes.put(key, newServiceCode);
    return newServiceCode.id();
  }

  private void addTripOnServiceDate(Trip trip, LocalDate serviceDate, String id) {
    var tripOnServiceDate = TripOnServiceDate.of(id(id))
      .withTrip(trip)
      .withServiceDate(serviceDate)
      .build();
    tripOnServiceDates.add(tripOnServiceDate);
  }

  private static StopPattern stopPattern(List<StopLocation> stops) {
    var builder = StopPattern.create(stops.size());
    for (int i = 0; i < stops.size(); i++) {
      builder.stops.with(i, stops.get(i));
      builder.pickups.with(i, PickDrop.SCHEDULED);
      builder.dropoffs.with(i, PickDrop.SCHEDULED);
    }
    return builder.build();
  }

  // We use route and stopPattern as key for tripPattern. This isn't enough in general so we
  // can extend it to more attributes from the trip pattern as needed.
  private record TripPatternKey(StopPattern stopPattern, Route route) {
    public static TripPatternKey of(TripPattern tripPattern) {
      return new TripPatternKey(tripPattern.getStopPattern(), tripPattern.getRoute());
    }
  }

  private record ServiceCode(List<LocalDate> serviceDates, FeedScopedId id) {}
}
