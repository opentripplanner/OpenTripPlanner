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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLUtils;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.DirectionMapper;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.RequestFunctions;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;

public class LegacyGraphQLQueryTypeImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLQueryType {

  // TODO: figure out a runtime solution
  private static final DirectionMapper DIRECTION_MAPPER = new DirectionMapper(
    DataImportIssueStore.NOOP
  );

  public static <T> boolean hasArgument(Map<String, T> m, String name) {
    return m.containsKey(name) && m.get(name) != null;
  }

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
      List<String> severities = args.getLegacyGraphQLSeverityLevel() == null
        ? null
        : (
          (List<LegacyGraphQLTypes.LegacyGraphQLAlertSeverityLevelType>) args.getLegacyGraphQLSeverityLevel()
        ).stream()
          .map(Enum::name)
          .toList();
      List<String> effects = args.getLegacyGraphQLEffect() == null
        ? null
        : (
          (List<LegacyGraphQLTypes.LegacyGraphQLAlertEffectType>) args.getLegacyGraphQLEffect()
        ).stream()
          .map(Enum::name)
          .toList();
      List<String> causes = args.getLegacyGraphQLCause() == null
        ? null
        : (
          (List<LegacyGraphQLTypes.LegacyGraphQLAlertCauseType>) args.getLegacyGraphQLCause()
        ).stream()
          .map(Enum::name)
          .toList();
      return alerts
        .stream()
        .filter(alert ->
          args.getLegacyGraphQLFeeds() == null ||
          ((List<String>) args.getLegacyGraphQLFeeds()).contains(alert.getFeedId())
        )
        .filter(alert ->
          severities == null || severities.contains(getLegacyGraphQLSeverity(alert.severity))
        )
        .filter(alert -> effects == null || effects.contains(getLegacyGraphQLEffect(alert.effect)))
        .filter(alert -> causes == null || causes.contains(getLegacyGraphQLCause(alert.cause)))
        .filter(alert ->
          args.getLegacyGraphQLRoute() == null ||
          alert
            .getEntities()
            .stream()
            .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
            .map(EntitySelector.Route.class::cast)
            .anyMatch(route ->
              ((List<String>) args.getLegacyGraphQLRoute()).contains(route.routeId().toString())
            )
        )
        .filter(alert ->
          args.getLegacyGraphQLStop() == null ||
          alert
            .getEntities()
            .stream()
            .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
            .map(EntitySelector.Stop.class::cast)
            .anyMatch(stop ->
              ((List<String>) args.getLegacyGraphQLStop()).contains(stop.stopId().toString())
            )
        )
        .collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleParking> bikePark() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeParkArgs(
        environment.getArguments()
      );

      VehicleParkingService vehicleParkingService = getRoutingService(environment)
        .getVehicleParkingService();

      if (vehicleParkingService == null) {
        return null;
      }

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
      VehicleParkingService vehicleParkingService = getRoutingService(environment)
        .getVehicleParkingService();

      if (vehicleParkingService == null) {
        return null;
      }

