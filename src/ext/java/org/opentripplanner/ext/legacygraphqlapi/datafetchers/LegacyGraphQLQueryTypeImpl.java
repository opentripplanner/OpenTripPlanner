package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import com.google.common.collect.Lists;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.routing.RoutingResponse;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.request.RoutingRequest;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LegacyGraphQLQueryTypeImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLQueryType {

  //TODO
  @Override
  public DataFetcher<Object> node() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeNodeArgs(environment.getArguments());
      String type = args.getLegacyGraphQLId().getType();
      String id = args.getLegacyGraphQLId().getId();

      return null;
    };
  }

  @Override
  public DataFetcher<Iterable<FeedInfo>> feeds() {
    return environment -> getRoutingService(environment)
        .getFeedInfoForId()
        .values();
  }

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> getRoutingService(environment).getAgencies();
  }

  //TODO
  @Override
  public DataFetcher<Iterable<FareRuleSet>> ticketTypes() {
    return null;
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> {
      FeedScopedId id = FeedScopedId.parseId(new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAgencyArgs(environment.getArguments()).getLegacyGraphQLId());

      return getRoutingService(environment).getAgencyForId(id);
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsArgs(environment.getArguments());

      RoutingService routingService = getRoutingService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return StreamSupport
            .stream(args.getLegacyGraphQLIds().spliterator(), false)
            .map(FeedScopedId::parseId)
            .map(routingService::getStopForId)
            .collect(Collectors.toList());
      }

      Stream<Stop> stopStream = routingService.getAllStops().stream();

      if (args.getLegacyGraphQLName() != null) {
        String name = args.getLegacyGraphQLName().toLowerCase(environment.getLocale());
        stopStream = stopStream.filter(stop -> stop
            .getName()
            .toLowerCase(environment.getLocale())
            .startsWith(name));
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stopsByBbox() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByBboxArgs(environment.getArguments());

      Stream<Stop> stopStream = getRoutingService(environment)
          .getStopSpatialIndex()
          .query(new Envelope(
              new Coordinate(args.getLegacyGraphQLMinLon(), args.getLegacyGraphQLMinLat()),
              new Coordinate(args.getLegacyGraphQLMaxLon(), args.getLegacyGraphQLMaxLat())
          ))
          .stream()
          .map(TransitStopVertex::getStop);

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feedIds = Lists.newArrayList(args.getLegacyGraphQLFeeds());
        stopStream = stopStream.filter(stop -> feedIds.contains(stop.getId().getFeedId()));
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  //TODO
  @Override
  public DataFetcher<Object> stopsByRadius() {
    return null;
  }

  //TODO
  @Override
  public DataFetcher<Object> nearest() {
    return null;
  }

  //TODO
  @Override
  public DataFetcher<Object> departureRow() {
    return null;
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getRoutingService(environment)
        .getStopForId(FeedScopedId.parseId(
            new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopArgs(environment.getArguments())
                .getLegacyGraphQLId()));
  }

  @Override
  public DataFetcher<Object> station() {
    return environment -> getRoutingService(environment)
        .getStationById(FeedScopedId.parseId(
            new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStationArgs(environment.getArguments())
                .getLegacyGraphQLId()));
  }

  @Override
  public DataFetcher<Iterable<Object>> stations() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return null;
  }

  @Override
  public DataFetcher<Route> route() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return null;
  }

  @Override
  public DataFetcher<Trip> trip() {
    return null;
  }

  @Override
  public DataFetcher<Trip> fuzzyTrip() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<TripTimeShort>> cancelledTripTimes() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return null;
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Object>> clusters() {
    return null;
  }

  @Override
  public DataFetcher<Object> cluster() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<AlertPatch>> alerts() {
    return null;
  }

  @Override
  public DataFetcher<Object> serviceTimeRange() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<BikeRentalStation>> bikeRentalStations() {
    return null;
  }

  @Override
  public DataFetcher<BikeRentalStation> bikeRentalStation() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<BikePark>> bikeParks() {
    return null;
  }

  @Override
  public DataFetcher<BikePark> bikePark() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Object>> carParks() {
    return null;
  }

  @Override
  public DataFetcher<Object> carPark() {
    return null;
  }

  @Override
  public DataFetcher<Object> viewer() {
    return environment -> new Object();
  }

  @Override
  public DataFetcher<RoutingResponse> plan() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypePlanArgs(environment.getArguments());

      RoutingRequest rr = new RoutingRequest();

      LegacyGraphQLRequestContext context = environment.<LegacyGraphQLRequestContext>getContext();
      return context.getRoutingService().route(rr, context.getRouter());
    };
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }
}
