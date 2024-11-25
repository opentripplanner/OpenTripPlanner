package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class TripOnServiceDateImpl implements GraphQLDataFetchers.GraphQLTripOnServiceDate {

  @Override
  public DataFetcher<LocalDate> serviceDate() {
    return env -> getSource(env).getServiceDate();
  }

  @Override
  public DataFetcher<Object> end() {
    return environment -> {
      final Result result = getResult(environment);
      if (result.timetable() == null) {
        return null;
      }
      return TripTimeOnDate.lastFromTripTimes(result.timetable(), result.trip(), result.serviceDate(), result.midnight());
    };
  }

  private Result getResult(DataFetchingEnvironment environment) {
    TransitService transitService = getTransitService(environment);
    Trip trip = getTrip(environment);
    var serviceDate = getSource(environment).getServiceDate();

    Instant midnight = ServiceDateUtils
      .asStartOfService(serviceDate, transitService.getTimeZone())
      .toInstant();
    Timetable timetable = getTimetable(environment, trip, serviceDate);
    return new Result(trip, serviceDate, midnight, timetable);
  }

  private record Result(Trip trip, LocalDate serviceDate, Instant midnight, @Nullable Timetable timetable) {
  }

  @Override
  public DataFetcher<Object> start() {
    return environment -> {
      var result = getResult(environment);
      if (result.timetable() == null) {
        return null;
      }
      return TripTimeOnDate.firstFromTripTimes(result.timetable, result.trip, result.serviceDate, result.midnight);
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stopCalls() {
    return environment -> {
      var result = getResult(environment);
      if (result.timetable == null) {
        return List.of();
      }
      return TripTimeOnDate
        .fromTripTimes(result.timetable, result.trip, result.serviceDate, result.midnight)
        .stream()
        .map(Object.class::cast)
        .toList();
    };
  }

  @Override
  public DataFetcher<Trip> trip() {
    return this::getTrip;
  }

  @Nullable
  private Timetable getTimetable(
    DataFetchingEnvironment environment,
    Trip trip,
    LocalDate serviceDate
  ) {
    TransitService transitService = getTransitService(environment);
    TripPattern tripPattern = transitService.getPatternForTrip(trip, serviceDate);
    // no matching pattern found
    if (tripPattern == null) {
      return null;
    }

    return transitService.getTimetableForTripPattern(tripPattern, serviceDate);
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private Trip getTrip(DataFetchingEnvironment environment) {
    return getSource(environment).getTrip();
  }

  private TripOnServiceDate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
