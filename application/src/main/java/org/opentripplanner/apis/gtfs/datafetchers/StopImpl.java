package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.support.filter.PatternByDateFilterUtil;
import org.opentripplanner.apis.gtfs.support.time.LocalDateRangeUtil;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.EntitySelector.StopAndRoute;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class StopImpl implements GraphQLDataFetchers.GraphQLStop {

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getTransitService(environment).getTransitAlertService();
      var args = new GraphQLTypes.GraphQLStopAlertsArgs(environment.getArguments());
      List<GraphQLTypes.GraphQLStopAlertType> types = args.getGraphQLTypes();
      FeedScopedId id = getValue(environment, StopLocation::getId, AbstractTransitEntity::getId);
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        if (types.contains(GraphQLTypes.GraphQLStopAlertType.STOP)) {
          alerts.addAll(alertService.getStopAlerts(id));
        }
        if (
          types.contains(GraphQLTypes.GraphQLStopAlertType.STOP_ON_ROUTES) ||
          types.contains(GraphQLTypes.GraphQLStopAlertType.STOP_ON_TRIPS)
        ) {
          alerts.addAll(
            alertService
              .getAllAlerts()
              .stream()
              .filter(alert ->
                alert
                  .entities()
                  .stream()
                  .anyMatch(
                    entity ->
                      (types.contains(GraphQLTypes.GraphQLStopAlertType.STOP_ON_ROUTES) &&
                        entity instanceof StopAndRoute stopAndRoute &&
                        stopAndRoute.stopId().equals(id)) ||
                      (types.contains(GraphQLTypes.GraphQLStopAlertType.STOP_ON_TRIPS) &&
                        entity instanceof EntitySelector.StopAndTrip stopAndTrip &&
                        stopAndTrip.stopId().equals(id))
                  )
              )
              .toList()
          );
        }
        if (
          types.contains(GraphQLTypes.GraphQLStopAlertType.PATTERNS) ||
          types.contains(GraphQLTypes.GraphQLStopAlertType.TRIPS)
        ) {
          var patterns = getPatterns(environment);
          if (patterns != null) {
            patterns.forEach(pattern -> {
              if (types.contains(GraphQLTypes.GraphQLStopAlertType.PATTERNS)) {
                alerts.addAll(
                  alertService.getDirectionAndRouteAlerts(
                    pattern.getDirection(),
                    pattern.getRoute().getId()
                  )
                );
              }
              if (types.contains(GraphQLTypes.GraphQLStopAlertType.TRIPS)) {
                pattern
                  .scheduledTripsAsStream()
                  .forEach(trip -> alerts.addAll(alertService.getTripAlerts(trip.getId(), null)));
              }
            });
          }
        }
        if (
          types.contains(GraphQLTypes.GraphQLStopAlertType.ROUTES) ||
          types.contains(GraphQLTypes.GraphQLStopAlertType.AGENCIES_OF_ROUTES)
        ) {
          var routes = getRoutes(environment);
          if (routes != null) {
            routes.forEach(route -> {
              if (types.contains(GraphQLTypes.GraphQLStopAlertType.ROUTES)) {
                alerts.addAll(alertService.getRouteAlerts(route.getId()));
              }
              if (types.contains(GraphQLTypes.GraphQLStopAlertType.AGENCIES_OF_ROUTES)) {
                alerts.addAll(alertService.getAgencyAlerts(route.getAgency().getId()));
              }
            });
          }
        }
        return alerts.stream().distinct().collect(Collectors.toList());
      } else {
        return alertService.getStopAlerts(id);
      }
    };
  }

  @Override
  public DataFetcher<Object> cluster() {
    return environment -> null;
  }

  @Override
  public DataFetcher<String> code() {
    return environment -> getValue(environment, StopLocation::getCode, Station::getCode);
  }

  @Override
  public DataFetcher<String> desc() {
    return environment ->
      getValue(
        environment,
        stop ->
          org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
            stop.getDescription(),
            environment
          ),
        station ->
          org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
            station.getDescription(),
            environment
          )
      );
  }

  @Override
  public DataFetcher<String> url() {
    return environment ->
      getValue(
        environment,
        stop ->
          org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
            stop.getUrl(),
            environment
          ),
        station ->
          org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
            station.getUrl(),
            environment
          )
      );
  }

  @Override
  public DataFetcher<Object> locationType() {
    return environment -> getValue(environment, stop -> "STOP", station -> "STATION");
  }

  @Override
  public DataFetcher<Object> parentStation() {
    return environment -> getValue(environment, StopLocation::getParentStation, station -> null);
  }

  // TODO
  @Override
  public DataFetcher<String> direction() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Object> geometries() {
    return environment -> getValue(environment, StopLocation::getGeometry, Station::getGeometry);
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment ->
      getValue(environment, stop -> stop.getId().toString(), station -> station.getId().toString());
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      getValue(
        environment,
        stop -> new Relay.ResolvedGlobalId("Stop", stop.getId().toString()),
        station -> new Relay.ResolvedGlobalId("Stop", station.getId().toString())
      );
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getValue(environment, StopLocation::getLat, Station::getLat);
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getValue(environment, StopLocation::getLon, Station::getLon);
  }

  @Override
  public DataFetcher<String> name() {
    return environment ->
      getValue(
        environment,
        stop ->
          org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
            stop.getName(),
            environment
          ),
        station ->
          org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
            station.getName(),
            environment
          )
      );
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return this::getPatterns;
  }

  @Override
  public DataFetcher<String> platformCode() {
    return environment -> getValue(environment, StopLocation::getPlatformCode, station -> null);
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return env -> {
      var args = new GraphQLTypes.GraphQLStopRoutesArgs(env.getArguments());
      var routes = getRoutes(env);
      if (LocalDateRangeUtil.hasServiceDateFilter(args.getGraphQLServiceDates())) {
        var filter = PatternByDateFilterUtil.ofGraphQL(
          args.getGraphQLServiceDates(),
          getTransitService(env)
        );
        return filter.filterRoutes(routes);
      } else {
        return routes;
      }
    };
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stopTimesForPattern() {
    return environment ->
      getValue(
        environment,
        stop -> {
          TransitService transitService = getTransitService(environment);
          GraphQLTypes.GraphQLStopStopTimesForPatternArgs args =
            new GraphQLTypes.GraphQLStopStopTimesForPatternArgs(environment.getArguments());
          TripPattern pattern = transitService.getTripPattern(
            FeedScopedId.parse(args.getGraphQLId())
          );

          if (pattern == null) {
            return null;
          }

          if (transitService.hasNewTripPatternsForModifiedTrips()) {
            return getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
              pattern,
              stop,
              transitService,
              args
            );
          }

          return transitService.findTripTimeOnDate(
            stop,
            pattern,
            GraphQLUtils.getTimeOrNow(args.getGraphQLStartTime()),
            Duration.ofSeconds(args.getGraphQLTimeRange()),
            args.getGraphQLNumberOfDepartures(),
            args.getGraphQLOmitNonPickups() ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH,
            !args.getGraphQLOmitCanceled()
          );
        },
        station -> null
      );
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment ->
      getValue(
        environment,
        stop -> null,
        station -> new ArrayList<Object>(station.getChildStops())
      );
  }

  @Override
  public DataFetcher<Iterable<StopTimesInPattern>> stoptimesForPatterns() {
    return environment -> {
      TransitService transitService = getTransitService(environment);
      var args = new GraphQLTypes.GraphQLStopStoptimesForPatternsArgs(environment.getArguments());

      Function<StopLocation, List<StopTimesInPattern>> stopTFunction = stop ->
        transitService.findStopTimesInPattern(
          stop,
          GraphQLUtils.getTimeOrNow(args.getGraphQLStartTime()),
          Duration.ofSeconds(args.getGraphQLTimeRange()),
          args.getGraphQLNumberOfDepartures(),
          args.getGraphQLOmitNonPickups() ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH,
          !args.getGraphQLOmitCanceled()
        );

      return getValue(environment, stopTFunction, station ->
        station
          .getChildStops()
          .stream()
          .map(stopTFunction)
          .flatMap(Collection::stream)
          .collect(Collectors.toList())
      );
    };
  }

  @Override
  public DataFetcher<Iterable<StopTimesInPattern>> stoptimesForServiceDate() {
    return environment -> {
      TransitService transitService = getTransitService(environment);
      var args = new GraphQLTypes.GraphQLStopStoptimesForServiceDateArgs(
        environment.getArguments()
      );
      LocalDate date;
      try {
        date = ServiceDateUtils.parseString(args.getGraphQLDate());
      } catch (ParseException e) {
        return null;
      }

      Function<StopLocation, List<StopTimesInPattern>> stopTFunction = stop ->
        transitService.findStopTimesInPattern(
          stop,
          date,
          args.getGraphQLOmitNonPickups() ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH,
          !args.getGraphQLOmitCanceled()
        );

      return getValue(environment, stopTFunction, station ->
        station
          .getChildStops()
          .stream()
          .map(stopTFunction)
          .flatMap(Collection::stream)
          .collect(Collectors.toList())
      );
    };
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stoptimesWithoutPatterns() {
    return environment -> {
      TransitService transitService = getTransitService(environment);
      var args = new GraphQLTypes.GraphQLStopStoptimesForPatternsArgs(environment.getArguments());

      Function<StopLocation, Stream<StopTimesInPattern>> stopTFunction = stop ->
        transitService
          .findStopTimesInPattern(
            stop,
            GraphQLUtils.getTimeOrNow(args.getGraphQLStartTime()),
            Duration.ofSeconds(args.getGraphQLTimeRange()),
            args.getGraphQLNumberOfDepartures(),
            args.getGraphQLOmitNonPickups() ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH,
            !args.getGraphQLOmitCanceled()
          )
          .stream();

      Stream<StopTimesInPattern> stream = getValue(environment, stopTFunction, station ->
        station.getChildStops().stream().flatMap(stopTFunction)
      );

      return stream
        .flatMap(stoptimesWithPattern -> stoptimesWithPattern.times.stream())
        .sorted(Comparator.comparing(t -> t.getServiceDayMidnight() + t.getRealtimeDeparture()))
        .limit(args.getGraphQLNumberOfDepartures())
        .collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<String> timezone() {
    return environment ->
      getValue(
        environment,
        stop -> stop.getTimeZone().toString(),
        station -> station.getTimezone().toString()
      );
  }

  @Override
  public DataFetcher<Iterable<NearbyStop>> transfers() {
    return environment ->
      getValue(
        environment,
        stop -> {
          Integer maxDistance = new GraphQLTypes.GraphQLStopTransfersArgs(
            environment.getArguments()
          ).getGraphQLMaxDistance();

          return getTransitService(environment)
            .findPathTransfers(stop)
            .stream()
            .filter(transfer -> maxDistance == null || transfer.getDistanceMeters() < maxDistance)
            .filter(transfer -> transfer.to instanceof RegularStop)
            .map(transfer ->
              new NearbyStop(transfer.to, transfer.getDistanceMeters(), transfer.getEdges(), null)
            )
            .collect(Collectors.toList());
        },
        station -> null
      );
  }

  @Override
  public DataFetcher<String> vehicleMode() {
    return environment -> {
      TransitService transitService = getTransitService(environment);
      return getValue(
        environment,
        stop ->
          transitService
            .findTransitModes(stop)
            .stream()
            .findFirst()
            .map(Enum::toString)
            .orElse(null),
        station ->
          transitService
            .findTransitModes(station)
            .stream()
            .findFirst()
            .map(Enum::toString)
            .orElse(null)
      );
    };
  }

  // TODO
  @Override
  public DataFetcher<Integer> vehicleType() {
    return environment -> null;
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLWheelchairBoarding> wheelchairBoarding() {
    return environment -> {
      var boarding = getValue(environment, StopLocation::getWheelchairAccessibility, station -> null
      );
      return GraphQLUtils.toGraphQL(boarding);
    };
  }

  @Override
  public DataFetcher<String> zoneId() {
    return environment ->
      getValue(environment, StopLocation::getFirstZoneAsString, station -> null);
  }

  @Nullable
  private Collection<TripPattern> getPatterns(DataFetchingEnvironment environment) {
    return getValue(
      environment,
      stop -> getTransitService(environment).findPatterns(stop, true),
      station -> null
    );
  }
  
  @Nullable
  private Collection<Route> getRoutes(DataFetchingEnvironment environment) {
    return getValue(
      environment,
      stop -> getTransitService(environment).findRoutes(stop),
      station -> null
    );
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  /**
   * TODO this functionality should be supported by {@link StopTimesHelper#stopTimesForPatternAtStop}
   */
  private List<TripTimeOnDate> getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
    TripPattern originalPattern,
    StopLocation stop,
    TransitService transitService,
    GraphQLTypes.GraphQLStopStopTimesForPatternArgs args
  ) {
    Instant startTime = GraphQLUtils.getTimeOrNow(args.getGraphQLStartTime());
    LocalDate date = startTime.atZone(transitService.getTimeZone()).toLocalDate();

    return Stream.concat(
      getRealtimeAddedPatternsAsStream(originalPattern, transitService, date),
      Stream.of(originalPattern)
    )
      .flatMap(tripPattern ->
        transitService
          .findTripTimeOnDate(
            stop,
            tripPattern,
            startTime,
            Duration.ofSeconds(args.getGraphQLTimeRange()),
            args.getGraphQLNumberOfDepartures(),
            args.getGraphQLOmitNonPickups() ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH,
            false
          )
          .stream()
      )
      .sorted(
        Comparator.comparing(
          (TripTimeOnDate tts) -> tts.getServiceDayMidnight() + tts.getRealtimeDeparture()
        )
      )
      .limit(args.getGraphQLNumberOfDepartures())
      .toList();
  }

  /**
   * Get a stream of {@link TripPattern} that were created real-time based of the provided pattern.
   * Only patterns that don't have removed (stops can still be skipped) or added stops are included.
   */
  private Stream<TripPattern> getRealtimeAddedPatternsAsStream(
    TripPattern originalPattern,
    TransitService transitService,
    LocalDate date
  ) {
    return originalPattern
      .scheduledTripsAsStream()
      .map(trip -> transitService.findNewTripPatternForModifiedTrip(trip.getId(), date))
      .filter(
        tripPattern ->
          tripPattern != null &&
          tripPattern.isModifiedFromTripPatternWithEqualStops(originalPattern)
      );
  }

  private static <T> T getValue(
    DataFetchingEnvironment environment,
    Function<StopLocation, T> stopTFunction,
    Function<Station, T> stationTFunction
  ) {
    Object source = environment.getSource();
    if (source instanceof StopLocation) {
      return stopTFunction.apply((StopLocation) source);
    } else if (source instanceof Station) {
      return stationTFunction.apply((Station) source);
    }
    return null;
  }
}
