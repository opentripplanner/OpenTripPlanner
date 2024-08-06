package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.PatternByServiceDatesFilter;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLBikesAllowed;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLTransitMode;
import org.opentripplanner.apis.gtfs.mapping.BikesAllowedMapper;
import org.opentripplanner.apis.gtfs.support.time.LocalDateRangeUtil;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

public class RouteImpl implements GraphQLDataFetchers.GraphQLRoute {

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment).getAgency();
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getAlertService(environment);
      var args = new GraphQLTypes.GraphQLRouteAlertsArgs(environment.getArguments());
      List<GraphQLTypes.GraphQLRouteAlertType> types = args.getGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          switch (type) {
            case ROUTE:
              alerts.addAll(alertService.getRouteAlerts(getSource(environment).getId()));
              break;
            case ROUTE_TYPE:
              alerts.addAll(
                alertService.getRouteTypeAlerts(
                  getSource(environment).getGtfsType(),
                  getSource(environment).getId().getFeedId()
                )
              );
              alerts.addAll(
                alertService.getRouteTypeAndAgencyAlerts(
                  getSource(environment).getGtfsType(),
                  getSource(environment).getAgency().getId()
                )
              );
              break;
            case AGENCY:
              alerts.addAll(
                alertService.getAgencyAlerts(getSource(environment).getAgency().getId())
              );
              break;
            case TRIPS:
              getTrips(environment)
                .forEach(trip -> alerts.addAll(alertService.getTripAlerts(trip.getId(), null)));
              break;
            case STOPS_ON_ROUTE:
              alerts.addAll(
                alertService
                  .getAllAlerts()
                  .stream()
                  .filter(alert ->
                    alert
                      .entities()
                      .stream()
                      .anyMatch(entity ->
                        entity instanceof EntitySelector.StopAndRoute stopAndRoute &&
                        stopAndRoute.routeId().equals(getSource(environment).getId())
                      )
                  )
                  .toList()
              );
              getStops(environment)
                .forEach(stop ->
                  alerts.addAll(alertService.getStopAlerts(((StopLocation) stop).getId()))
                );
              break;
            case STOPS_ON_TRIPS:
              Iterable<Trip> trips = getTrips(environment);
              trips.forEach(trip ->
                alerts.addAll(
                  alertService
                    .getAllAlerts()
                    .stream()
                    .filter(alert ->
                      alert
                        .entities()
                        .stream()
                        .anyMatch(entity ->
                          entity instanceof EntitySelector.StopAndTrip stopAndTrip &&
                          stopAndTrip.tripId().equals(trip.getId())
                        )
                    )
                    .toList()
                )
              );
              break;
            case PATTERNS:
              alerts.addAll(
                alertService.getDirectionAndRouteAlerts(
                  Direction.INBOUND,
                  getSource(environment).getId()
                )
              );
              alerts.addAll(
                alertService.getDirectionAndRouteAlerts(
                  Direction.OUTBOUND,
                  getSource(environment).getId()
                )
              );
              break;
          }
        });
        return alerts.stream().distinct().collect(Collectors.toList());
      } else {
        return getAlertService(environment).getRouteAlerts(getSource(environment).getId());
      }
    };
  }

  @Override
  public DataFetcher<GraphQLBikesAllowed> bikesAllowed() {
    return environment -> BikesAllowedMapper.map(getSource(environment).getBikesAllowed());
  }

  @Override
  public DataFetcher<String> color() {
    return environment -> getSource(environment).getColor();
  }

  @Override
  public DataFetcher<String> desc() {
    return environment -> getSource(environment).getDescription();
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("Route", getSource(environment).getId().toString());
  }

  @Override
  public DataFetcher<String> longName() {
    return environment ->
      org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
        getSource(environment).getLongName(),
        environment
      );
  }

  @Override
  public DataFetcher<GraphQLTransitMode> mode() {
    return environment -> GraphQLUtils.toGraphQL(getSource(environment).getMode());
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> {
      final TransitService transitService = getTransitService(environment);
      var patterns = transitService.getPatternsForRoute(getSource(environment));

      var args = new GraphQLTypes.GraphQLRoutePatternsArgs(environment.getArguments());

      if (LocalDateRangeUtil.hasServiceDateFilter(args.getGraphQLServiceDates())) {
        var filter = new PatternByServiceDatesFilter(args.getGraphQLServiceDates(), transitService);
        return filter.filterPatterns(patterns);
      } else {
        return patterns;
      }
    };
  }

  @Override
  public DataFetcher<String> shortName() {
    return environment -> getSource(environment).getShortName();
  }

  @Override
  public DataFetcher<Integer> sortOrder() {
    return env -> getSource(env).getGtfsSortOrder();
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return this::getStops;
  }

  @Override
  public DataFetcher<String> textColor() {
    return environment -> getSource(environment).getTextColor();
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return this::getTrips;
  }

  @Override
  public DataFetcher<Integer> type() {
    return environment -> getSource(environment).getGtfsType();
  }

  @Override
  public DataFetcher<String> url() {
    return environment -> getSource(environment).getUrl();
  }

  private Iterable<Object> getStops(DataFetchingEnvironment environment) {
    return getTransitService(environment)
      .getPatternsForRoute(getSource(environment))
      .stream()
      .map(TripPattern::getStops)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private Iterable<Trip> getTrips(DataFetchingEnvironment environment) {
    return getTransitService(environment)
      .getPatternsForRoute(getSource(environment))
      .stream()
      .flatMap(TripPattern::scheduledTripsAsStream)
      .collect(Collectors.toSet());
  }

  private TransitAlertService getAlertService(DataFetchingEnvironment environment) {
    return getTransitService(environment).getTransitAlertService();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private Route getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
