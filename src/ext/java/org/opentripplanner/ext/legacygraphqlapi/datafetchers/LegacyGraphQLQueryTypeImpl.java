package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLCauseMapper.getLegacyGraphQLCause;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLEffectMapper.getLegacyGraphQLEffect;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLSeverityMapper.getLegacyGraphQLSeverity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import graphql.execution.DataFetcherResult;
import graphql.relay.Connection;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.util.ResourceBundleSingleton;

public class LegacyGraphQLQueryTypeImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLQueryType {

  @Override
  public DataFetcher<Object> node() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeNodeArgs(environment.getArguments());
      String type = args.getLegacyGraphQLId().getType();
      String id = args.getLegacyGraphQLId().getId();
      RoutingService routingService = environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
      VehicleParkingService vehicleParkingService = routingService.getVehicleParkingService();
      VehicleRentalStationService vehicleRentalStationService = routingService.getVehicleRentalStationService();

      switch (type) {
        case "Agency":
          return routingService.getAgencyForId(FeedScopedId.parseId(id));
        case "Alert":
          return null; //TODO
        case "BikePark":
          var bikeParkId = FeedScopedId.parseId(id);
          return vehicleParkingService == null ? null : vehicleParkingService
              .getBikeParks()
              .filter(bikePark -> bikePark.getId().equals(bikeParkId))
              .findAny()
              .orElse(null);
        case "BikeRentalStation":
          return vehicleRentalStationService == null ? null :
              vehicleRentalStationService.getVehicleRentalPlace(FeedScopedId.parseId(id));
        case "VehicleRentalStation":
          return vehicleRentalStationService == null ? null :
              vehicleRentalStationService.getVehicleRentalStation(FeedScopedId.parseId(id));
        case "RentalVehicle":
          return vehicleRentalStationService == null ? null :
              vehicleRentalStationService.getVehicleRentalVehicle(FeedScopedId.parseId(id));
        case "CarPark":
          var carParkId = FeedScopedId.parseId(id);
          return vehicleParkingService == null ? null : vehicleParkingService
              .getCarParks()
              .filter(carPark -> carPark.getId().equals(carParkId))
              .findAny()
              .orElse(null);
        case "Cluster":
          return null; //TODO
        case "DepartureRow":
          return PatternAtStop.fromId(routingService, id);
        case "Pattern":
          return routingService.getTripPatternForId(FeedScopedId.parseId(id));
        case "placeAtDistance": {
          String[] parts = id.split(";");

          Relay.ResolvedGlobalId internalId = new Relay().fromGlobalId(parts[1]);

          Object place = node().get(DataFetchingEnvironmentImpl
              .newDataFetchingEnvironment(environment)
              .source(new Object())
              .arguments(Map.of("id", internalId))
              .build());

          return new PlaceAtDistance(place, Double.parseDouble(parts[0]));
        }
        case "Route":
          return routingService.getRouteForId(FeedScopedId.parseId(id));
        case "Stop":
          return routingService.getStopForId(FeedScopedId.parseId(id));
        case "Stoptime":
          return null; //TODO
        case "stopAtDistance": {
          String[] parts = id.split(";");
          var stop = routingService.getStopForId(FeedScopedId.parseId(parts[1]));

          // TODO: Add geometry
          return new NearbyStop(stop, Integer.parseInt(parts[0]), null, null, null);
        }
        case "TicketType":
          return null; //TODO
        case "Trip":
          var scopedId = FeedScopedId.parseId(id);
          var trip = routingService.getTripForId().get(scopedId);
          return trip;
        case "VehicleParking":
          var vehicleParkingId = FeedScopedId.parseId(id);
          return vehicleParkingService == null ? null : vehicleParkingService
                  .getVehicleParkings()
                  .filter(bikePark -> bikePark.getId().equals(vehicleParkingId))
                  .findAny()
                  .orElse(null);
        default:
          return null;
      }
    };
  }

  @Override
  public DataFetcher<Iterable<String>> feeds() {
    return environment -> getRoutingService(environment).getFeedIds();
  }

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> getRoutingService(environment).getAgencies();
  }

  //TODO
  @Override
  public DataFetcher<Iterable<FareRuleSet>> ticketTypes() {
    return environment -> null;
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

      var stopStream = routingService.getAllStops().stream();

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

      Envelope envelope = new Envelope(
          new Coordinate(args.getLegacyGraphQLMinLon(), args.getLegacyGraphQLMinLat()),
          new Coordinate(args.getLegacyGraphQLMaxLon(), args.getLegacyGraphQLMaxLat())
      );

      Stream<Stop> stopStream = getRoutingService(environment)
          .getStopSpatialIndex()
          .query(envelope)
          .stream()
          .filter(transitStopVertex -> envelope.contains(transitStopVertex.getCoordinate()))
          .map(TransitStopVertex::getStop);

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feedIds = Lists.newArrayList(args.getLegacyGraphQLFeeds());
        stopStream = stopStream.filter(stop -> feedIds.contains(stop.getId().getFeedId()));
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Connection<NearbyStop>> stopsByRadius() {
    return environment -> {
      LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByRadiusArgs args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByRadiusArgs(environment.getArguments());

      List<NearbyStop> stops;
      try {
        stops = getRoutingService(environment).findClosestStops(
            args.getLegacyGraphQLLat(),
            args.getLegacyGraphQLLon(),
            args.getLegacyGraphQLRadius()
        );
      }
      catch (RoutingValidationException e) {
        stops = Collections.emptyList();
      }

      return new SimpleListConnection<>(stops).get(environment);
    };
  }

  @Override
  public DataFetcher<Connection<PlaceAtDistance>> nearest() {
    return environment -> {
      List<FeedScopedId> filterByStops = null;
      List<FeedScopedId> filterByRoutes = null;
      List<String> filterByBikeRentalStations = null;
      List<String> filterByBikeParks = null;
      List<String> filterByCarParks = null;

      LegacyGraphQLTypes.LegacyGraphQLQueryTypeNearestArgs args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeNearestArgs(
          environment.getArguments());

      LegacyGraphQLTypes.LegacyGraphQLInputFiltersInput filterByIds = args.getLegacyGraphQLFilterByIds();

      if (filterByIds != null) {
        filterByStops = filterByIds.getLegacyGraphQLStops() != null ? StreamSupport
            .stream(filterByIds.getLegacyGraphQLStops().spliterator(), false)
            .map(FeedScopedId::parseId)
            .collect(Collectors.toList()) : null;
        filterByRoutes = filterByIds.getLegacyGraphQLRoutes() != null ? StreamSupport
            .stream(filterByIds.getLegacyGraphQLRoutes().spliterator(), false)
            .map(FeedScopedId::parseId)
            .collect(Collectors.toList()) : null;
        filterByBikeRentalStations = filterByIds.getLegacyGraphQLBikeRentalStations() != null
            ? Lists.newArrayList(filterByIds.getLegacyGraphQLBikeRentalStations())
            : null;
        filterByBikeParks = filterByIds.getLegacyGraphQLBikeParks() != null ? Lists.newArrayList(
            filterByIds.getLegacyGraphQLBikeParks()) : null;
        filterByCarParks = filterByIds.getLegacyGraphQLCarParks() != null ? Lists.newArrayList(
            filterByIds.getLegacyGraphQLCarParks()) : null;
      }

      List<TransitMode> filterByModes = args.getLegacyGraphQLFilterByModes() != null ? StreamSupport
          .stream(args.getLegacyGraphQLFilterByModes().spliterator(), false)
          .map(mode -> {
            try { return TransitMode.valueOf(mode.name()); }
            catch (IllegalArgumentException ignored) { return null; }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList()) : null;
      List<PlaceType> filterByPlaceTypes =
          args.getLegacyGraphQLFilterByPlaceTypes() != null ? StreamSupport
              .stream(args.getLegacyGraphQLFilterByPlaceTypes().spliterator(), false)
              .map(placeType -> placeType.name())
              .map(placeType -> placeType.equals("DEPARTURE_ROW") ? "PATTERN_AT_STOP" : placeType)
              .map(PlaceType::valueOf)
              .collect(Collectors.toList()) : null;

      List<PlaceAtDistance> places;
      try {
        places = new ArrayList<>(getRoutingService(environment).findClosestPlaces(
            args.getLegacyGraphQLLat(),
            args.getLegacyGraphQLLon(),
            args.getLegacyGraphQLMaxDistance(),
            args.getLegacyGraphQLMaxResults(),
            filterByModes,
            filterByPlaceTypes,
            filterByStops,
            filterByRoutes,
            filterByBikeRentalStations,
            filterByBikeParks,
            filterByCarParks,
            getRoutingService(environment)
        ));
      }
      catch (RoutingValidationException e) {
        places = Collections.emptyList();
      }

      return new SimpleListConnection<>(places).get(environment);
    };
  }

  @Override
  public DataFetcher<PatternAtStop> departureRow() {
    return environment -> PatternAtStop.fromId(
        getRoutingService(environment),
        new LegacyGraphQLTypes.LegacyGraphQLQueryTypeDepartureRowArgs(environment.getArguments()).getLegacyGraphQLId()
    );
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
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStationsArgs(environment.getArguments());

      RoutingService routingService = getRoutingService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return StreamSupport
                .stream(args.getLegacyGraphQLIds().spliterator(), false)
                .map(FeedScopedId::parseId)
                .map(routingService::getStationById)
                .collect(Collectors.toList());
      }

      Stream<Station> stationStream = routingService.getStations().stream();

      if (args.getLegacyGraphQLName() != null) {
        String name = args.getLegacyGraphQLName().toLowerCase(environment.getLocale());
        stationStream = stationStream.filter(station -> station
                .getName()
                .toLowerCase(environment.getLocale())
                .startsWith(name));
      }

      return stationStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRoutesArgs(environment.getArguments());

      RoutingService routingService = getRoutingService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return StreamSupport
                .stream(args.getLegacyGraphQLIds().spliterator(), false)
                .map(FeedScopedId::parseId)
                .map(routingService::getRouteForId)
                .collect(Collectors.toList());
      }

      Stream<Route> routeStream = routingService.getAllRoutes().stream();

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feeds = StreamSupport.stream(args.getLegacyGraphQLFeeds().spliterator(), false).collect(Collectors.toList());
        routeStream = routeStream.filter(route -> feeds.contains(route.getId().getFeedId()));
      }

      if (args.getLegacyGraphQLTransportModes() != null) {
        List<TransitMode> modes = StreamSupport
                .stream(args.getLegacyGraphQLTransportModes().spliterator(), false)
                .map(mode -> TransitMode.valueOf(mode.name()))
                .collect(Collectors.toList());
        routeStream = routeStream.filter(route -> modes.contains(route.getMode()));
      }

      if (args.getLegacyGraphQLName() != null) {
        String name = args.getLegacyGraphQLName().toLowerCase(environment.getLocale());
        routeStream = routeStream.filter(route -> Stream
            .of(route.getShortName(), route.getLongName())
            .filter(Objects::nonNull)
            .map(s -> s.toLowerCase(environment.getLocale()))
            .anyMatch(s -> s.startsWith(name)));
      }

      return routeStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getRoutingService(environment)
            .getRouteForId(FeedScopedId.parseId(
                    new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRouteArgs(environment.getArguments())
                            .getLegacyGraphQLId()));
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeTripsArgs(environment.getArguments());

      Stream<Trip> tripStream = getRoutingService(environment).getTripForId().values().stream();

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feeds = StreamSupport.stream(args.getLegacyGraphQLFeeds().spliterator(), false).collect(Collectors.toList());
        tripStream = tripStream.filter(trip -> feeds.contains(trip.getId().getFeedId()));
      }

      return tripStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getRoutingService(environment)
            .getTripForId().get(FeedScopedId.parseId(
                    new LegacyGraphQLTypes.LegacyGraphQLQueryTypeTripArgs(environment.getArguments())
                            .getLegacyGraphQLId()));
  }

  @Override
  public DataFetcher<Trip> fuzzyTrip() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeFuzzyTripArgs(environment.getArguments());

      RoutingService routingService = getRoutingService(environment);

      return new GtfsRealtimeFuzzyTripMatcher(routingService).getTrip(
              routingService.getRouteForId(FeedScopedId.parseId(args.getLegacyGraphQLRoute())),
              args.getLegacyGraphQLDirection(),
              args.getLegacyGraphQLTime(),
              ServiceDate.parseString(args.getLegacyGraphQLDate())
      );
    };
  }

  // TODO
  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> cancelledTripTimes() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> getRoutingService(environment).getTripPatterns();
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return environment -> getRoutingService(environment)
            .getTripPatternForId(FeedScopedId.parseId(
                    new LegacyGraphQLTypes.LegacyGraphQLQueryTypePatternArgs(environment.getArguments())
                            .getLegacyGraphQLId()));
  }

  @Override
  public DataFetcher<Iterable<Object>> clusters() {
    return environment -> Collections.EMPTY_LIST;
  }

  @Override
  public DataFetcher<Object> cluster() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      Collection<TransitAlert> alerts = getRoutingService(environment)
              .getTransitAlertService()
              .getAllAlerts();
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(
              environment.getArguments());
      List<String> severities = args.getLegacyGraphQLSeverityLevel() == null
              ? null
              : ((List<LegacyGraphQLTypes.LegacyGraphQLAlertSeverityLevelType>) args.getLegacyGraphQLSeverityLevel()).stream()
                      .map(severity -> severity.name())
                      .collect(Collectors.toList());
      List<String> effects = args.getLegacyGraphQLEffect() == null
              ? null
              : ((List<LegacyGraphQLTypes.LegacyGraphQLAlertEffectType>) args.getLegacyGraphQLEffect()).stream()
                      .map(effect -> effect.name())
                      .collect(Collectors.toList());
      List<String> causes = args.getLegacyGraphQLCause() == null
              ? null
              : ((List<LegacyGraphQLTypes.LegacyGraphQLAlertCauseType>) args.getLegacyGraphQLCause()).stream()
                      .map(cause -> cause.name())
                      .collect(Collectors.toList());
      return alerts.stream()
              .filter(alert -> args.getLegacyGraphQLFeeds() == null
                      || ((List<String>) args.getLegacyGraphQLFeeds()).contains(alert.getFeedId()))
              .filter(alert -> args.getLegacyGraphQLSeverityLevel() == null
                      || severities.contains(getLegacyGraphQLSeverity(alert.severity)))
              .filter(alert -> args.getLegacyGraphQLEffect() == null
                      || effects.contains(getLegacyGraphQLEffect(alert.effect)))
              .filter(alert -> args.getLegacyGraphQLCause() == null
                      || causes.contains(getLegacyGraphQLCause(alert.cause)))
              .filter(alert -> args.getLegacyGraphQLRoute() == null
                      || alert.getEntities()
                      .stream()
                      .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
                      .map(EntitySelector.Route.class::cast)
                      .anyMatch(route -> ((List<String>) args.getLegacyGraphQLRoute()).contains(
                              route.routeId.toString())))
              .filter(alert -> args.getLegacyGraphQLStop() == null
                      || alert.getEntities()
                      .stream()
                      .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
                      .map(EntitySelector.Stop.class::cast)
                      .anyMatch(stop -> ((List<String>) args.getLegacyGraphQLStop()).contains(
                              stop.stopId.toString())))
              .collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Object> serviceTimeRange() {
    return environment -> new Object();
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalPlace>> bikeRentalStations() {
    return environment -> {
      VehicleRentalStationService vehicleRentalStationService = getRoutingService(environment)
              .getVehicleRentalStationService();

      if (vehicleRentalStationService == null) { return null; }

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeRentalStationsArgs(
              environment.getArguments());

      if (args.getLegacyGraphQLIds() != null) {
        ArrayListMultimap<String, VehicleRentalPlace> vehicleRentalStations =
                vehicleRentalStationService.getVehicleRentalPlaces()
                        .stream()
                        .collect(Multimaps.toMultimap(VehicleRentalPlace::getStationId, station -> station, ArrayListMultimap::create));
        return ((List<String>) args.getLegacyGraphQLIds())
                .stream()
                .flatMap(id -> vehicleRentalStations.get(id).stream())
                .collect(Collectors.toList());
      }

      return vehicleRentalStationService.getVehicleRentalPlaces();
    };
  }

  @Override
  public DataFetcher<VehicleRentalPlace> bikeRentalStation() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeRentalStationArgs(environment.getArguments());

      VehicleRentalStationService vehicleRentalStationService = getRoutingService(environment)
          .getVehicleRentalStationService();

      if (vehicleRentalStationService == null) { return null; }

      return vehicleRentalStationService
              .getVehicleRentalPlaces()
              .stream()
              .filter(vehicleRentalStation -> vehicleRentalStation.getStationId().equals(args.getLegacyGraphQLId()))
              .findAny()
              .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalStation>> vehicleRentalStations() {
    return environment -> {
      VehicleRentalStationService vehicleRentalStationService = getRoutingService(environment)
              .getVehicleRentalStationService();

      if (vehicleRentalStationService == null) {
        return null;
      }

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleRentalStationsArgs(
              environment.getArguments());

      if (args.getLegacyGraphQLIds() != null) {
        ArrayListMultimap<String, VehicleRentalStation> vehicleRentalStations =
                vehicleRentalStationService.getVehicleRentalStations()
                        .stream()
                        .collect(Multimaps.toMultimap(station -> station.getId().toString(),
                                station -> station, ArrayListMultimap::create
                        ));
        return ((List<String>) args.getLegacyGraphQLIds())
                .stream()
                .flatMap(id -> vehicleRentalStations.get(id).stream())
                .collect(Collectors.toList());
      }

      return vehicleRentalStationService.getVehicleRentalStations();
    };
  }

  @Override
  public DataFetcher<VehicleRentalStation> vehicleRentalStation() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleRentalStationArgs(
              environment.getArguments());

      VehicleRentalStationService vehicleRentalStationService = getRoutingService(environment)
              .getVehicleRentalStationService();

      if (vehicleRentalStationService == null) {
        return null;
      }

      return vehicleRentalStationService
              .getVehicleRentalStations()
              .stream()
              .filter(vehicleRentalStation -> vehicleRentalStation.getId()
                      .toString()
                      .equals(args.getLegacyGraphQLId()))
              .findAny()
              .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalVehicle>> rentalVehicles() {
    return environment -> {
      VehicleRentalStationService vehicleRentalStationService = getRoutingService(environment)
              .getVehicleRentalStationService();

      if (vehicleRentalStationService == null) {
        return null;
      }

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRentalVehiclesArgs(
              environment.getArguments());

      if (args.getLegacyGraphQLIds() != null) {
        ArrayListMultimap<String, VehicleRentalVehicle> vehicleRentalVehicles =
                vehicleRentalStationService.getVehicleRentalVehicles()
                        .stream()
                        .collect(Multimaps.toMultimap(vehicle -> vehicle.getId().toString(),
                                vehicle -> vehicle, ArrayListMultimap::create
                        ));
        return ((List<String>) args.getLegacyGraphQLIds())
                .stream()
                .flatMap(id -> vehicleRentalVehicles.get(id).stream())
                .collect(Collectors.toList());
      }

      return vehicleRentalStationService.getVehicleRentalVehicles();
    };
  }

  @Override
  public DataFetcher<VehicleRentalVehicle> rentalVehicle() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRentalVehicleArgs(
              environment.getArguments());

      VehicleRentalStationService vehicleRentalStationService = getRoutingService(environment)
              .getVehicleRentalStationService();

      if (vehicleRentalStationService == null) {
        return null;
      }

      return vehicleRentalStationService
              .getVehicleRentalVehicles()
              .stream()
              .filter(vehicleRentalVehicle -> vehicleRentalVehicle.getId()
                      .toString()
                      .equals(args.getLegacyGraphQLId()))
              .findAny()
              .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> bikeParks() {
    return environment -> {
      VehicleParkingService vehicleParkingService = getRoutingService(environment)
          .getVehicleParkingService();

      if (vehicleParkingService == null) { return null; }

      return vehicleParkingService.getBikeParks().collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleParking> bikePark() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeParkArgs(environment.getArguments());

      VehicleParkingService vehicleParkingService = getRoutingService(environment)
          .getVehicleParkingService();

      if (vehicleParkingService == null) { return null; }

      return vehicleParkingService
              .getBikeParks()
              .filter(bikePark -> bikePark.getId().getId().equals(args.getLegacyGraphQLId()))
              .findAny()
              .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> carParks() {
    return environment -> {
      VehicleParkingService vehicleParkingService = getRoutingService(environment)
          .getVehicleParkingService();

      if (vehicleParkingService == null) { return null; }

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeCarParksArgs(
          environment.getArguments());

      if (args.getLegacyGraphQLIds() != null) {
        var idList = ((List<String>) args.getLegacyGraphQLIds());

        if (!idList.isEmpty()) {
          Map<String, VehicleParking> carParkMap = vehicleParkingService.getCarParks()
                  .collect(Collectors.toMap(station -> station.getId().getId(), station -> station));

          return idList.stream()
              .map(carParkMap::get)
              .collect(Collectors.toList());
        }
      }

      return vehicleParkingService
          .getCarParks().collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleParking> carPark() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeCarParkArgs(environment.getArguments());

      VehicleParkingService vehicleParkingService = getRoutingService(environment)
          .getVehicleParkingService();

      if (vehicleParkingService == null) { return null; }

      return vehicleParkingService
          .getCarParks()
          .filter(carPark -> carPark.getId().getId().equals(args.getLegacyGraphQLId()))
          .findAny()
          .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> vehicleParkings() {
    return environment -> {
      VehicleParkingService vehicleParkingService = getRoutingService(environment)
          .getVehicleParkingService();

      if (vehicleParkingService == null) { return null; }

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleParkingsArgs(
          environment.getArguments());

      if (args.getLegacyGraphQLIds() != null) {
        var idList = ((List<String>) args.getLegacyGraphQLIds());

        if (!idList.isEmpty()) {
          Map<String, VehicleParking> vehicleParkingMap = vehicleParkingService.getVehicleParkings()
                  .collect(Collectors.toMap(station -> station.getId().toString(), station -> station));

          return idList.stream()
              .map(vehicleParkingMap::get)
              .collect(Collectors.toList());
        }
      }

      return vehicleParkingService
          .getVehicleParkings().collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleParking> vehicleParking() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleParkingArgs(environment.getArguments());

      VehicleParkingService vehicleParkingService = getRoutingService(environment)
          .getVehicleParkingService();

      if (vehicleParkingService == null) { return null; }

      var vehicleParkingId = FeedScopedId.parseId(args.getLegacyGraphQLId());
      return vehicleParkingService
          .getVehicleParkings()
          .filter(vehicleParking -> vehicleParking.getId().equals(vehicleParkingId))
          .findAny()
          .orElse(null);
    };
  }

  @Override
  public DataFetcher<Object> viewer() {
    return environment -> new Object();
  }

  @Override
  public DataFetcher<DataFetcherResult<RoutingResponse>> plan() {
    return environment -> {
      LegacyGraphQLRequestContext context = environment.<LegacyGraphQLRequestContext>getContext();
      RoutingRequest request = context.getRouter().defaultRoutingRequest.clone();

      CallerWithEnvironment callWith = new CallerWithEnvironment(environment);

      callWith.argument("fromPlace", request::setFromString);
      callWith.argument("toPlace", request::setToString);

      callWith.argument("from", (Map<String, Object> v) -> request.from = toGenericLocation(v));
      callWith.argument("to", (Map<String, Object> v) -> request.to = toGenericLocation(v));

      request.setDateTime(
          environment.getArgument("date"),
          environment.getArgument("time"),
          context.getRouter().graph.getTimeZone()
      );

      callWith.argument("wheelchair", request::setWheelchairAccessible);
      callWith.argument("numItineraries", request::setNumItineraries);
      callWith.argument("searchWindow", (Long m) -> request.searchWindow = Duration.ofSeconds(m));
      callWith.argument("pageCursor", request::setPageCursor);
      // callWith.argument("maxSlope", request::setMaxSlope);
      // callWith.argument("carParkCarLegWeight", request::setCarParkCarLegWeight);
      // callWith.argument("itineraryFiltering", request::setItineraryFiltering);
      callWith.argument("bikeReluctance", request::setBikeReluctance);
      callWith.argument("bikeWalkingReluctance", request::setBikeWalkingReluctance);
      callWith.argument("carReluctance", request::setCarReluctance);
      callWith.argument("walkReluctance", request::setWalkReluctance);
      // callWith.argument("walkOnStreetReluctance", request::setWalkOnStreetReluctance);
      callWith.argument("waitReluctance", request::setWaitReluctance);
      callWith.argument("waitAtBeginningFactor", request::setWaitAtBeginningFactor);
      callWith.argument("walkSpeed", (Double v) -> request.walkSpeed = v);
      callWith.argument("bikeWalkingSpeed", (Double v) -> request.bikeWalkingSpeed = v);
      callWith.argument("bikeSpeed", (Double v) -> request.bikeSpeed = v);
      callWith.argument("bikeSwitchTime", (Integer v) -> request.bikeSwitchTime = v);
      callWith.argument("bikeSwitchCost", (Integer v) -> request.bikeSwitchCost = v);
      callWith.argument("allowKeepingRentedBicycleAtDestination", (Boolean v) -> request.allowKeepingRentedVehicleAtDestination = v);
      callWith.argument("keepingRentedBicycleAtDestinationCost", (Integer v) -> request.keepingRentedVehicleAtDestinationCost = v);

      callWith.argument(
              "modeWeight", (Map<String, Object> v) -> request.setTransitReluctanceForMode(
                      v.entrySet()
                              .stream()
                              .collect(Collectors.toMap(e -> TransitMode.valueOf(e.getKey()),
                                      e -> (Double) e.getValue()
                              ))));
      callWith.argument("debugItineraryFilter", (Boolean v) -> request.itineraryFilters.debug = v);
      callWith.argument("arriveBy", request::setArriveBy);
      request.showIntermediateStops = true;
      callWith.argument("intermediatePlaces", (List<Map<String, Object>> v) -> request.intermediatePlaces = v.stream().map(LegacyGraphQLQueryTypeImpl::toGenericLocation).collect(Collectors.toList()));
      callWith.argument("preferred.routes", request::setPreferredRoutesFromSting);
      callWith.argument("preferred.otherThanPreferredRoutesPenalty", request::setOtherThanPreferredRoutesPenalty);
      callWith.argument("preferred.agencies", request::setPreferredAgenciesFromString);
      callWith.argument("unpreferred.routes", request::setUnpreferredRoutesFromSting);
      callWith.argument("unpreferred.agencies", request::setUnpreferredAgenciesFromString);
      // callWith.argument("unpreferred.useUnpreferredRoutesPenalty", request::setUseUnpreferredRoutesPenalty);
      callWith.argument("walkBoardCost", request::setWalkBoardCost);
      callWith.argument("bikeBoardCost", request::setBikeBoardCost);
      callWith.argument("banned.routes", request::setBannedRoutesFromSting);
      callWith.argument("banned.agencies", request::setBannedAgenciesFromSting);
      // callWith.argument("banned.trips", (String v) -> request.bannedTrips = RoutingResource.makeBannedTripMap(v));
      // callWith.argument("banned.stops", request::setBannedStops);
      // callWith.argument("banned.stopsHard", request::setBannedStopsHard);
      callWith.argument("transferPenalty", (Integer v) -> request.transferCost = v);
      // callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
      // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> request.compactLegsByReversedSearch = v);

      if (environment.getArgument("optimize") != null) {
        BicycleOptimizeType optimize = BicycleOptimizeType.valueOf(environment.getArgument("optimize"));

        if (optimize == BicycleOptimizeType.TRIANGLE) {

          // because we must use a final variable in the lambda we have to use this ugly crutch.
          // Arguments: [ safety, slope, time ]
          final double[] args = new double[3];

          callWith.argument("triangle.safetyFactor", (Double v) -> args[0] = v);
          callWith.argument("triangle.slopeFactor", (Double v) -> args[1] = v);
          callWith.argument("triangle.timeFactor", (Double v) -> args[2] = v);

          request.setTriangleNormalized(args[0], args[1], args[2]);
        }

        if (optimize == BicycleOptimizeType.TRANSFERS) {
          optimize = BicycleOptimizeType.QUICK;
          request.transferCost += 1800;
        }

        if (optimize != null) {
          request.bicycleOptimizeType = optimize;
        }
      }


      if (hasArgument(environment, "transportModes")) {
        QualifiedModeSet modes = new QualifiedModeSet("WALK");

        modes.qModes = environment.<List<Map<String, String>>>getArgument("transportModes")
            .stream()
            .map(transportMode -> new QualifiedMode(transportMode.get("mode") + (transportMode.get("qualifier") == null ? "" : "_" + transportMode.get("qualifier"))))
            .collect(Collectors.toSet());

        request.modes = modes.getRequestModes();
      }

      if (hasArgument(environment,  "allowedTicketTypes")) {
        // request.allowedFares = Sets.newHashSet();
        // ((List<String>)environment.getArgument("allowedTicketTypes")).forEach(ticketType -> request.allowedFares.add(ticketType.replaceFirst("_", ":")));
      }

      if (hasArgument(environment, "allowedBikeRentalNetworks")) {
        // ArrayList<String> allowedBikeRentalNetworks = environment.getArgument("allowedBikeRentalNetworks");
        // request.allowedBikeRentalNetworks = new HashSet<>(allowedBikeRentalNetworks);
      }

      if (request.vehicleRental && !hasArgument(environment, "bikeSpeed")) {
        //slower bike speed for bike sharing, based on empirical evidence from DC.
        request.bikeSpeed = 4.3;
      }

      callWith.argument("boardSlack", (Integer v) -> request.boardSlack = v);
      callWith.argument("alightSlack", (Integer v) -> request.alightSlack = v);
      callWith.argument("minTransferTime", (Integer v) -> request.transferSlack = v); // TODO RoutingRequest field should be renamed
      callWith.argument("nonpreferredTransferPenalty", (Integer v) -> request.nonpreferredTransferCost = v);

      callWith.argument("maxTransfers", (Integer v) -> request.maxTransfers = v);

      request.useVehicleRentalAvailabilityInformation = request.isTripPlannedForNow();

      callWith.argument("startTransitStopId", (String v) -> request.startingTransitStopId = FeedScopedId.parseId(v));
      callWith.argument("startTransitTripId", (String v) -> request.startingTransitTripId = FeedScopedId.parseId(v));
      //callWith.argument("reverseOptimizeOnTheFly", (Boolean v) -> request.reverseOptimizeOnTheFly = v);
      //callWith.argument("omitCanceled", (Boolean v) -> request.omitCanceled = v);
      callWith.argument("ignoreRealtimeUpdates", (Boolean v) -> request.ignoreRealtimeUpdates = v);
      callWith.argument("disableRemainingWeightHeuristic", (Boolean v) -> request.disableRemainingWeightHeuristic = v);

      callWith.argument("locale", (String v) -> request.locale = ResourceBundleSingleton.INSTANCE.getLocale(v));
      RoutingResponse res = context.getRoutingService().route(request, context.getRouter());
      return DataFetcherResult.<RoutingResponse>newResult().data(res).localContext(Map.of("locale", request.locale)).build();
    };
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private static <T> void call(Map<String, T> m, String name, Consumer<T> consumer) {
    if (!name.contains(".")) {
      if (hasArgument(m, name)) {
        T v = m.get(name);
        consumer.accept(v);
      }
    } else {
      String[] parts = name.split("\\.");
      if (hasArgument(m, parts[0])) {
        Map<String, T> nm = (Map<String, T>) m.get(parts[0]);
        call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
      }
    }
  }

  private static <T> void call(DataFetchingEnvironment environment, String name, Consumer<T> consumer) {
    if (!name.contains(".")) {
      if (hasArgument(environment, name)) {
        consumer.accept(environment.getArgument(name));
      }
    } else {
      String[] parts = name.split("\\.");
      if (hasArgument(environment, parts[0])) {
        Map<String, T> nm = environment.getArgument(parts[0]);
        call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
      }
    }
  }

  private static class CallerWithEnvironment {
    private final DataFetchingEnvironment environment;

    public CallerWithEnvironment(DataFetchingEnvironment e) {
      this.environment = e;
    }

    private <T> void argument(String name, Consumer<T> consumer) {
      call(environment, name, consumer);
    }
  }

  private static GenericLocation toGenericLocation(Map<String, Object> m) {
    double lat = (double) m.get("lat");
    double lng = (double) m.get("lon");
    String address = (String) m.get("address");
    Integer locationSlack = null; // (Integer) m.get("locationSlack");

    if (address != null) {
      return new GenericLocation(address, null, lat, lng);
    }

    return new GenericLocation(lat, lng);
  }

  private static boolean hasArgument(DataFetchingEnvironment environment, String name) {
    return environment.containsArgument(name) && environment.getArgument(name) != null;
  }

  public static <T> boolean hasArgument(Map<String, T> m, String name) {
    return m.containsKey(name) && m.get(name) != null;
  }
}
