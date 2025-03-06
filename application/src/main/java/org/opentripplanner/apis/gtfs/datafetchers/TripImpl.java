package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLBikesAllowed;
import org.opentripplanner.apis.gtfs.mapping.BikesAllowedMapper;
import org.opentripplanner.apis.gtfs.model.TripOccupancy;
import org.opentripplanner.apis.support.SemanticHash;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class TripImpl implements GraphQLDataFetchers.GraphQLTrip {

  @Override
  public DataFetcher<Iterable<String>> activeDates() {
    return environment ->
      getTransitService(environment)
        .getCalendarService()
        .getServiceDatesForServiceId(getSource(environment).getServiceId())
        .stream()
        .map(ServiceDateUtils::asCompactString)
        .collect(Collectors.toList());
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getTransitService(environment).getTransitAlertService();
      var args = new GraphQLTypes.GraphQLTripAlertsArgs(environment.getArguments());
      List<GraphQLTypes.GraphQLTripAlertType> types = args.getGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          switch (type) {
            case TRIP:
              alerts.addAll(alertService.getTripAlerts(getSource(environment).getId(), null));
              break;
            case AGENCY:
              alerts.addAll(alertService.getAgencyAlerts(getAgency(environment).getId()));
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
            case ROUTE:
              alerts.addAll(alertService.getRouteAlerts(getRoute(environment).getId()));
              break;
            case PATTERN:
              alerts.addAll(
                alertService.getDirectionAndRouteAlerts(
                  getSource(environment).getDirection(),
                  getRoute(environment).getId()
                )
              );
              break;
            case STOPS_ON_TRIP:
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
                          (entity instanceof EntitySelector.StopAndRoute stopAndRoute &&
                            stopAndRoute.routeId().equals(getRoute(environment).getId())) ||
                          (entity instanceof EntitySelector.StopAndTrip stopAndTrip &&
                            stopAndTrip.tripId().equals(getSource(environment).getId()))
                      )
                  )
                  .toList()
              );
              getStops(environment).forEach(stop -> {
                FeedScopedId stopId = ((StopLocation) stop).getId();
                alerts.addAll(alertService.getStopAlerts(stopId));
              });
              break;
          }
        });
        return alerts.stream().distinct().collect(Collectors.toList());
      } else {
        return alertService.getTripAlerts(getSource(environment).getId(), null);
      }
    };
  }

  @Override
  public DataFetcher<TripTimeOnDate> arrivalStoptime() {
    return environment -> {
      var serviceDate = getOptionalServiceDateArgument(environment);
      var trip = getSource(environment);
      var transitService = getTransitService(environment);
      var stopTimes = serviceDate
        .map(date -> transitService.getTripTimeOnDates(trip, date))
        .orElseGet(() -> transitService.getScheduledTripTimes(trip));
      return stopTimes.map(List::getLast).orElse(null);
    };
  }

  @Override
  public DataFetcher<GraphQLBikesAllowed> bikesAllowed() {
    return environment -> BikesAllowedMapper.map(getSource(environment).getBikesAllowed());
  }

  @Override
  public DataFetcher<String> blockId() {
    return environment -> getSource(environment).getGtfsBlockId();
  }

  @Override
  public DataFetcher<TripTimeOnDate> departureStoptime() {
    return environment -> {
      var serviceDate = getOptionalServiceDateArgument(environment);
      var trip = getSource(environment);
      var transitService = getTransitService(environment);
      var stopTimes = serviceDate
        .map(date -> transitService.getTripTimeOnDates(trip, date))
        .orElseGet(() -> transitService.getScheduledTripTimes(trip));
      return stopTimes.map(List::getFirst).orElse(null);
    };
  }

  @Override
  public DataFetcher<String> directionId() {
    return environment -> {
      Direction direction = getSource(environment).getDirection();
      if (direction == Direction.UNKNOWN) {
        return null;
      }
      return Integer.toString(direction.gtfsCode);
    };
  }

  @Override
  public DataFetcher<Iterable<Iterable<Double>>> geometry() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) {
        return null;
      }

      LineString geometry = tripPattern.getGeometry();
      if (geometry == null) {
        return null;
      }
      return Arrays.stream(geometry.getCoordinateSequence().toCoordinateArray())
        .map(coordinate -> Arrays.asList(coordinate.x, coordinate.y))
        .collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("Trip", getSource(environment).getId().toString());
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return this::getTripPattern;
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).getRoute();
  }

  @Override
  public DataFetcher<String> routeShortName() {
    return environment -> {
      Trip trip = getSource(environment);
      return trip.getRoute().getShortName();
    };
  }

  @Override
  public DataFetcher<String> semanticHash() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) {
        return null;
      }
      return SemanticHash.forTripPattern(tripPattern, getSource(environment));
    };
  }

  @Override
  public DataFetcher<String> serviceId() {
    return environment -> getSource(environment).getServiceId().toString();
  }

  @Override
  public DataFetcher<String> shapeId() {
    return environment ->
      Optional.ofNullable(getSource(environment).getShapeId())
        .map(FeedScopedId::toString)
        .orElse(null);
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return this::getStops;
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stoptimes() {
    return environment ->
      getTransitService(environment).getScheduledTripTimes(getSource(environment)).orElse(null);
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stoptimesForDate() {
    return environment -> {
      TransitService transitService = getTransitService(environment);
      Trip trip = getSource(environment);
      var args = new GraphQLTypes.GraphQLTripStoptimesForDateArgs(environment.getArguments());

      ZoneId timeZone = transitService.getTimeZone();
      LocalDate serviceDate = args.getGraphQLServiceDate() != null
        ? ServiceDateUtils.parseString(args.getGraphQLServiceDate())
        : LocalDate.now(timeZone);

      return transitService.getTripTimeOnDates(trip, serviceDate).orElse(null);
    };
  }

  @Override
  public DataFetcher<Geometry> tripGeometry() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) {
        return null;
      }
      return tripPattern.getGeometry();
    };
  }

  @Override
  public DataFetcher<String> tripHeadsign() {
    return environment ->
      org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
        getSource(environment).getHeadsign(),
        environment
      );
  }

  @Override
  public DataFetcher<String> tripShortName() {
    return environment -> getSource(environment).getShortName();
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLWheelchairBoarding> wheelchairAccessible() {
    return environment -> GraphQLUtils.toGraphQL(getSource(environment).getWheelchairBoarding());
  }

  @Override
  public DataFetcher<TripOccupancy> occupancy() {
    return environment -> {
      Trip trip = getSource(environment);
      return new TripOccupancy(
        getRealtimeVehiclesService(environment).getVehicleOccupancyStatus(trip)
      );
    };
  }

  private List<Object> getStops(DataFetchingEnvironment environment) {
    TripPattern tripPattern = getTripPattern(environment);
    if (tripPattern == null) {
      return List.of();
    }
    return List.copyOf(tripPattern.getStops());
  }

  private Agency getAgency(DataFetchingEnvironment environment) {
    return getRoute(environment).getAgency();
  }

  private Route getRoute(DataFetchingEnvironment environment) {
    return getSource(environment).getRoute();
  }

  private TripPattern getTripPattern(DataFetchingEnvironment environment) {
    return getTransitService(environment).findPattern(environment.getSource());
  }

  private TripPattern getTripPattern(
    DataFetchingEnvironment environment,
    @Nullable LocalDate date
  ) {
    return date == null
      ? getTripPattern(environment)
      : getTransitService(environment).findPattern(environment.getSource(), date);
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private RealtimeVehicleService getRealtimeVehiclesService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().realTimeVehicleService();
  }

  private static Optional<LocalDate> getOptionalServiceDateArgument(
    DataFetchingEnvironment environment
  ) throws ParseException {
    var args = new GraphQLTypes.GraphQLTripArrivalStoptimeArgs(environment.getArguments());
    if (args.getGraphQLServiceDate() != null) {
      return Optional.of(ServiceDateUtils.parseString(args.getGraphQLServiceDate()));
    }
    return Optional.empty();
  }

  private Trip getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
