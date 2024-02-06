package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.mapping.AlertCauseMapper.getGraphQLCause;
import static org.opentripplanner.apis.gtfs.mapping.AlertEffectMapper.getGraphQLEffect;
import static org.opentripplanner.apis.gtfs.mapping.SeverityMapper.getGraphQLSeverity;

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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLQueryTypeStopsByRadiusArgs;
import org.opentripplanner.apis.gtfs.mapping.RouteRequestMapper;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.ext.fares.impl.GtfsFaresService;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.application.OTPFeature;
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

public class QueryTypeImpl implements GraphQLDataFetchers.GraphQLQueryType {

  // TODO: figure out a runtime solution
  private static final DirectionMapper DIRECTION_MAPPER = new DirectionMapper(
    DataImportIssueStore.NOOP
  );

  private final List<PlaceType> DEFAULT_PLACE_TYPES = List.copyOf(
    EnumSet.complementOf(EnumSet.of(PlaceType.STATION))
  );

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> getTransitService(environment).getAgencies();
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> {
      FeedScopedId id = FeedScopedId.parse(
        new GraphQLTypes.GraphQLQueryTypeAgencyArgs(environment.getArguments()).getGraphQLId()
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
      var args = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(environment.getArguments());
      return filterAlerts(alerts, args);
    };
  }

  @Override
  public DataFetcher<VehicleParking> bikePark() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeBikeParkArgs(environment.getArguments());

      VehicleParkingService vehicleParkingService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleParkingService();

      return vehicleParkingService
        .getBikeParks()
        .filter(bikePark -> bikePark.getId().getId().equals(args.getGraphQLId()))
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> bikeParks() {
    return environment -> {
      VehicleParkingService vehicleParkingService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleParkingService();

      return vehicleParkingService.getBikeParks().toList();
    };
  }

  @Override
  public DataFetcher<VehicleRentalPlace> bikeRentalStation() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeBikeRentalStationArgs(environment.getArguments());

      VehicleRentalService vehicleRentalStationService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleRentalService();

