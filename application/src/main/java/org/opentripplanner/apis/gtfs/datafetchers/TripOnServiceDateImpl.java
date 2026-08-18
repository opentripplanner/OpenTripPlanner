package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.RealTimeTripStateModel;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.ReplacedByRelation;
import org.opentripplanner.transit.model.network.ReplacementForRelation;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Timetable;
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
  public DataFetcher<TripTimeOnDate> end() {
    return environment -> {
      var arguments = getFromTripTimesArguments(environment);
      if (arguments.timetable() == null) {
        return null;
      }
      return TripTimeOnDate.lastFromTripTimes(
        arguments.timetable(),
        arguments.trip(),
        arguments.serviceDate(),
        arguments.midnight()
      );
    };
  }

  @Override
  public DataFetcher<Boolean> isReplacement() {
    return environment ->
      getTransitService(environment)
        .getReplacementHelper()
        .isReplacementTripOnServiceDate(getSource(environment));
  }

  @Override
  public DataFetcher<RealTimeTripStateModel> realTimeTripState() {
    return environment -> {
      var transitService = getTransitService(environment);
      var tripOnServiceDate = getSource(environment);
      return transitService
        .findTripTimes(tripOnServiceDate.getTrip(), tripOnServiceDate.getServiceDate())
        .map(tripTimes -> {
          if (tripTimes.isDeleted()) {
            throw new RuntimeException(
              "Trip has been deleted. this should not be exposed to the API and is probably a bug"
            );
          }
          return new RealTimeTripStateModel(
            tripTimes.isAdded(),
            tripTimes.isCanceled(),
            tripTimes.isTimesModified(),
            tripTimes.isTripPatternModified(),
            tripTimes.hasAnyUpdates()
          );
        })
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<ReplacedByRelation>> replacedByRelation() {
    return environment ->
      getTransitService(environment).getReplacementHelper().getReplacedBy(getSource(environment));
  }

  @Override
  public DataFetcher<Iterable<ReplacementForRelation>> replacementForRelation() {
    return environment ->
      getTransitService(environment)
        .getReplacementHelper()
        .getReplacementFor(getSource(environment));
  }

  @Override
  public DataFetcher<TripTimeOnDate> start() {
    return environment -> {
      var arguments = getFromTripTimesArguments(environment);
      if (arguments.timetable() == null) {
        return null;
      }
      return TripTimeOnDate.firstFromTripTimes(
        arguments.timetable(),
        arguments.trip(),
        arguments.serviceDate(),
        arguments.midnight()
      );
    };
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stopCalls() {
    return environment -> {
      var arguments = getFromTripTimesArguments(environment);
      if (arguments.timetable() == null) {
        return List.of();
      }
      return TripTimeOnDate.fromTripTimesWithScheduleFallback(
        arguments.timetable(),
        arguments.trip(),
        arguments.serviceDate(),
        arguments.midnight()
      );
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
    TripPattern tripPattern = transitService.findPattern(trip, serviceDate);
    // no matching pattern found
    if (tripPattern == null) {
      return null;
    }

    return transitService.findTimetable(tripPattern, serviceDate);
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

  private FromTripTimesArguments getFromTripTimesArguments(DataFetchingEnvironment environment) {
    TransitService transitService = getTransitService(environment);
    Trip trip = getTrip(environment);
    var serviceDate = getSource(environment).getServiceDate();

    Instant midnight = ServiceDateUtils.asStartOfService(
      serviceDate,
      transitService.getTimeZone()
    ).toInstant();
    Timetable timetable = getTimetable(environment, trip, serviceDate);
    return new FromTripTimesArguments(trip, serviceDate, midnight, timetable);
  }

  private record FromTripTimesArguments(
    Trip trip,
    LocalDate serviceDate,
    Instant midnight,
    @Nullable Timetable timetable
  ) {}
}
