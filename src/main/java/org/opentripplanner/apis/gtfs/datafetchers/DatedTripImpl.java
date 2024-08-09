package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.stopTimeToInt;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.ArrivalDepartureTime;
import org.opentripplanner.ext.restapi.mapping.LocalDateMapper;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.DatedTrip;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

public class DatedTripImpl implements GraphQLDataFetchers.GraphQLDatedTrip {

  @Override
  public DataFetcher<LocalDate> date() {
    return env -> getSource(env).serviceDate();
  }

  @Override
  public DataFetcher<ArrivalDepartureTime> end() {
    return env -> {
      var tripTimes = getTripTimes(env);
      if (tripTimes == null) {
        return null;
      }
      var stopIndex = tripTimes.getNumStops() - 1;
      var scheduledTime = getZonedDateTime(env, tripTimes.getScheduledArrivalTime(stopIndex));
      if (scheduledTime == null) {
        return null;
      }
      return tripTimes.isRealTimeUpdated(stopIndex)
        ? ArrivalDepartureTime.of(scheduledTime, tripTimes.getArrivalDelay(stopIndex))
        : ArrivalDepartureTime.ofStatic(scheduledTime);
    };
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return env ->
      new Relay.ResolvedGlobalId(
        "DatedTrip",
        getSource(env).trip().getId().toString() +
        ';' +
        LocalDateMapper.mapToApi(getSource(env).serviceDate())
      );
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return this::getTripPattern;
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).trip().getRoute();
  }

  @Override
  public DataFetcher<ArrivalDepartureTime> start() {
    return env -> {
      var tripTimes = getTripTimes(env);
      if (tripTimes == null) {
        return null;
      }
      var scheduledTime = getZonedDateTime(env, tripTimes.getScheduledDepartureTime(0));
      if (scheduledTime == null) {
        return null;
      }
      return tripTimes.isRealTimeUpdated(0)
        ? ArrivalDepartureTime.of(scheduledTime, tripTimes.getDepartureDelay(0))
        : ArrivalDepartureTime.ofStatic(scheduledTime);
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return this::getStops;
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stoptimes() {
    return environment -> {
      TransitService transitService = getTransitService(environment);
      Trip trip = getSource(environment).trip();
      var serviceDate = getSource(environment).serviceDate();

      Instant midnight = ServiceDateUtils
        .asStartOfService(serviceDate, transitService.getTimeZone())
        .toInstant();
      Timetable timetable = getTimetable(environment, trip, serviceDate);
      if (timetable == null) {
        return List.of();
      }
      return TripTimeOnDate.fromTripTimes(timetable, trip, serviceDate, midnight);
    };
  }

  @Override
  public DataFetcher<String> tripHeadsign() {
    return environment ->
      org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
        getSource(environment).trip().getHeadsign(),
        environment
      );
  }

  @Override
  public DataFetcher<String> tripShortName() {
    return environment -> getSource(environment).trip().getShortName();
  }

  private List<Object> getStops(DataFetchingEnvironment environment) {
    TripPattern tripPattern = getTripPattern(environment);
    if (tripPattern == null) {
      return List.of();
    }
    return List.copyOf(tripPattern.getStops());
  }

  private TripPattern getTripPattern(DataFetchingEnvironment environment) {
    return getTransitService(environment).getPatternForTrip(getSource(environment).trip());
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

  @Nullable
  private TripTimes getTripTimes(DataFetchingEnvironment environment) {
    Trip trip = getSource(environment).trip();
    var serviceDate = getSource(environment).serviceDate();
    var timetable = getTimetable(environment, trip, serviceDate);
    if (timetable == null) {
      return null;
    }
    return timetable.getTripTimes(trip);
  }

  private ZonedDateTime getZonedDateTime(DataFetchingEnvironment environment, int time) {
    var fixedTime = stopTimeToInt(time);
    if (fixedTime == null) {
      return null;
    }
    var serviceDate = getSource(environment).serviceDate();
    TransitService transitService = getTransitService(environment);
    return ServiceDateUtils.toZonedDateTime(serviceDate, transitService.getTimeZone(), fixedTime);
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private DatedTrip getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
