package org.opentripplanner.apis.gtfs.datafetchers;

import gnu.trove.set.TIntSet;
import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.SemanticHash;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class PatternImpl implements GraphQLDataFetchers.GraphQLPattern {

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getTransitService(environment).getTransitAlertService();
      var args = new GraphQLTypes.GraphQLPatternAlertsArgs(environment.getArguments());
      List<GraphQLTypes.GraphQLPatternAlertType> types = args.getGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          switch (type) {
            case PATTERN:
              alerts.addAll(
                alertService.getDirectionAndRouteAlerts(
                  getSource(environment).getDirection(),
                  getRoute(environment).getId()
                )
              );
              break;
            case AGENCY:
              alerts.addAll(alertService.getAgencyAlerts(getAgency(environment).getId()));
              break;
            case ROUTE:
              alerts.addAll(alertService.getRouteAlerts(getRoute(environment).getId()));
              break;
            case ROUTE_TYPE:
              int routeType = getRoute(environment).getGtfsType();
              alerts.addAll(
                alertService.getRouteTypeAlerts(
                  routeType,
                  getSource(environment).getId().getFeedId()
                )
              );
              alerts.addAll(
                alertService.getRouteTypeAndAgencyAlerts(routeType, getAgency(environment).getId())
              );
              break;
            case TRIPS:
              getTrips(environment).forEach(trip ->
                alerts.addAll(alertService.getTripAlerts(trip.getId()))
              );
              break;
            case STOPS_ON_PATTERN:
              alerts.addAll(
                alertService
                  .getAllAlerts()
                  .stream()
                  .filter(alert ->
                    alert
                      .entities()
                      .stream()
                      .anyMatch(entity ->
                        (entity instanceof EntitySelector.StopAndRoute stopAndRoute &&
                          stopAndRoute.routeId().equals(getRoute(environment).getId()))
                      )
                  )
                  .toList()
              );
              getSource(environment)
                .getStops()
                .forEach(stop -> {
                  FeedScopedId stopId = stop.getId();
                  alerts.addAll(alertService.getStopAlerts(stopId));
                });
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
                          (entity instanceof EntitySelector.StopAndTrip stopAndTrip &&
                            stopAndTrip.tripId().equals(getSource(environment).getId()))
                        )
                    )
                    .toList()
                )
              );
              break;
          }
        });
        return alerts.stream().distinct().collect(Collectors.toList());
      } else {
        return alertService.getDirectionAndRouteAlerts(
          getSource(environment).getDirection(),
          getRoute(environment).getId()
        );
      }
    };
  }

  @Override
  public DataFetcher<String> code() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Integer> directionId() {
    return environment -> getSource(environment).getDirection().gtfsCode;
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
  public DataFetcher<String> headsign() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).getTripHeadsign(), environment);
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("Pattern", getSource(environment).getId().toString());
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).getName();
  }

  @Override
  public DataFetcher<TripPattern> originalTripPattern() {
    return environment -> getSource(environment).getOriginalTripPattern();
  }

  @Override
  public DataFetcher<Geometry> patternGeometry() {
    return environment -> getSource(environment).getGeometry();
  }

  @Override
  public DataFetcher<Route> route() {
    return this::getRoute;
  }

  @Override
  public DataFetcher<String> semanticHash() {
    return environment -> SemanticHash.forTripPattern(getSource(environment), null);
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return this::getStops;
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return this::getTrips;
  }

  @Override
  public DataFetcher<Iterable<Trip>> tripsForDate() {
    return environment -> {
      String serviceDate = new GraphQLTypes.GraphQLPatternTripsForDateArgs(
        environment.getArguments()
      ).getGraphQLServiceDate();

      try {
        TIntSet services = getTransitService(environment).getServiceCodesRunningForDate(
          ServiceDateUtils.parseString(serviceDate)
        );
        return getSource(environment)
          .getScheduledTimetable()
          .getTripTimes()
          .stream()
          .filter(times -> services.contains(times.getServiceCode()))
          .map(TripTimes::getTrip)
          .collect(Collectors.toList());
      } catch (ParseException e) {
        return null; // Invalid date format
      }
    };
  }

  @Override
  public DataFetcher<Iterable<RealtimeVehicle>> vehiclePositions() {
    return environment ->
      getRealtimeVehiclesService(environment).getRealtimeVehicles(this.getSource(environment));
  }

  private Agency getAgency(DataFetchingEnvironment environment) {
    return getRoute(environment).getAgency();
  }

  private Route getRoute(DataFetchingEnvironment environment) {
    return getSource(environment).getRoute();
  }

  private List<Object> getStops(DataFetchingEnvironment environment) {
    return getSource(environment).getStops().stream().map(Object.class::cast).toList();
  }

  private List<Trip> getTrips(DataFetchingEnvironment environment) {
    return getSource(environment).scheduledTripsAsStream().collect(Collectors.toList());
  }

  private RealtimeVehicleService getRealtimeVehiclesService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().realTimeVehicleService();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private TripPattern getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
