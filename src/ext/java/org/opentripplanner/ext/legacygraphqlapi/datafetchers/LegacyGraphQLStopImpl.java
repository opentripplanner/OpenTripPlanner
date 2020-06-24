package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.StopFinder;
import org.opentripplanner.routing.alertpatch.AlertPatch;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LegacyGraphQLStopImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLStop {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> getValue(
        environment,
        stop -> new Relay.ResolvedGlobalId("Stop", stop.getId().toString()),
        station -> new Relay.ResolvedGlobalId("Stop", station.getId().toString())
    );
  }

  //TODO
  @Override
  public DataFetcher<Iterable<TripTimeShort>> stopTimesForPattern() {
    return null;
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getValue(
        environment,
        stop -> stop.getId().toString(),
        station -> station.getId().toString()
    );
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getValue(environment, Stop::getName, Station::getName);
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getValue(environment, Stop::getLat, Station::getLat);
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getValue(environment, Stop::getLon, Station::getLon);
  }

  @Override
  public DataFetcher<String> code() {
    return environment -> getValue(environment, Stop::getCode, Station::getCode);
  }

  @Override
  public DataFetcher<String> desc() {
    return environment -> getValue(environment, Stop::getDescription, Station::getDescription);
  }

  @Override
  public DataFetcher<String> zoneId() {
    return environment -> getValue(environment, Stop::getZone, station -> null);
  }

  @Override
  public DataFetcher<String> url() {
    return environment -> getValue(environment, Stop::getUrl, Station::getUrl);
  }

  @Override
  public DataFetcher<Object> locationType() {
    return environment -> getValue(environment, stop -> "STOP", station -> "STATION");
  }

  @Override
  public DataFetcher<Object> parentStation() {
    return environment -> getValue(environment, Stop::getParentStation, station -> null);
  }

  @Override
  public DataFetcher<Object> wheelchairBoarding() {
    return environment -> getValue(environment, Stop::getWheelchairBoarding, station -> null);
  }

  // TODO
  @Override
  public DataFetcher<String> direction() {
    return environment -> null;
  }

  // TODO
  @Override
  public DataFetcher<String> timezone() {
    return environment -> null;
  }

  // TODO
  @Override
  public DataFetcher<Integer> vehicleType() {
    return environment -> null;
  }

  // TODO
  @Override
  public DataFetcher<String> vehicleMode() {
    return environment -> null;
  }

  // TODO
  @Override
  public DataFetcher<String> platformCode() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Object> cluster() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment -> getValue(
        environment,
        stop -> null,
        station -> new ArrayList<Object>(station.getChildStops()));
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> getValue(
        environment,
        stop -> getRoutingService(environment).getRoutesForStop(stop),
        station -> null
    );
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> getValue(
        environment,
        stop -> getRoutingService(environment).getPatternsForStop(stop, true),
        station -> null
    );
  }

  //TODO
  @Override
  public DataFetcher<Iterable<StopFinder.StopAndDistance>> transfers() {
    return environment -> getValue(
        environment,
        stop -> {
          Integer maxDistance = new LegacyGraphQLTypes
              .LegacyGraphQLStopTransfersArgs(environment.getArguments())
              .getLegacyGraphQLMaxDistance();

          return getRoutingService(environment)
              .getTransfersByStop(stop)
              .stream()
              .filter(simpleTransfer -> maxDistance == null || simpleTransfer.getDistanceMeters() < maxDistance)
              .map(transfer -> new StopFinder.StopAndDistance(
                  transfer.to,
                  (int) transfer.getDistanceMeters()
              ))
              .collect(Collectors.toList());
        },
        station -> null
    );
  }

  @Override
  public DataFetcher<Iterable<StopTimesInPattern>> stoptimesForServiceDate() {
    return environment -> {
      RoutingService routingService = getRoutingService(environment);
      var args = new LegacyGraphQLTypes.LegacyGraphQLStopStoptimesForServiceDateArgs(environment.getArguments());
      ServiceDate date;
      try {
        date = ServiceDate.parseString(args.getLegacyGraphQLDate());
      } catch (ParseException e) {
        return null;
      }

      // TODO: use args.getLegacyGraphQLOmitCanceled()

      Function<Stop, List<StopTimesInPattern>> stopTFunction = stop ->
          routingService.getStopTimesForStop(stop, date, args.getLegacyGraphQLOmitNonPickups());

      return getValue(
          environment,
          stopTFunction,
          station -> station
              .getChildStops()
              .stream()
              .map(stopTFunction)
              .flatMap(Collection::stream)
              .collect(Collectors.toList())
      );
    };
  }

  @Override
  public DataFetcher<Iterable<StopTimesInPattern>> stoptimesForPatterns() {
    return environment -> {
      RoutingService routingService = getRoutingService(environment);
      var args = new LegacyGraphQLTypes.LegacyGraphQLStopStoptimesForPatternsArgs(environment.getArguments());

      // TODO: use args.getLegacyGraphQLOmitCanceled()

      Function<Stop, List<StopTimesInPattern>> stopTFunction = stop ->
          routingService.stopTimesForStop(
              stop,
              args.getLegacyGraphQLStartTime(),
              args.getLegacyGraphQLTimeRange(),
              args.getLegacyGraphQLNumberOfDepartures(),
              args.getLegacyGraphQLOmitNonPickups());

      return getValue(
          environment,
          stopTFunction,
          station -> station
              .getChildStops()
              .stream()
              .map(stopTFunction)
              .flatMap(Collection::stream)
              .collect(Collectors.toList())
      );
    };
  }

  @Override
  public DataFetcher<Iterable<TripTimeShort>> stoptimesWithoutPatterns() {
    return environment -> {
      RoutingService routingService = getRoutingService(environment);
      var args = new LegacyGraphQLTypes.LegacyGraphQLStopStoptimesForPatternsArgs(environment.getArguments());

      // TODO: use args.getLegacyGraphQLOmitCanceled()

      Function<Stop, Stream<StopTimesInPattern>> stopTFunction = stop ->
          routingService.stopTimesForStop(
              stop,
              args.getLegacyGraphQLStartTime(),
              args.getLegacyGraphQLTimeRange(),
              args.getLegacyGraphQLNumberOfDepartures(),
              args.getLegacyGraphQLOmitNonPickups()
          ).stream();

      Stream<StopTimesInPattern> stream = getValue(
          environment,
          stopTFunction,
          station -> station
              .getChildStops()
              .stream()
              .flatMap(stopTFunction)
      );

      return stream.flatMap(stoptimesWithPattern -> stoptimesWithPattern.times.stream())
          .sorted(Comparator.comparing(t -> t.serviceDay + t.realtimeDeparture))
          .limit(args.getLegacyGraphQLNumberOfDepartures())
          .collect(Collectors.toList());
    };
  }

  // TODO
  @Override
  public DataFetcher<Iterable<AlertPatch>> alerts() {
    return null;
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private <T> T getValue(
      DataFetchingEnvironment environment,
      Function<Stop, T> stopTFunction,
      Function<Station, T> stationTFunction
  ) {
      Object source = environment.getSource();
      if (source instanceof Stop) {
        return stopTFunction.apply((Stop) source);
      }
      else if (source instanceof Station) {
        return stationTFunction.apply((Station) source);
      }
      return null;
  }
}