      return vehicleRentalStationService
        .getVehicleRentalPlaces()
        .stream()
        .filter(vehicleRentalStation ->
          vehicleRentalStation.getStationId().equals(args.getGraphQLId())
        )
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalPlace>> bikeRentalStations() {
    return environment -> {
      VehicleRentalService vehicleRentalStationService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleRentalService();

      var args = new GraphQLTypes.GraphQLQueryTypeBikeRentalStationsArgs(
        environment.getArguments()
      );

      if (args.getGraphQLIds() != null) {
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
          .getGraphQLIds()
          .stream()
          .flatMap(id -> vehicleRentalStations.get(id).stream())
          .toList();
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
      var args = new GraphQLTypes.GraphQLQueryTypeCarParkArgs(environment.getArguments());

      VehicleParkingService vehicleParkingService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleParkingService();

      return vehicleParkingService
        .getCarParks()
        .filter(carPark -> carPark.getId().getId().equals(args.getGraphQLId()))
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleParking>> carParks() {
    return environment -> {
      VehicleParkingService vehicleParkingService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleParkingService();

      var args = new GraphQLTypes.GraphQLQueryTypeCarParksArgs(environment.getArguments());

      if (args.getGraphQLIds() != null) {
        var idList = args.getGraphQLIds();

        if (!idList.isEmpty()) {
          Map<String, VehicleParking> carParkMap = vehicleParkingService
            .getCarParks()
            .collect(Collectors.toMap(station -> station.getId().getId(), station -> station));

          return idList.stream().map(carParkMap::get).toList();
        }
      }

      return vehicleParkingService.getCarParks().toList();
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
        new GraphQLTypes.GraphQLQueryTypeDepartureRowArgs(environment.getArguments()).getGraphQLId()
      );
  }

  @Override
  public DataFetcher<Iterable<String>> feeds() {
    return environment -> getTransitService(environment).getFeedIds();
  }

  @Override
  public DataFetcher<Trip> fuzzyTrip() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeFuzzyTripArgs(environment.getArguments());

      TransitService transitService = getTransitService(environment);

      return new GtfsRealtimeFuzzyTripMatcher(transitService)
        .getTrip(
          transitService.getRouteForId(FeedScopedId.parse(args.getGraphQLRoute())),
          DIRECTION_MAPPER.map(args.getGraphQLDirection()),
          args.getGraphQLTime(),
          ServiceDateUtils.parseString(args.getGraphQLDate())
        );
    };
  }

  @Override
  public DataFetcher<Connection<PlaceAtDistance>> nearest() {
    return environment -> {
      List<FeedScopedId> filterByStops = null;
      List<FeedScopedId> filterByStations = null;
      List<FeedScopedId> filterByRoutes = null;
      List<String> filterByBikeRentalStations = null;
      // TODO implement
      List<String> filterByBikeParks = null;
      List<String> filterByCarParks = null;

      GraphQLTypes.GraphQLQueryTypeNearestArgs args = new GraphQLTypes.GraphQLQueryTypeNearestArgs(
        environment.getArguments()
      );

      GraphQLTypes.GraphQLInputFiltersInput filterByIds = args.getGraphQLFilterByIds();

      if (filterByIds != null) {
        filterByStops =
          filterByIds.getGraphQLStops() != null
            ? filterByIds.getGraphQLStops().stream().map(FeedScopedId::parse).toList()
            : null;
        filterByStations =
          filterByIds.getGraphQLStations() != null
            ? filterByIds.getGraphQLStations().stream().map(FeedScopedId::parse).toList()
            : null;
        filterByRoutes =
          filterByIds.getGraphQLRoutes() != null
            ? filterByIds.getGraphQLRoutes().stream().map(FeedScopedId::parse).toList()
            : null;
        filterByBikeRentalStations = filterByIds.getGraphQLBikeRentalStations();
        filterByBikeParks = filterByIds.getGraphQLBikeParks();
        filterByCarParks = filterByIds.getGraphQLCarParks();
      }

      List<TransitMode> filterByModes = args.getGraphQLFilterByModes() != null
        ? args
          .getGraphQLFilterByModes()
          .stream()
          .map(mode -> {
            try {
              return TransitMode.valueOf(mode.name());
            } catch (IllegalArgumentException ignored) {
              return null;
            }
          })
          .filter(Objects::nonNull)
          .toList()
        : null;
      List<PlaceType> filterByPlaceTypes = args.getGraphQLFilterByPlaceTypes() != null
        ? args.getGraphQLFilterByPlaceTypes().stream().map(GraphQLUtils::toModel).toList()
        : DEFAULT_PLACE_TYPES;

      List<PlaceAtDistance> places;
      try {
        places =
          new ArrayList<>(
            getGraphFinder(environment)
              .findClosestPlaces(
                args.getGraphQLLat(),
                args.getGraphQLLon(),
                args.getGraphQLMaxDistance(),
                args.getGraphQLMaxResults(),
                filterByModes,
                filterByPlaceTypes,
                filterByStops,
                filterByStations,
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
      var args = new GraphQLTypes.GraphQLQueryTypeNodeArgs(environment.getArguments());
      String type = args.getGraphQLId().getType();
      String id = args.getGraphQLId().getId();
      final GraphQLRequestContext context = environment.getContext();
      TransitService transitService = context.transitService();
      VehicleParkingService vehicleParkingService = context.vehicleParkingService();
      VehicleRentalService vehicleRentalStationService = context.vehicleRentalService();

      switch (type) {
        case "Agency":
          return transitService.getAgencyForId(FeedScopedId.parse(id));
        case "Alert":
          return null; //TODO
        case "BikePark":
          var bikeParkId = FeedScopedId.parse(id);
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
            : vehicleRentalStationService.getVehicleRentalPlace(FeedScopedId.parse(id));
        case "VehicleRentalStation":
          return vehicleRentalStationService == null
            ? null
            : vehicleRentalStationService.getVehicleRentalStation(FeedScopedId.parse(id));
        case "RentalVehicle":
          return vehicleRentalStationService == null
            ? null
            : vehicleRentalStationService.getVehicleRentalVehicle(FeedScopedId.parse(id));
        case "CarPark":
          var carParkId = FeedScopedId.parse(id);
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
          return transitService.getTripPatternForId(FeedScopedId.parse(id));
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
          return transitService.getRouteForId(FeedScopedId.parse(id));
        case "Stop":
          return transitService.getRegularStop(FeedScopedId.parse(id));
        case "Stoptime":
          return null; //TODO
        case "stopAtDistance":
          {
            String[] parts = id.split(";");
            var stop = transitService.getRegularStop(FeedScopedId.parse(parts[1]));

            // TODO: Add geometry
            return new NearbyStop(stop, Integer.parseInt(parts[0]), null, null);
          }
        case "TicketType":
          return null; //TODO
        case "Trip":
          var scopedId = FeedScopedId.parse(id);
          return transitService.getTripForId(scopedId);
        case "VehicleParking":
          var vehicleParkingId = FeedScopedId.parse(id);
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
          FeedScopedId.parse(
            new GraphQLTypes.GraphQLQueryTypePatternArgs(environment.getArguments()).getGraphQLId()
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
      GraphQLRequestContext context = environment.<GraphQLRequestContext>getContext();
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
      var args = new GraphQLTypes.GraphQLQueryTypeRentalVehicleArgs(environment.getArguments());

      VehicleRentalService vehicleRentalStationService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleRentalService();

      return vehicleRentalStationService
        .getVehicleRentalVehicles()
        .stream()
        .filter(vehicleRentalVehicle ->
          vehicleRentalVehicle.getId().toString().equals(args.getGraphQLId())
        )
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalVehicle>> rentalVehicles() {
    return environment -> {
      VehicleRentalService vehicleRentalStationService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleRentalService();

      var args = new GraphQLTypes.GraphQLQueryTypeRentalVehiclesArgs(environment.getArguments());

      if (args.getGraphQLIds() != null) {
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
          .getGraphQLIds()
          .stream()
          .flatMap(id -> vehicleRentalVehicles.get(id).stream())
          .toList();
      }

      var formFactorArgs = args.getGraphQLFormFactors();
      if (formFactorArgs != null) {
        var requiredFormFactors = formFactorArgs.stream().map(GraphQLUtils::toModel).toList();

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
          FeedScopedId.parse(
            new GraphQLTypes.GraphQLQueryTypeRouteArgs(environment.getArguments()).getGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeRoutesArgs(environment.getArguments());

      TransitService transitService = getTransitService(environment);

      if (args.getGraphQLIds() != null) {
        return args
          .getGraphQLIds()
          .stream()
          .map(FeedScopedId::parse)
          .map(transitService::getRouteForId)
          .toList();
      }

      Stream<Route> routeStream = transitService.getAllRoutes().stream();

      if (args.getGraphQLFeeds() != null) {
        List<String> feeds = args.getGraphQLFeeds();
        routeStream = routeStream.filter(route -> feeds.contains(route.getId().getFeedId()));
      }

      if (args.getGraphQLTransportModes() != null) {
        List<TransitMode> modes = args
          .getGraphQLTransportModes()
          .stream()
          .map(mode -> TransitMode.valueOf(mode.name()))
          .toList();
        routeStream = routeStream.filter(route -> modes.contains(route.getMode()));
      }

      if (args.getGraphQLName() != null) {
        String name = args.getGraphQLName().toLowerCase(environment.getLocale());
        routeStream =
          routeStream.filter(route ->
            GraphQLUtils.startsWith(route.getShortName(), name, environment.getLocale()) ||
            GraphQLUtils.startsWith(route.getLongName(), name, environment.getLocale())
          );
      }
      return routeStream.toList();
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
          FeedScopedId.parse(
            new GraphQLTypes.GraphQLQueryTypeStationArgs(environment.getArguments()).getGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Object>> stations() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeStationsArgs(environment.getArguments());

      TransitService transitService = getTransitService(environment);

      if (args.getGraphQLIds() != null) {
        return args
          .getGraphQLIds()
          .stream()
          .map(FeedScopedId::parse)
          .map(transitService::getStationById)
          .collect(Collectors.toList());
      }

      Stream<Station> stationStream = transitService.getStations().stream();

      if (args.getGraphQLName() != null) {
        String name = args.getGraphQLName().toLowerCase(environment.getLocale());
        stationStream =
          stationStream.filter(station ->
            GraphQLUtils.startsWith(station.getName(), name, environment.getLocale())
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
          FeedScopedId.parse(
            new GraphQLTypes.GraphQLQueryTypeStopArgs(environment.getArguments()).getGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeStopsArgs(environment.getArguments());

      TransitService transitService = getTransitService(environment);

      if (args.getGraphQLIds() != null) {
        return args
          .getGraphQLIds()
          .stream()
          .map(FeedScopedId::parse)
          .map(transitService::getRegularStop)
          .collect(Collectors.toList());
      }

      var stopStream = transitService
        .listStopLocations()
        .stream()
        .sorted(Comparator.comparing(StopLocation::getId));

      if (args.getGraphQLName() != null) {
        String name = args.getGraphQLName().toLowerCase(environment.getLocale());
        stopStream =
          stopStream.filter(stop ->
            GraphQLUtils.startsWith(stop.getName(), name, environment.getLocale())
          );
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stopsByBbox() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeStopsByBboxArgs(environment.getArguments());

      Envelope envelope = new Envelope(
        new Coordinate(args.getGraphQLMinLon(), args.getGraphQLMinLat()),
        new Coordinate(args.getGraphQLMaxLon(), args.getGraphQLMaxLat())
      );

      Stream<RegularStop> stopStream = getTransitService(environment)
        .findRegularStops(envelope)
        .stream()
        .filter(stop -> envelope.contains(stop.getCoordinate().asJtsCoordinate()));

      if (args.getGraphQLFeeds() != null) {
        List<String> feedIds = args.getGraphQLFeeds();
        stopStream = stopStream.filter(stop -> feedIds.contains(stop.getId().getFeedId()));
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Connection<NearbyStop>> stopsByRadius() {
    return environment -> {
      // TODO implement rest of the args
      GraphQLQueryTypeStopsByRadiusArgs args = new GraphQLQueryTypeStopsByRadiusArgs(
        environment.getArguments()
      );

      List<NearbyStop> stops;
      try {
        stops =
          getGraphFinder(environment)
            .findClosestStops(
              new Coordinate(args.getGraphQLLon(), args.getGraphQLLat()),
              args.getGraphQLRadius()
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
          FeedScopedId.parse(
            new GraphQLTypes.GraphQLQueryTypeTripArgs(environment.getArguments()).getGraphQLId()
          )
        );
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeTripsArgs(environment.getArguments());

      Stream<Trip> tripStream = getTransitService(environment).getAllTrips().stream();

      if (args.getGraphQLFeeds() != null) {
        List<String> feeds = args.getGraphQLFeeds();
        tripStream = tripStream.filter(trip -> feeds.contains(trip.getId().getFeedId()));
      }

      return tripStream.toList();
    };
  }

  @Override
  public DataFetcher<VehicleParking> vehicleParking() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeVehicleParkingArgs(environment.getArguments());

      VehicleParkingService vehicleParkingService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleParkingService();

      var vehicleParkingId = FeedScopedId.parse(args.getGraphQLId());
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
        .<GraphQLRequestContext>getContext()
        .vehicleParkingService();

      var args = new GraphQLTypes.GraphQLQueryTypeVehicleParkingsArgs(environment.getArguments());

      if (args.getGraphQLIds() != null) {
        var idList = args.getGraphQLIds();

        if (!idList.isEmpty()) {
          Map<String, VehicleParking> vehicleParkingMap = vehicleParkingService
            .getVehicleParkings()
            .collect(Collectors.toMap(station -> station.getId().toString(), station -> station));

          return idList.stream().map(vehicleParkingMap::get).toList();
        }
      }

      return vehicleParkingService.getVehicleParkings().toList();
    };
  }

  @Override
  public DataFetcher<VehicleRentalStation> vehicleRentalStation() {
    return environment -> {
      var args = new GraphQLTypes.GraphQLQueryTypeVehicleRentalStationArgs(
        environment.getArguments()
      );

      VehicleRentalService vehicleRentalStationService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleRentalService();

      var id = args.getGraphQLId();

      // TODO the fuzzy matching can be potentially removed after a while.
      return vehicleRentalStationService
        .getVehicleRentalStations()
        .stream()
        .filter(vehicleRentalStation ->
          OTPFeature.GtfsGraphQlApiRentalStationFuzzyMatching.isOn()
            ? stationIdFuzzyMatches(vehicleRentalStation, id)
            : stationIdMatches(vehicleRentalStation, id)
        )
        .findAny()
        .orElse(null);
    };
  }

  @Override
  public DataFetcher<Iterable<VehicleRentalStation>> vehicleRentalStations() {
    return environment -> {
      VehicleRentalService vehicleRentalStationService = environment
        .<GraphQLRequestContext>getContext()
        .vehicleRentalService();

      var args = new GraphQLTypes.GraphQLQueryTypeVehicleRentalStationsArgs(
        environment.getArguments()
      );

      if (args.getGraphQLIds() != null) {
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
          .getGraphQLIds()
          .stream()
          .flatMap(id -> vehicleRentalStations.get(id).stream())
          .toList();
      }

      return vehicleRentalStationService.getVehicleRentalStations();
    };
  }

  @Override
  public DataFetcher<Object> viewer() {
    return environment -> new Object();
  }

  /**
   * This matches station's feedScopedId to the given string.
   */
  private boolean stationIdMatches(VehicleRentalStation station, String feedScopedId) {
    return station.getId().toString().equals(feedScopedId);
  }

  /**
   * This matches station's feedScopedId to the given string if the string is feed scoped (i.e
   * contains a `:` separator) or only matches the station's id without the feed to the given
   * string. This approach can lead to a random station matching the criteria if there are multiple
   * stations with the same id in different feeds.
   * <p>
   * TODO this can be potentially removed after a while, only used by Digitransit as of now.
   */
  private boolean stationIdFuzzyMatches(VehicleRentalStation station, String idWithoutFeed) {
    if (idWithoutFeed != null && idWithoutFeed.contains(":")) {
      return stationIdMatches(station, idWithoutFeed);
    }
    return station.getId().getId().equals(idWithoutFeed);
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private FareService getFareService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().fareService();
  }

  private GraphFinder getGraphFinder(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().graphFinder();
  }

  protected static List<TransitAlert> filterAlerts(
    Collection<TransitAlert> alerts,
    GraphQLTypes.GraphQLQueryTypeAlertsArgs args
  ) {
    var severities = args.getGraphQLSeverityLevel();
    var effects = args.getGraphQLEffect();
    var causes = args.getGraphQLCause();
    return alerts
      .stream()
      .filter(alert ->
        args.getGraphQLFeeds() == null || args.getGraphQLFeeds().contains(alert.getId().getFeedId())
      )
      .filter(alert ->
        severities == null || severities.contains(getGraphQLSeverity(alert.severity()))
      )
      .filter(alert -> effects == null || effects.contains(getGraphQLEffect(alert.effect())))
      .filter(alert -> causes == null || causes.contains(getGraphQLCause(alert.cause())))
      .filter(alert ->
        args.getGraphQLRoute() == null ||
        alert
          .entities()
          .stream()
          .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
          .map(EntitySelector.Route.class::cast)
          .anyMatch(route -> args.getGraphQLRoute().contains(route.routeId().toString()))
      )
      .filter(alert ->
        args.getGraphQLStop() == null ||
        alert
          .entities()
          .stream()
          .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
          .map(EntitySelector.Stop.class::cast)
          .anyMatch(stop -> args.getGraphQLStop().contains(stop.stopId().toString()))
      )
      .toList();
  }
}
