package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLCauseMapper.getLegacyGraphQLCause;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLEffectMapper.getLegacyGraphQLEffect;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLSeverityMapper.getLegacyGraphQLSeverity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import graphql.execution.DataFetcherResult;
import graphql.relay.Connection;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.ext.fares.impl.GtfsFaresService;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLUtils;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.ext.legacygraphqlapi.mapping.RouteRequestMapper;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.DirectionMapper;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;

public class LegacyGraphQLQueryTypeImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLQueryType {

  // TODO: figure out a runtime solution
  private static final DirectionMapper DIRECTION_MAPPER = new DirectionMapper(
    DataImportIssueStore.NOOP
  );

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> getTransitService(environment).getAgencies();
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> {
      FeedScopedId id = FeedScopedId.parseId(
        new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAgencyArgs(environment.getArguments())
          .getLegacyGraphQLId()
      );

      return getTransitService(environment).getAgencyForId(id);
    };
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      Collection<TransitAlert> alerts = getTransitService(environment)
        .getTransitAlertService()
        .getAllAlerts();
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(
        environment.getArguments()
      );
      return filterAlerts(alerts, args);
    };
  }

  @Override
  public DataFetcher<VehicleParking> bikePark() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeParkArgs(
        environment.getArguments()
      );

      VehicleParkingService vehicleParkingService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleParkingService();

      return vehicleParkingService
        .getBikeParks()
        .filter(bikePark -> bikePark.getId().getId().equals(args.getLegacyGraphQLId()))
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> bikeParks() {
    return environment -> {
      VehicleParkingService vehicleParkingService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleParkingService();

      return vehicleParkingService.getBikeParks().collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleRentalPlace> bikeRentalStation() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeRentalStationArgs(
        environment.getArguments()
      );

      VehicleRentalService vehicleRentalStationService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleRentalService();

      return vehicleRentalStationService
        .getVehicleRentalPlaces()
        .stream()
        .filter(vehicleRentalStation ->
          vehicleRentalStation.getStationId().equals(args.getLegacyGraphQLId())
        )
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalPlace>> bikeRentalStations() {
    return environment -> {
      VehicleRentalService vehicleRentalStationService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleRentalService();

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeRentalStationsArgs(
        environment.getArguments()
      );

      if (args.getLegacyGraphQLIds() != null) {
        ArrayListMultimap<String, VehicleRentalPlace> vehicleRentalStations = vehicleRentalStationService
          .getVehicleRentalPlaces()
          .stream()
          .collect(
            Multimaps.toMultimap(
              VehicleRentalPlace::getStationId,
              station -> station,
              ArrayListMultimap::create
            )
          );
        return args
          .getLegacyGraphQLIds()
          .stream()
          .flatMap(id -> vehicleRentalStations.get(id).stream())
          .collect(Collectors.toList());
      }

      return vehicleRentalStationService.getVehicleRentalPlaces();
    };
  }

  // TODO
  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> cancelledTripTimes() {
    return environment -> null;
  }

  @Override
  public DataFetcher<VehicleParking> carPark() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeCarParkArgs(
        environment.getArguments()
      );

      VehicleParkingService vehicleParkingService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleParkingService();

      return vehicleParkingService
        .getCarParks()
        .filter(carPark -> carPark.getId().getId().equals(args.getLegacyGraphQLId()))
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> carParks() {
    return environment -> {
      VehicleParkingService vehicleParkingService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleParkingService();

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeCarParksArgs(
        environment.getArguments()
      );

      if (args.getLegacyGraphQLIds() != null) {
        var idList = args.getLegacyGraphQLIds();

        if (!idList.isEmpty()) {
          Map<String, VehicleParking> carParkMap = vehicleParkingService
            .getCarParks()
            .collect(Collectors.toMap(station -> station.getId().getId(), station -> station));

          return idList.stream().map(carParkMap::get).collect(Collectors.toList());
        }
      }

      return vehicleParkingService.getCarParks().collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Object> cluster() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Iterable<Object>> clusters() {
    return environment -> Collections.EMPTY_LIST;
  }

  @Override
  public DataFetcher<PatternAtStop> departureRow() {
    return environment ->
      PatternAtStop.fromId(
        getTransitService(environment),
        new LegacyGraphQLTypes.LegacyGraphQLQueryTypeDepartureRowArgs(environment.getArguments())
          .getLegacyGraphQLId()
      );
  }

  @Override
  public DataFetcher<Iterable<String>> feeds() {
    return environment -> getTransitService(environment).getFeedIds();
  }

  @Override
  public DataFetcher<Trip> fuzzyTrip() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeFuzzyTripArgs(
        environment.getArguments()
      );

      TransitService transitService = getTransitService(environment);

      return new GtfsRealtimeFuzzyTripMatcher(transitService)
        .getTrip(
          transitService.getRouteForId(FeedScopedId.parseId(args.getLegacyGraphQLRoute())),
          DIRECTION_MAPPER.map(args.getLegacyGraphQLDirection()),
          args.getLegacyGraphQLTime(),
          ServiceDateUtils.parseString(args.getLegacyGraphQLDate())
        );
    };
  }

  @Override
  public DataFetcher<Connection<PlaceAtDistance>> nearest() {
    return environment -> {
      List<FeedScopedId> filterByStops = null;
      List<FeedScopedId> filterByRoutes = null;
      List<String> filterByBikeRentalStations = null;
      // TODO implement
      List<String> filterByBikeParks = null;
      List<String> filterByCarParks = null;

      LegacyGraphQLTypes.LegacyGraphQLQueryTypeNearestArgs args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeNearestArgs(
        environment.getArguments()
      );

      LegacyGraphQLTypes.LegacyGraphQLInputFiltersInput filterByIds = args.getLegacyGraphQLFilterByIds();

      if (filterByIds != null) {
        filterByStops =
          filterByIds.getLegacyGraphQLStops() != null
            ? filterByIds
              .getLegacyGraphQLStops()
              .stream()
              .map(FeedScopedId::parseId)
              .collect(Collectors.toList())
            : null;
        filterByRoutes =
          filterByIds.getLegacyGraphQLRoutes() != null
            ? filterByIds
              .getLegacyGraphQLRoutes()
              .stream()
              .map(FeedScopedId::parseId)
              .collect(Collectors.toList())
            : null;
        filterByBikeRentalStations = filterByIds.getLegacyGraphQLBikeRentalStations();
        filterByBikeParks = filterByIds.getLegacyGraphQLBikeParks();
        filterByCarParks = filterByIds.getLegacyGraphQLCarParks();
      }

      List<TransitMode> filterByModes = args.getLegacyGraphQLFilterByModes() != null
        ? args
          .getLegacyGraphQLFilterByModes()
          .stream()
          .map(mode -> {
            try {
              return TransitMode.valueOf(mode.name());
            } catch (IllegalArgumentException ignored) {
              return null;
            }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList())
        : null;
      List<PlaceType> filterByPlaceTypes = args.getLegacyGraphQLFilterByPlaceTypes() != null
        ? args
          .getLegacyGraphQLFilterByPlaceTypes()
          .stream()
          .map(LegacyGraphQLUtils::toModel)
          .toList()
        : null;

      List<PlaceAtDistance> places;
      try {
        places =
          new ArrayList<>(
            getGraphFinder(environment)
              .findClosestPlaces(
                args.getLegacyGraphQLLat(),
                args.getLegacyGraphQLLon(),
                args.getLegacyGraphQLMaxDistance(),
                args.getLegacyGraphQLMaxResults(),
                filterByModes,
                filterByPlaceTypes,
                filterByStops,
                filterByRoutes,
                filterByBikeRentalStations,
                getTransitService(environment)
              )
          );
      } catch (RoutingValidationException e) {
        places = Collections.emptyList();
      }

      return new SimpleListConnection<>(places).get(environment);
    };
  }

  @Override
  public DataFetcher<Object> node() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeNodeArgs(environment.getArguments());
      String type = args.getLegacyGraphQLId().getType();
      String id = args.getLegacyGraphQLId().getId();
      final LegacyGraphQLRequestContext context = environment.<LegacyGraphQLRequestContext>getContext();
      TransitService transitService = context.transitService();
      VehicleParkingService vehicleParkingService = context.vehicleParkingService();
      VehicleRentalService vehicleRentalStationService = context.vehicleRentalService();

      switch (type) {
        case "Agency":
          return transitService.getAgencyForId(FeedScopedId.parseId(id));
        case "Alert":
          return null; //TODO
        case "BikePark":
          var bikeParkId = FeedScopedId.parseId(id);
          return vehicleParkingService == null
            ? null
            : vehicleParkingService
              .getBikeParks()
              .filter(bikePark -> bikePark.getId().equals(bikeParkId))
              .findAny()
              .orElse(null);
        case "BikeRentalStation":
          return vehicleRentalStationService == null
            ? null
            : vehicleRentalStationService.getVehicleRentalPlace(FeedScopedId.parseId(id));
        case "VehicleRentalStation":
          return vehicleRentalStationService == null
            ? null
            : vehicleRentalStationService.getVehicleRentalStation(FeedScopedId.parseId(id));
        case "RentalVehicle":
          return vehicleRentalStationService == null
            ? null
            : vehicleRentalStationService.getVehicleRentalVehicle(FeedScopedId.parseId(id));
        case "CarPark":
          var carParkId = FeedScopedId.parseId(id);
          return vehicleParkingService == null
            ? null
            : vehicleParkingService
              .getCarParks()
              .filter(carPark -> carPark.getId().equals(carParkId))
              .findAny()
              .orElse(null);
        case "Cluster":
          return null; //TODO
        case "DepartureRow":
          return PatternAtStop.fromId(transitService, id);
        case "Pattern":
          return transitService.getTripPatternForId(FeedScopedId.parseId(id));
        case "placeAtDistance":
          {
            String[] parts = id.split(";");

            Relay.ResolvedGlobalId internalId = new Relay().fromGlobalId(parts[1]);

            Object place = node()
              .get(
                DataFetchingEnvironmentImpl
                  .newDataFetchingEnvironment(environment)
                  .source(new Object())
                  .arguments(Map.of("id", internalId))
                  .build()
              );

            return new PlaceAtDistance(place, Double.parseDouble(parts[0]));
          }
        case "Route":
          return transitService.getRouteForId(FeedScopedId.parseId(id));
        case "Stop":
          return transitService.getRegularStop(FeedScopedId.parseId(id));
        case "Stoptime":
          return null; //TODO
        case "stopAtDistance":
          {
            String[] parts = id.split(";");
            var stop = transitService.getRegularStop(FeedScopedId.parseId(parts[1]));

            // TODO: Add geometry
            return new NearbyStop(stop, Integer.parseInt(parts[0]), null, null);
          }
        case "TicketType":
          return null; //TODO
        case "Trip":
          var scopedId = FeedScopedId.parseId(id);
          return transitService.getTripForId(scopedId);
        case "VehicleParking":
          var vehicleParkingId = FeedScopedId.parseId(id);
          return vehicleParkingService == null
            ? null
            : vehicleParkingService
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
  public DataFetcher<TripPattern> pattern() {
    return environment ->
      getTransitService(environment)
        .getTripPatternForId(
          FeedScopedId.parseId(
            new LegacyGraphQLTypes.LegacyGraphQLQueryTypePatternArgs(environment.getArguments())
              .getLegacyGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> getTransitService(environment).getAllTripPatterns();
  }

  @Override
  public DataFetcher<DataFetcherResult<RoutingResponse>> plan() {
    return environment -> {
      LegacyGraphQLRequestContext context = environment.<LegacyGraphQLRequestContext>getContext();
      RouteRequest request = RouteRequestMapper.toRouteRequest(environment, context);
      RoutingResponse res = context.routingService().route(request);
      return DataFetcherResult
        .<RoutingResponse>newResult()
        .data(res)
        .localContext(Map.of("locale", request.locale()))
        .build();
    };
  }

  @Override
  public DataFetcher<VehicleRentalVehicle> rentalVehicle() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRentalVehicleArgs(
        environment.getArguments()
      );

      VehicleRentalService vehicleRentalStationService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleRentalService();

      return vehicleRentalStationService
        .getVehicleRentalVehicles()
        .stream()
        .filter(vehicleRentalVehicle ->
          vehicleRentalVehicle.getId().toString().equals(args.getLegacyGraphQLId())
        )
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalVehicle>> rentalVehicles() {
    return environment -> {
      VehicleRentalService vehicleRentalStationService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleRentalService();

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRentalVehiclesArgs(
        environment.getArguments()
      );

      if (args.getLegacyGraphQLIds() != null) {
        ArrayListMultimap<String, VehicleRentalVehicle> vehicleRentalVehicles = vehicleRentalStationService
          .getVehicleRentalVehicles()
          .stream()
          .collect(
            Multimaps.toMultimap(
              vehicle -> vehicle.getId().toString(),
              vehicle -> vehicle,
              ArrayListMultimap::create
            )
          );
        return args
          .getLegacyGraphQLIds()
          .stream()
          .flatMap(id -> vehicleRentalVehicles.get(id).stream())
          .collect(Collectors.toList());
      }

      var formFactorArgs = args.getLegacyGraphQLFormFactors();
      if (formFactorArgs != null) {
        var requiredFormFactors = formFactorArgs.stream().map(LegacyGraphQLUtils::toModel).toList();

        return vehicleRentalStationService
          .getVehicleRentalVehicles()
          .stream()
          .filter(v -> v.vehicleType != null)
          .filter(v -> requiredFormFactors.contains(v.vehicleType.formFactor))
          .toList();
      }

      return vehicleRentalStationService.getVehicleRentalVehicles();
    };
  }

  @Override
  public DataFetcher<Route> route() {
    return environment ->
      getTransitService(environment)
        .getRouteForId(
          FeedScopedId.parseId(
            new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRouteArgs(environment.getArguments())
              .getLegacyGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeRoutesArgs(
        environment.getArguments()
      );

      TransitService transitService = getTransitService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return args
          .getLegacyGraphQLIds()
          .stream()
          .map(FeedScopedId::parseId)
          .map(transitService::getRouteForId)
          .collect(Collectors.toList());
      }

      Stream<Route> routeStream = transitService.getAllRoutes().stream();

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feeds = args.getLegacyGraphQLFeeds();
        routeStream = routeStream.filter(route -> feeds.contains(route.getId().getFeedId()));
      }

      if (args.getLegacyGraphQLTransportModes() != null) {
        List<TransitMode> modes = args
          .getLegacyGraphQLTransportModes()
          .stream()
          .map(mode -> TransitMode.valueOf(mode.name()))
          .collect(Collectors.toList());
        routeStream = routeStream.filter(route -> modes.contains(route.getMode()));
      }

      if (args.getLegacyGraphQLName() != null) {
        String name = args.getLegacyGraphQLName().toLowerCase(environment.getLocale());
        routeStream =
          routeStream.filter(route ->
            LegacyGraphQLUtils.startsWith(route.getShortName(), name, environment.getLocale()) ||
            LegacyGraphQLUtils.startsWith(route.getLongName(), name, environment.getLocale())
          );
      }
      return routeStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Object> serviceTimeRange() {
    return environment -> new Object();
  }

  @Override
  public DataFetcher<Object> station() {
    return environment ->
      getTransitService(environment)
        .getStationById(
          FeedScopedId.parseId(
            new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStationArgs(environment.getArguments())
              .getLegacyGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Object>> stations() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStationsArgs(
        environment.getArguments()
      );

      TransitService transitService = getTransitService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return args
          .getLegacyGraphQLIds()
          .stream()
          .map(FeedScopedId::parseId)
          .map(transitService::getStationById)
          .collect(Collectors.toList());
      }

      Stream<Station> stationStream = transitService.getStations().stream();

      if (args.getLegacyGraphQLName() != null) {
        String name = args.getLegacyGraphQLName().toLowerCase(environment.getLocale());
        stationStream =
          stationStream.filter(station ->
            LegacyGraphQLUtils.startsWith(station.getName(), name, environment.getLocale())
          );
      }

      return stationStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment ->
      getTransitService(environment)
        .getRegularStop(
          FeedScopedId.parseId(
            new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopArgs(environment.getArguments())
              .getLegacyGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsArgs(environment.getArguments());

      TransitService transitService = getTransitService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return args
          .getLegacyGraphQLIds()
          .stream()
          .map(FeedScopedId::parseId)
          .map(transitService::getRegularStop)
          .collect(Collectors.toList());
      }

      var stopStream = transitService
        .listStopLocations()
        .stream()
        .sorted(Comparator.comparing(StopLocation::getId));

      if (args.getLegacyGraphQLName() != null) {
        String name = args.getLegacyGraphQLName().toLowerCase(environment.getLocale());
        stopStream =
          stopStream.filter(stop ->
            LegacyGraphQLUtils.startsWith(stop.getName(), name, environment.getLocale())
          );
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stopsByBbox() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByBboxArgs(
        environment.getArguments()
      );

      Envelope envelope = new Envelope(
        new Coordinate(args.getLegacyGraphQLMinLon(), args.getLegacyGraphQLMinLat()),
        new Coordinate(args.getLegacyGraphQLMaxLon(), args.getLegacyGraphQLMaxLat())
      );

      Stream<RegularStop> stopStream = getTransitService(environment)
        .findRegularStop(envelope)
        .stream()
        .filter(stop -> envelope.contains(stop.getCoordinate().asJtsCoordinate()));

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feedIds = args.getLegacyGraphQLFeeds();
        stopStream = stopStream.filter(stop -> feedIds.contains(stop.getId().getFeedId()));
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Connection<NearbyStop>> stopsByRadius() {
    return environment -> {
      // TODO implement rest of the args
      LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByRadiusArgs args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByRadiusArgs(
        environment.getArguments()
      );

      List<NearbyStop> stops;
      try {
        stops =
          getGraphFinder(environment)
            .findClosestStops(
              new Coordinate(args.getLegacyGraphQLLon(), args.getLegacyGraphQLLat()),
              args.getLegacyGraphQLRadius()
            );
      } catch (RoutingValidationException e) {
        stops = Collections.emptyList();
      }

      return new SimpleListConnection<>(stops).get(environment);
    };
  }

  @Override
  public DataFetcher<Iterable<FareRuleSet>> ticketTypes() {
    return environment -> {
      var fareService = getFareService(environment);
      Map<FareType, Collection<FareRuleSet>> fareRules = fareService instanceof GtfsFaresService
        ? ((GtfsFaresService) fareService).faresV1().getFareRulesPerType()
        : ((DefaultFareService) fareService).getFareRulesPerType();

      return fareRules
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey() == FareType.regular)
        .map(Map.Entry::getValue)
        .flatMap(Collection::stream)
        .toList();
    };
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment ->
      getTransitService(environment)
        .getTripForId(
          FeedScopedId.parseId(
            new LegacyGraphQLTypes.LegacyGraphQLQueryTypeTripArgs(environment.getArguments())
              .getLegacyGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeTripsArgs(environment.getArguments());

      Stream<Trip> tripStream = getTransitService(environment).getAllTrips().stream();

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feeds = args.getLegacyGraphQLFeeds();
        tripStream = tripStream.filter(trip -> feeds.contains(trip.getId().getFeedId()));
      }

      return tripStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleParking> vehicleParking() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleParkingArgs(
        environment.getArguments()
      );

      VehicleParkingService vehicleParkingService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleParkingService();

      var vehicleParkingId = FeedScopedId.parseId(args.getLegacyGraphQLId());
      return vehicleParkingService
        .getVehicleParkings()
        .filter(vehicleParking -> vehicleParking.getId().equals(vehicleParkingId))
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> vehicleParkings() {
    return environment -> {
      VehicleParkingService vehicleParkingService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleParkingService();

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleParkingsArgs(
        environment.getArguments()
      );

      if (args.getLegacyGraphQLIds() != null) {
        var idList = args.getLegacyGraphQLIds();

        if (!idList.isEmpty()) {
          Map<String, VehicleParking> vehicleParkingMap = vehicleParkingService
            .getVehicleParkings()
            .collect(Collectors.toMap(station -> station.getId().toString(), station -> station));

          return idList.stream().map(vehicleParkingMap::get).collect(Collectors.toList());
        }
      }

      return vehicleParkingService.getVehicleParkings().collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleRentalStation> vehicleRentalStation() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleRentalStationArgs(
        environment.getArguments()
      );

      VehicleRentalService vehicleRentalStationService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleRentalService();

      return vehicleRentalStationService
        .getVehicleRentalStations()
        .stream()
        .filter(vehicleRentalStation ->
          vehicleRentalStation.getId().toString().equals(args.getLegacyGraphQLId())
        )
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalStation>> vehicleRentalStations() {
    return environment -> {
      VehicleRentalService vehicleRentalStationService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .vehicleRentalService();

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleRentalStationsArgs(
        environment.getArguments()
      );

      if (args.getLegacyGraphQLIds() != null) {
        ArrayListMultimap<String, VehicleRentalStation> vehicleRentalStations = vehicleRentalStationService
          .getVehicleRentalStations()
          .stream()
          .collect(
            Multimaps.toMultimap(
              station -> station.getId().toString(),
              station -> station,
              ArrayListMultimap::create
            )
          );
        return args
          .getLegacyGraphQLIds()
          .stream()
          .flatMap(id -> vehicleRentalStations.get(id).stream())
          .collect(Collectors.toList());
      }

      return vehicleRentalStationService.getVehicleRentalStations();
    };
  }

  @Override
  public DataFetcher<Object> viewer() {
    return environment -> new Object();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().transitService();
  }

  private FareService getFareService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().fareService();
  }

  private GraphFinder getGraphFinder(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().graphFinder();
  }

  protected static List<TransitAlert> filterAlerts(
    Collection<TransitAlert> alerts,
    LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs args
  ) {
    var severities = args.getLegacyGraphQLSeverityLevel();
    var effects = args.getLegacyGraphQLEffect();
    var causes = args.getLegacyGraphQLCause();
    return alerts
      .stream()
      .filter(alert ->
        args.getLegacyGraphQLFeeds() == null ||
        args.getLegacyGraphQLFeeds().contains(alert.getId().getFeedId())
      )
      .filter(alert ->
        severities == null || severities.contains(getLegacyGraphQLSeverity(alert.severity()))
      )
      .filter(alert -> effects == null || effects.contains(getLegacyGraphQLEffect(alert.effect())))
      .filter(alert -> causes == null || causes.contains(getLegacyGraphQLCause(alert.cause())))
      .filter(alert ->
        args.getLegacyGraphQLRoute() == null ||
        alert
          .entities()
          .stream()
          .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
          .map(EntitySelector.Route.class::cast)
          .anyMatch(route -> args.getLegacyGraphQLRoute().contains(route.routeId().toString()))
      )
      .filter(alert ->
        args.getLegacyGraphQLStop() == null ||
        alert
          .entities()
          .stream()
          .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
          .map(EntitySelector.Stop.class::cast)
          .anyMatch(stop -> args.getLegacyGraphQLStop().contains(stop.stopId().toString()))
      )
      .collect(Collectors.toList());
  }
}