      return vehicleParkingService.getBikeParks().collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<VehicleRentalPlace> bikeRentalStation() {
    return environment -> {
      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeBikeRentalStationArgs(
        environment.getArguments()
      );

      VehicleRentalService vehicleRentalStationService = getRoutingService(environment)
        .getVehicleRentalService();

      if (vehicleRentalStationService == null) {
        return null;
      }

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
      VehicleRentalService vehicleRentalStationService = getRoutingService(environment)
        .getVehicleRentalService();

      if (vehicleRentalStationService == null) {
        return null;
      }

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
        return ((List<String>) args.getLegacyGraphQLIds()).stream()
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

      VehicleParkingService vehicleParkingService = getRoutingService(environment)
        .getVehicleParkingService();

      if (vehicleParkingService == null) {
        return null;
      }

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
      VehicleParkingService vehicleParkingService = getRoutingService(environment)
        .getVehicleParkingService();

      if (vehicleParkingService == null) {
        return null;
      }

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeCarParksArgs(
        environment.getArguments()
      );

      if (args.getLegacyGraphQLIds() != null) {
        var idList = ((List<String>) args.getLegacyGraphQLIds());

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
      List<String> filterByBikeParks = null;
      List<String> filterByCarParks = null;

      LegacyGraphQLTypes.LegacyGraphQLQueryTypeNearestArgs args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeNearestArgs(
        environment.getArguments()
      );

      LegacyGraphQLTypes.LegacyGraphQLInputFiltersInput filterByIds = args.getLegacyGraphQLFilterByIds();

      if (filterByIds != null) {
        filterByStops =
          filterByIds.getLegacyGraphQLStops() != null
            ? StreamSupport
              .stream(filterByIds.getLegacyGraphQLStops().spliterator(), false)
              .map(FeedScopedId::parseId)
              .collect(Collectors.toList())
            : null;
        filterByRoutes =
          filterByIds.getLegacyGraphQLRoutes() != null
            ? StreamSupport
              .stream(filterByIds.getLegacyGraphQLRoutes().spliterator(), false)
              .map(FeedScopedId::parseId)
              .collect(Collectors.toList())
            : null;
        filterByBikeRentalStations =
          filterByIds.getLegacyGraphQLBikeRentalStations() != null
            ? Lists.newArrayList(filterByIds.getLegacyGraphQLBikeRentalStations())
            : null;
        filterByBikeParks =
          filterByIds.getLegacyGraphQLBikeParks() != null
            ? Lists.newArrayList(filterByIds.getLegacyGraphQLBikeParks())
            : null;
        filterByCarParks =
          filterByIds.getLegacyGraphQLCarParks() != null
            ? Lists.newArrayList(filterByIds.getLegacyGraphQLCarParks())
            : null;
      }

      List<TransitMode> filterByModes = args.getLegacyGraphQLFilterByModes() != null
        ? StreamSupport
          .stream(args.getLegacyGraphQLFilterByModes().spliterator(), false)
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
        ? StreamSupport
          .stream(args.getLegacyGraphQLFilterByPlaceTypes().spliterator(), false)
          .map(LegacyGraphQLUtils::toModel)
          .toList()
        : null;

      List<PlaceAtDistance> places;
      try {
        places =
          new ArrayList<>(
            getRoutingService(environment)
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
                filterByBikeParks,
                filterByCarParks,
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
      RoutingService routingService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .getRoutingService();
      TransitService transitService = environment
        .<LegacyGraphQLRequestContext>getContext()
        .getTransitService();
      VehicleParkingService vehicleParkingService = routingService.getVehicleParkingService();
      VehicleRentalService vehicleRentalStationService = routingService.getVehicleRentalService();

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
      // we need to clone the default request as it is request-scoped and this method
      // can be used by a batch query, causing several invocations to use the same instance
      RouteRequest request = context.getServerContext().defaultRouteRequest().clone();

      CallerWithEnvironment callWith = new CallerWithEnvironment(environment);

      callWith.argument(
        "fromPlace",
        (String from) -> request.setFrom(LocationStringParser.fromOldStyleString(from))
      );
      callWith.argument(
        "toPlace",
        (String to) -> request.setTo(LocationStringParser.fromOldStyleString(to))
      );

      callWith.argument("from", (Map<String, Object> v) -> request.setFrom(toGenericLocation(v)));
      callWith.argument("to", (Map<String, Object> v) -> request.setTo(toGenericLocation(v)));

      request.setDateTime(
        environment.getArgument("date"),
        environment.getArgument("time"),
        context.getServerContext().transitService().getTimeZone()
      );

      // TODO VIA: double check that
      callWith.argument("wheelchair", request::setWheelchair);
      callWith.argument("numItineraries", request::setNumItineraries);
      callWith.argument("searchWindow", (Long m) -> request.setSearchWindow(Duration.ofSeconds(m)));
      callWith.argument("pageCursor", request::setPageCursorFromEncoded);
      // callWith.argument("maxSlope", request::setMaxSlope);
      // callWith.argument("carParkCarLegWeight", request::setCarParkCarLegWeight);
      // callWith.argument("itineraryFiltering", request::setItineraryFiltering);

      request.withPreferences(preferences -> {
        preferences.withBike(bike -> {
          callWith.argument("bikeReluctance", bike::withReluctance);
          callWith.argument("bikeWalkingReluctance", bike::withWalkingReluctance);
          callWith.argument("bikeWalkingSpeed", bike::withWalkingSpeed);
          callWith.argument("bikeSpeed", bike::withSpeed);
          callWith.argument("bikeSwitchTime", bike::withSwitchTime);
          callWith.argument("bikeSwitchCost", bike::withSwitchCost);
          callWith.argument("bikeBoardCost", bike::withBoardCost);

          if (environment.getArgument("optimize") != null) {
            bike.withOptimizeType(BicycleOptimizeType.valueOf(environment.getArgument("optimize")));
          }
          if (bike.optimizeType() == BicycleOptimizeType.TRIANGLE) {
            bike.withOptimizeTriangle(triangle -> {
              callWith.argument("triangle.timeFactor", triangle::withTime);
              callWith.argument("triangle.slopeFactor", triangle::withSlope);
              callWith.argument("triangle.safetyFactor", triangle::withSafety);
            });
          }
        });

        preferences.withCar(car -> callWith.argument("carReluctance", car::withReluctance));

        preferences.withWalk(b -> {
          callWith.argument("walkReluctance", b::withReluctance);
          callWith.argument("walkSpeed", b::withSpeed);
          callWith.argument("walkBoardCost", b::withBoardCost);
          callWith.argument("walkSafetyFactor", b::withSafetyFactor);
        });
        preferences.withRental(rental -> {
          callWith.argument(
            "keepingRentedBicycleAtDestinationCost",
            rental::withArrivingInRentalVehicleAtDestinationCost
          );
          rental.withUseAvailabilityInformation(request.isTripPlannedForNow());
        });
        callWith.argument(
          "debugItineraryFilter",
          (Boolean v) -> preferences.withItineraryFilter(it -> it.withDebug(v))
        );
        preferences.withTransit(tr -> {
          callWith.argument("boardSlack", tr::withDefaultBoardSlackSec);
          callWith.argument("alightSlack", tr::withDefaultAlightSlackSec);
          callWith.argument(
            "preferred.otherThanPreferredRoutesPenalty",
            tr::setOtherThanPreferredRoutesPenalty
          );
          // This is deprecated, if both are set, the proper one will override this
          callWith.argument(
            "unpreferred.useUnpreferredRoutesPenalty",
            (Integer v) ->
              tr.setUnpreferredCostString(
                RequestFunctions.serialize(RequestFunctions.createLinearFunction(v, 0.0))
              )
          );
          callWith.argument("unpreferred.unpreferredCost", tr::setUnpreferredCostString);
          callWith.argument("ignoreRealtimeUpdates", tr::setIgnoreRealtimeUpdates);
          callWith.argument(
            "modeWeight",
            (Map<String, Object> modeWeights) ->
              tr.setReluctanceForMode(
                modeWeights
                  .entrySet()
                  .stream()
                  .collect(
                    Collectors.toMap(
                      e -> TransitMode.valueOf(e.getKey()),
                      e -> (Double) e.getValue()
                    )
                  )
              )
          );
        });
        preferences.withTransfer(tx -> {
          callWith.argument("transferPenalty", tx::withCost);
          callWith.argument("minTransferTime", tx::withSlack);
          callWith.argument("waitReluctance", tx::withWaitReluctance);
          callWith.argument("maxTransfers", tx::withMaxTransfers);
          callWith.argument("nonpreferredTransferPenalty", tx::withNonpreferredCost);
        });
      });

      callWith.argument(
        "allowKeepingRentedBicycleAtDestination",
        request.journey().rental()::setAllowArrivingInRentedVehicleAtDestination
      );
      callWith.argument("arriveBy", request::setArriveBy);
      // TODO VIA (HSL): 2022-08-24 I'm just commenting this out since we have to refactor it anyway
      //      callWith.argument(
      //        "intermediatePlaces",
      //        (List<Map<String, Object>> v) ->
      //          request.intermediatePlaces =
      //            v
      //              .stream()
      //              .map(LegacyGraphQLQueryTypeImpl::toGenericLocation)
      //              .collect(Collectors.toList())
      //      );

      callWith.argument(
        "preferred.routes",
        request.journey().transit()::setPreferredRoutesFromString
      );

      callWith.argument(
        "preferred.agencies",
        request.journey().transit()::setPreferredAgenciesFromString
      );
      callWith.argument(
        "unpreferred.routes",
        request.journey().transit()::setUnpreferredRoutesFromString
      );
      callWith.argument(
        "unpreferred.agencies",
        request.journey().transit()::setUnpreferredAgenciesFromString
      );
      callWith.argument("banned.routes", request.journey().transit()::setBannedRoutesFromString);
      callWith.argument("banned.agencies", request.journey().transit()::setBannedAgenciesFromSting);
      callWith.argument("banned.trips", request.journey().transit()::setBannedTripsFromString);
      // callWith.argument("banned.stops", request::setBannedStops);
      // callWith.argument("banned.stopsHard", request::setBannedStopsHard);
      // callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
      // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> request.compactLegsByReversedSearch = v);

      if (hasArgument(environment, "transportModes")) {
        QualifiedModeSet modes = new QualifiedModeSet("WALK");

        modes.qModes =
          environment
            .<List<Map<String, String>>>getArgument("transportModes")
            .stream()
            .map(transportMode ->
              new QualifiedMode(
                transportMode.get("mode") +
                (transportMode.get("qualifier") == null ? "" : "_" + transportMode.get("qualifier"))
              )
            )
            .collect(Collectors.toSet());

        request.journey().setModes(modes.getRequestModes());
      }

      if (hasArgument(environment, "allowedTicketTypes")) {
        // request.allowedFares = new HashSet();
        // ((List<String>)environment.getArgument("allowedTicketTypes")).forEach(ticketType -> request.allowedFares.add(ticketType.replaceFirst("_", ":")));
      }

      var vehicleRental = request.journey().rental();

      // Deprecated, the next one will override this, if both are set
      callWith.argument(
        "allowedBikeRentalNetworks",
        (Collection<String> v) -> vehicleRental.setAllowedNetworks(new HashSet<>(v))
      );
      callWith.argument(
        "allowedVehicleRentalNetworks",
        (Collection<String> v) -> vehicleRental.setAllowedNetworks(new HashSet<>(v))
      );
      callWith.argument(
        "bannedVehicleRentalNetworks",
        (Collection<String> v) -> vehicleRental.setBannedNetworks(new HashSet<>(v))
      );

      callWith.argument(
        "locale",
        (String v) -> request.setLocale(LegacyGraphQLUtils.getLocale(environment, v))
      );
      RoutingResponse res = context.getRoutingService().route(request);
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

      VehicleRentalService vehicleRentalStationService = getRoutingService(environment)
        .getVehicleRentalService();

      if (vehicleRentalStationService == null) {
        return null;
      }

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
      VehicleRentalService vehicleRentalStationService = getRoutingService(environment)
        .getVehicleRentalService();

      if (vehicleRentalStationService == null) {
        return null;
      }

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
        return ((List<String>) args.getLegacyGraphQLIds()).stream()
          .flatMap(id -> vehicleRentalVehicles.get(id).stream())
          .collect(Collectors.toList());
      }

      var formFactorArgs = args.getLegacyGraphQLFormFactors();
      if (formFactorArgs != null) {
        var requiredFormFactors = StreamSupport
          .stream(formFactorArgs.spliterator(), false)
          .map(LegacyGraphQLUtils::toModel)
          .toList();

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

      RoutingService routingService = getRoutingService(environment);
      TransitService transitService = getTransitService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return StreamSupport
          .stream(args.getLegacyGraphQLIds().spliterator(), false)
          .map(FeedScopedId::parseId)
          .map(transitService::getRouteForId)
          .collect(Collectors.toList());
      }

      Stream<Route> routeStream = transitService.getAllRoutes().stream();

      if (args.getLegacyGraphQLFeeds() != null) {
        List<String> feeds = StreamSupport
          .stream(args.getLegacyGraphQLFeeds().spliterator(), false)
          .collect(Collectors.toList());
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

      RoutingService routingService = getRoutingService(environment);
      TransitService transitService = getTransitService(environment);

      if (args.getLegacyGraphQLIds() != null) {
        return StreamSupport
          .stream(args.getLegacyGraphQLIds().spliterator(), false)
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
        return StreamSupport
          .stream(args.getLegacyGraphQLIds().spliterator(), false)
          .map(FeedScopedId::parseId)
          .map(transitService::getRegularStop)
          .collect(Collectors.toList());
      }

      var stopStream = transitService.listStopLocations().stream();

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
        List<String> feedIds = Lists.newArrayList(args.getLegacyGraphQLFeeds());
        stopStream = stopStream.filter(stop -> feedIds.contains(stop.getId().getFeedId()));
      }

      return stopStream.collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Connection<NearbyStop>> stopsByRadius() {
    return environment -> {
      LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByRadiusArgs args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeStopsByRadiusArgs(
        environment.getArguments()
      );

      List<NearbyStop> stops;
      try {
        stops =
          getRoutingService(environment)
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
      Map<FareType, Collection<FareRuleSet>> fareRules =
        ((DefaultFareService) getFareService(environment)).getFareRulesPerType();

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
        List<String> feeds = StreamSupport
          .stream(args.getLegacyGraphQLFeeds().spliterator(), false)
          .collect(Collectors.toList());
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

      VehicleParkingService vehicleParkingService = getRoutingService(environment)
        .getVehicleParkingService();

      if (vehicleParkingService == null) {
        return null;
      }

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
      VehicleParkingService vehicleParkingService = getRoutingService(environment)
        .getVehicleParkingService();

      if (vehicleParkingService == null) {
        return null;
      }

      var args = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeVehicleParkingsArgs(
        environment.getArguments()
      );

      if (args.getLegacyGraphQLIds() != null) {
        var idList = ((List<String>) args.getLegacyGraphQLIds());

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

      VehicleRentalService vehicleRentalStationService = getRoutingService(environment)
        .getVehicleRentalService();

      if (vehicleRentalStationService == null) {
        return null;
      }

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
      VehicleRentalService vehicleRentalStationService = getRoutingService(environment)
        .getVehicleRentalService();

      if (vehicleRentalStationService == null) {
        return null;
      }

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
        return ((List<String>) args.getLegacyGraphQLIds()).stream()
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

  private static <T> void call(
    DataFetchingEnvironment environment,
    String name,
    Consumer<T> consumer
  ) {
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

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getTransitService();
  }

  private FareService getFareService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getFareService();
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
}
