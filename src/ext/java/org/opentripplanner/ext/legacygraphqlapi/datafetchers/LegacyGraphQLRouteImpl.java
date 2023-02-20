package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLUtils;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLTransitMode;
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

public class LegacyGraphQLRouteImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLRoute {

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment).getAgency();
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getAlertService(environment);
      var args = new LegacyGraphQLTypes.LegacyGraphQLRouteAlertsArgs(environment.getArguments());
      Iterable<LegacyGraphQLTypes.LegacyGraphQLRouteAlertType> types = args.getLegacyGraphQLTypes();
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
  public DataFetcher<String> bikesAllowed() {
    return environment ->
      switch (getSource(environment).getBikesAllowed()) {
        case UNKNOWN -> "NO_INFORMATION";
        case ALLOWED -> "POSSIBLE";
        case NOT_ALLOWED -> "NOT_POSSIBLE";
      };
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
      LegacyGraphQLUtils.getTranslation(getSource(environment).getLongName(), environment);
  }

  @Override
  public DataFetcher<LegacyGraphQLTransitMode> mode() {
    return environment -> LegacyGraphQLUtils.toGraphQL(getSource(environment).getMode());
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment ->
      getTransitService(environment).getPatternsForRoute(getSource(environment));
  }

  @Override
  public DataFetcher<String> shortName() {
    return environment -> getSource(environment).getShortName();
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
    return environment.<LegacyGraphQLRequestContext>getContext().transitService();
  }

  private Route getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
