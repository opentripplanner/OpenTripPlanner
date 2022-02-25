package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;

import java.text.ParseException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyGraphQLPatternImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLPattern {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("Pattern",
        getSource(environment).getId().toString()
    );
  }

  @Override
  public DataFetcher<Route> route() {
    return this::getRoute;
  }

  @Override
  public DataFetcher<Integer> directionId() {
    return environment -> getSource(environment).getDirection().gtfsCode;
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).getName();
  }

  @Override
  public DataFetcher<String> code() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<String> headsign() {
    return environment -> getSource(environment).getTripHeadsign();
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return this::getTrips;
  }

  @Override
  public DataFetcher<Iterable<Trip>> tripsForDate() {
    return environment -> {
      String servicaDate = new LegacyGraphQLTypes.LegacyGraphQLPatternTripsForDateArgs(environment.getArguments()).getLegacyGraphQLServiceDate();

      try {
        BitSet services = getRoutingService(environment).getServicesRunningForDate(
            ServiceDate.parseString(servicaDate)
        );
        return getSource(environment).getScheduledTimetable().getTripTimes()
            .stream()
            .filter(times -> services.get(times.getServiceCode()))
            .map(times -> times.getTrip())
            .collect(Collectors.toList());
      } catch (ParseException e) {
        return null; // Invalid date format
      }
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return this::getStops;
  }

  @Override
  public DataFetcher<Iterable<Coordinate>> geometry() {
    return environment -> {
      LineString geometry = getSource(environment).getGeometry();
      if (geometry == null) {
        return null;
      } else {
        return Arrays.asList(geometry.getCoordinates());
      }
    };
  }

  @Override
  public DataFetcher<Geometry> patternGeometry() {
    return environment -> getSource(environment).getGeometry();
  }

  @Override
  public DataFetcher<String> semanticHash() {
    return environment -> getSource(environment).semanticHashString(null);
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getRoutingService(environment).getTransitAlertService();
      var args = new LegacyGraphQLTypes.LegacyGraphQLPatternAlertsArgs(
              environment.getArguments());
      Iterable<LegacyGraphQLTypes.LegacyGraphQLPatternAlertType> types =
              args.getLegacyGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          switch (type) {
            case PATTERN:
              alerts.addAll(alertService.getDirectionAndRouteAlerts(
                      getSource(environment).getDirection().gtfsCode,
                      getRoute(environment).getId()
              ));
              break;
            case AGENCY:
              alerts.addAll(alertService.getAgencyAlerts(getAgency(environment).getId()));
              break;
            case ROUTE:
              alerts.addAll(alertService.getRouteAlerts(getRoute(environment).getId()));
              break;
            case ROUTE_TYPE:
              int routeType = getRoute(environment).getGtfsType();
              alerts.addAll(alertService.getRouteTypeAlerts(
                      routeType,
                      getSource(environment).getId().getFeedId()
              ));
              alerts.addAll(alertService.getRouteTypeAndAgencyAlerts(
                      routeType,
                      getAgency(environment).getId()
              ));
              break;
            case TRIPS:
              getTrips(environment).forEach(
                      trip -> alerts.addAll(alertService.getTripAlerts(trip.getId(), null)));
              break;
            case STOPS_ON_PATTERN:
              alerts.addAll(alertService.getAllAlerts()
                      .stream()
                      .filter(alert -> alert.getEntities()
                              .stream()
                              .anyMatch(entity -> (
                                      entity instanceof EntitySelector.StopAndRoute
                                              && ((EntitySelector.StopAndRoute) entity).stopAndRoute.routeOrTrip.equals(
                                              getRoute(environment).getId())
                              )))
                      .collect(Collectors.toList()));
              getSource(environment).getStops().forEach(stop -> {
                FeedScopedId stopId = stop.getId();
                alerts.addAll(alertService.getStopAlerts(stopId));
              });
              break;
            case STOPS_ON_TRIPS:
              Iterable<Trip> trips = getTrips(environment);
              trips.forEach(trip -> alerts.addAll(alertService.getAllAlerts()
                      .stream()
                      .filter(alert -> alert.getEntities()
                              .stream()
                              .anyMatch(entity -> (
                                      entity instanceof EntitySelector.StopAndTrip
                                              && ((EntitySelector.StopAndTrip) entity).stopAndTrip.routeOrTrip.equals(
                                              getSource(environment).getId())
                              )))
                      .collect(Collectors.toList())));
              break;
          }
        });
        return alerts.stream().distinct().collect(Collectors.toList());
      }
      else {
        return alertService.getDirectionAndRouteAlerts(
                getSource(environment).getDirection().gtfsCode,
                getRoute(environment).getId()
        );
      }
    };
  }

  private Agency getAgency(DataFetchingEnvironment environment) {
    return getRoute(environment).getAgency();
  }

  private Route getRoute(DataFetchingEnvironment environment) {
    return getSource(environment).getRoute();
  }


  private List<Object> getStops(DataFetchingEnvironment environment) {
    return getSource(environment).getStops().stream()
            .map(Object.class::cast)
            .collect(Collectors.toList());
  }

  private List<Trip> getTrips(DataFetchingEnvironment environment) {
    return getSource(environment).scheduledTripsAsStream().collect(Collectors.toList());
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private TripPattern getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
