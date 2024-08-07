package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.restapi.mapping.LocalDateMapper;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.DatedTrip;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

public class DatedTripImpl implements GraphQLDataFetchers.GraphQLDatedTrip {

  @Override
  public DataFetcher<LocalDate> date() {
    return env -> getSource(env).serviceDate();
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
  public DataFetcher<Iterable<Object>> stops() {
    return this::getStops;
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stoptimes() {
    return environment -> {
      TransitService transitService = getTransitService(environment);
      Trip trip = getSource(environment).trip();

      var serviceDate = getSource(environment).serviceDate();
      TripPattern tripPattern = transitService.getPatternForTrip(trip, serviceDate);
      // no matching pattern found
      if (tripPattern == null) {
        return List.of();
      }

      Instant midnight = ServiceDateUtils
        .asStartOfService(serviceDate, transitService.getTimeZone())
        .toInstant();
      Timetable timetable = transitService.getTimetableForTripPattern(tripPattern, serviceDate);
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

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private DatedTrip getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
