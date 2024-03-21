package org.opentripplanner.apis.gtfs.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.framework.collection.CollectionUtils;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class RouteRequestMapper {

  @Nonnull
  public static RouteRequest toRouteRequest(
    DataFetchingEnvironment environment,
    GraphQLRequestContext context
  ) {
    RouteRequest request = context.defaultRouteRequest();
    var args = new GraphQLTypes.GraphQLQueryTypePlanConnectionArgs(environment.getArguments());
    var dateTime = args.getGraphQLDateTime();
    if (dateTime.getGraphQLEarliestDeparture() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLEarliestDeparture().toInstant());
    } else if (dateTime.getGraphQLLatestArrival() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLLatestArrival().toInstant());
      request.setArriveBy(true);
    } else {
      request.setDateTime(Instant.now());
    }
    request.setFrom(parseGenericLocation(args.getGraphQLOrigin()));
    request.setTo(parseGenericLocation(args.getGraphQLDestination()));
    request.setLocale(GraphQLUtils.getLocale(environment, args.getGraphQLLocale()));
    if (args.getGraphQLSearchWindow() != null) {
      request.setSearchWindow(
        DurationUtils.requireNonNegativeLarge(args.getGraphQLSearchWindow(), "searchWindow")
      );
    }

    if (args.getGraphQLBefore() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLBefore());
      if (args.getGraphQLLast() != null) {
        request.setNumItineraries(args.getGraphQLLast());
      }
    } else if (args.getGraphQLAfter() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLAfter());
      if (args.getGraphQLFirst() != null) {
        request.setNumItineraries(args.getGraphQLFirst());
      }
    } else if (args.getGraphQLNumberOfItineraries() != null) {
      request.setNumItineraries(args.getGraphQLNumberOfItineraries());
    }

    request.withPreferences(preferences -> setPreferences(preferences, request, args, environment));

    setModes(request.journey(), args.getGraphQLModes(), environment);

    return request;
  }

  static Set<String> parseNotFilters(Collection<Map<String, Object>> filters) {
    return parseFilters(filters, "not");
  }

  static Set<String> parseSelectFilters(Collection<Map<String, Object>> filters) {
    return parseFilters(filters, "select");
  }

  @Nonnull
  private static Set<String> parseFilters(Collection<Map<String, Object>> filters, String key) {
    return filters
      .stream()
      .flatMap(f ->
        parseOperation((Collection<Map<String, Collection<String>>>) f.getOrDefault(key, List.of()))
      )
      .collect(Collectors.toSet());
  }

  private static Stream<String> parseOperation(Collection<Map<String, Collection<String>>> map) {
    return map
      .stream()
      .flatMap(f -> {
        var tags = f.getOrDefault("tags", List.of());
        return tags.stream();
      });
  }

  private static void setPreferences(
    RoutingPreferences.Builder prefs,
    RouteRequest request,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args,
    DataFetchingEnvironment environment
  ) {
    var preferenceArgs = args.getGraphQLPreferences();
    prefs.withItineraryFilter(filters ->
      setItineraryFilters(filters, args.getGraphQLItineraryFilter())
    );
    prefs.withTransit(transit -> {
      prefs.withTransfer(transfer -> setTransitPreferences(transit, transfer, args, environment));
    });
    setStreetPreferences(prefs, preferenceArgs.getGraphQLStreet(), environment);
    setAccessibilityPreferences(request, preferenceArgs.getGraphQLAccessibility());
  }

  private static void setItineraryFilters(
    ItineraryFilterPreferences.Builder filterPreferences,
    GraphQLTypes.GraphQLPlanItineraryFilterInput filters
  ) {
    if (filters.getGraphQLItineraryFilterDebugProfile() != null) {
      filterPreferences.withDebug(
        ItineraryFilterDebugProfileMapper.map(filters.getGraphQLItineraryFilterDebugProfile())
      );
    }
    if (filters.getGraphQLGroupSimilarityKeepOne() != null) {
      filterPreferences.withGroupSimilarityKeepOne(filters.getGraphQLGroupSimilarityKeepOne());
    }
    if (filters.getGraphQLGroupSimilarityKeepThree() != null) {
      filterPreferences.withGroupSimilarityKeepThree(filters.getGraphQLGroupSimilarityKeepThree());
    }
    if (filters.getGraphQLGroupedOtherThanSameLegsMaxCostMultiplier() != null) {
      filterPreferences.withGroupedOtherThanSameLegsMaxCostMultiplier(
        filters.getGraphQLGroupedOtherThanSameLegsMaxCostMultiplier()
      );
    }
  }

  private static void setTransitPreferences(
    TransitPreferences.Builder transitPreferences,
    TransferPreferences.Builder transferPreferences,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args,
    DataFetchingEnvironment environment
  ) {
    var modes = args.getGraphQLModes();
    var transit = getTransitModes(environment);
    if (!Boolean.TRUE.equals(modes.getGraphQLDirectOnly()) && !CollectionUtils.isEmpty(transit)) {
      var reluctanceForMode = transit
        .stream()
        .filter(mode -> mode.containsKey("cost"))
        .collect(
          Collectors.toMap(
            mode ->
              TransitModeMapper.map(
                GraphQLTypes.GraphQLTransitMode.valueOf((String) mode.get("mode"))
              ),
            mode -> (Double) ((Map<String, Object>) mode.get("cost")).get("reluctance")
          )
        );
      transitPreferences.setReluctanceForMode(reluctanceForMode);
    }
    var transitArgs = args.getGraphQLPreferences().getGraphQLTransit();
    if (transitArgs != null) {
      var board = transitArgs.getGraphQLBoard();
      if (board != null) {
        var slack = board.getGraphQLSlack();
        if (slack != null) {
          transitPreferences.withDefaultBoardSlackSec(
            (int) DurationUtils.requireNonNegativeMedium(slack, "board slack").toSeconds()
          );
        }
        var waitReluctance = board.getGraphQLWaitReluctance();
        if (waitReluctance != null) {
          transferPreferences.withWaitReluctance(waitReluctance);
        }
      }
      var alight = transitArgs.getGraphQLAlight();
      if (alight != null) {
        var slack = alight.getGraphQLSlack();
        if (slack != null) {
          transitPreferences.withDefaultAlightSlackSec(
            (int) DurationUtils.requireNonNegativeMedium(slack, "alight slack").toSeconds()
          );
        }
      }
      var transfer = transitArgs.getGraphQLTransfer();
      if (transfer != null) {
        var cost = transfer.getGraphQLCost();
        if (cost != null) {
          transferPreferences.withCost(cost.toSeconds());
        }
        var slack = transfer.getGraphQLSlack();
        if (slack != null) {
          transferPreferences.withSlack(
            (int) DurationUtils.requireNonNegativeMedium(slack, "transfer slack").toSeconds()
          );
        }
        var maxTransfers = transfer.getGraphQLMaximumTransfers();
        if (maxTransfers != null) {
          transferPreferences.withMaxTransfers(maxTransfers);
        }
        var additionalTransfers = transfer.getGraphQLMaximumAdditionalTransfers();
        if (additionalTransfers != null) {
          transferPreferences.withMaxAdditionalTransfers(additionalTransfers);
        }
      }
      var timetable = transitArgs.getGraphQLTimetable();
      if (timetable != null) {
        var excludeUpdates = timetable.getGraphQLExcludeRealTimeUpdates();
        if (excludeUpdates != null) {
          transitPreferences.setIgnoreRealtimeUpdates(excludeUpdates);
        }
        var includePlannedCancellations = timetable.getGraphQLIncludePlannedCancellations();
        if (includePlannedCancellations != null) {
          transitPreferences.setIncludePlannedCancellations(includePlannedCancellations);
        }
        var includeRealtimeCancellations = timetable.getGraphQLIncludeRealTimeCancellations();
        if (includeRealtimeCancellations != null) {
          transitPreferences.setIncludeRealtimeCancellations(includeRealtimeCancellations);
        }
      }
    }
  }

  private static void setStreetPreferences(
    RoutingPreferences.Builder preferences,
    GraphQLTypes.GraphQLPlanStreetPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args != null) {
      preferences.withBike(bicycle ->
        setBicyclePreferences(bicycle, args.getGraphQLBicycle(), environment)
      );
      preferences.withCar(car -> setCarPreferences(car, args.getGraphQLCar(), environment));
      preferences.withScooter(scooter -> setScooterPreferences(scooter, args.getGraphQLScooter()));
      preferences.withWalk(walk -> setWalkPreferences(walk, args.getGraphQLWalk()));
    }
  }

  private static void setBicyclePreferences(
    BikePreferences.Builder preferences,
    GraphQLTypes.GraphQLBicyclePreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args != null) {
      var speed = args.getGraphQLSpeed();
      if (speed != null) {
        preferences.withSpeed(speed);
      }
      var reluctance = args.getGraphQLReluctance();
      if (reluctance != null) {
        preferences.withReluctance(reluctance);
      }
      var boardCost = args.getGraphQLBoardCost();
      if (boardCost != null) {
        preferences.withBoardCost(boardCost.toSeconds());
      }
      preferences.withWalking(walk -> setBicycleWalkPreferences(walk, args.getGraphQLWalk()));
      preferences.withParking(parking ->
        setBicycleParkingPreferences(parking, args.getGraphQLParking(), environment)
      );
      preferences.withRental(rental -> setBicycleRentalPreferences(rental, args.getGraphQLRental())
      );
      setBicycleOptimization(preferences, args.getGraphQLOptimization());
    }
  }

  private static void setBicycleWalkPreferences(
    VehicleWalkingPreferences.Builder preferences,
    GraphQLTypes.GraphQLBicycleWalkPreferencesInput args
  ) {
    if (args != null) {
      var speed = args.getGraphQLSpeed();
      if (speed != null) {
        preferences.withSpeed(speed);
      }
      var mountTime = args.getGraphQLMountDismountTime();
      if (mountTime != null) {
        preferences.withMountDismountTime(
          DurationUtils.requireNonNegativeShort(mountTime, "bicycle mount dismount time")
        );
      }
      var cost = args.getGraphQLCost();
      if (cost != null) {
        var reluctance = cost.getGraphQLReluctance();
        if (reluctance != null) {
          preferences.withReluctance(reluctance);
        }
        var mountCost = cost.getGraphQLMountDismountCost();
        if (mountCost != null) {
          preferences.withMountDismountCost(mountCost.toSeconds());
        }
      }
    }
  }

  private static void setBicycleParkingPreferences(
    VehicleParkingPreferences.Builder preferences,
    GraphQLTypes.GraphQLBicycleParkingPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args != null) {
      var unpreferredCost = args.getGraphQLUnpreferredCost();
      if (unpreferredCost != null) {
        preferences.withUnpreferredVehicleParkingTagCost(unpreferredCost.toSeconds());
      }
      var filters = getParkingFilters(environment, "bicycle");
      preferences.withRequiredVehicleParkingTags(parseSelectFilters(filters));
      preferences.withBannedVehicleParkingTags(parseNotFilters(filters));
      var preferred = getParkingPreferred(environment, "bicycle");
      preferences.withPreferredVehicleParkingTags(parseSelectFilters(preferred));
      preferences.withNotPreferredVehicleParkingTags(parseNotFilters(preferred));
    }
  }

  /**
   * This methods returns required/banned parking tags of the given type from argument structure:
   * preferences.street.type.parking.filters. This methods circumvents a bug in graphql-codegen as
   * getting a list of input objects is not possible through using the generated types in
   * {@link GraphQLTypes}.
   * <p>
   * TODO this ugliness can be removed when the bug gets fixed
   */
  @Nonnull
  private static Collection<Map<String, Object>> getParkingFilters(
    DataFetchingEnvironment environment,
    String type
  ) {
    var parking = getParking(environment, type);
    var filters = parking != null && parking.containsKey("filters")
      ? getParking(environment, type).get("filters")
      : null;
    return filters != null ? (Collection<Map<String, Object>>) filters : List.of();
  }

  /**
   * This methods returns preferred/unpreferred parking tags of the given type from argument
   * structure: preferences.street.type.parking.preferred. This methods circumvents a bug in
   * graphql-codegen as getting a list of input objects is not possible through using the generated
   * types in {@link GraphQLTypes}.
   * <p>
   * TODO this ugliness can be removed when the bug gets fixed
   */
  @Nonnull
  private static Collection<Map<String, Object>> getParkingPreferred(
    DataFetchingEnvironment environment,
    String type
  ) {
    var parking = getParking(environment, type);
    var preferred = parking != null && parking.containsKey("preferred")
      ? getParking(environment, type).get("preferred")
      : null;
    return preferred != null ? (Collection<Map<String, Object>>) preferred : List.of();
  }

  /**
   * This methods returns parking preferences of the given type from argument structure:
   * preferences.street.type.parking. This methods circumvents a bug in graphql-codegen as getting a
   * list of input objects is not possible through using the generated types in
   * {@link GraphQLTypes}.
   * <p>
   * TODO this ugliness can be removed when the bug gets fixed
   */
  @Nullable
  private static Map<String, Object> getParking(DataFetchingEnvironment environment, String type) {
    return (
      (Map<String, Object>) (
        (Map<String, Object>) (
          (Map<String, Object>) ((Map<String, Object>) environment.getArgument("preferences")).get(
              "street"
            )
        ).get(type)
      ).get("parking")
    );
  }

  private static void setBicycleRentalPreferences(
    VehicleRentalPreferences.Builder preferences,
    GraphQLTypes.GraphQLBicycleRentalPreferencesInput args
  ) {
    if (args != null) {
      var allowedNetworks = args.getGraphQLAllowedNetworks();
      if (!CollectionUtils.isEmpty(allowedNetworks)) {
        preferences.withAllowedNetworks(Set.copyOf(allowedNetworks));
      }
      var bannedNetworks = args.getGraphQLBannedNetworks();
      if (!CollectionUtils.isEmpty(bannedNetworks)) {
        preferences.withBannedNetworks(Set.copyOf(bannedNetworks));
      }
      var destinationPolicy = args.getGraphQLDestinationBicyclePolicy();
      if (destinationPolicy != null) {
        var allowed = destinationPolicy.getGraphQLAllowKeeping();
        if (allowed != null) {
          preferences.withAllowArrivingInRentedVehicleAtDestination(allowed);
        }
        var cost = destinationPolicy.getGraphQLKeepingCost();
        if (cost != null) {
          preferences.withArrivingInRentalVehicleAtDestinationCost(cost.toSeconds());
        }
      }
    }
  }

  private static void setBicycleOptimization(
    BikePreferences.Builder preferences,
    GraphQLTypes.GraphQLCyclingOptimizationInput args
  ) {
    if (args != null) {
      var type = args.getGraphQLType();
      var mappedType = type != null ? VehicleOptimizationTypeMapper.map(type) : null;
      if (mappedType != null) {
        preferences.withOptimizeType(mappedType);
      }
      var triangleArgs = args.getGraphQLTriangle();
      if (isBicycleTriangleSet(triangleArgs)) {
        preferences.withForcedOptimizeTriangle(triangle -> {
          triangle
            .withSlope(triangleArgs.getGraphQLFlatness())
            .withSafety(triangleArgs.getGraphQLSafety())
            .withTime(triangleArgs.getGraphQLTime());
        });
      }
    }
  }

  private static boolean isBicycleTriangleSet(
    GraphQLTypes.GraphQLTriangleCyclingFactorsInput args
  ) {
    return (
      args != null &&
      args.getGraphQLFlatness() != null &&
      args.getGraphQLSafety() != null &&
      args.getGraphQLTime() != null
    );
  }

  private static void setCarPreferences(
    CarPreferences.Builder preferences,
    GraphQLTypes.GraphQLCarPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args != null) {
      var reluctance = args.getGraphQLReluctance();
      if (reluctance != null) {
        preferences.withReluctance(reluctance);
      }
      preferences.withParking(parking ->
        setCarParkingPreferences(parking, args.getGraphQLParking(), environment)
      );
      preferences.withRental(rental -> setCarRentalPreferences(rental, args.getGraphQLRental()));
    }
  }

  private static void setCarParkingPreferences(
    VehicleParkingPreferences.Builder preferences,
    GraphQLTypes.GraphQLCarParkingPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args != null) {
      var unpreferredCost = args.getGraphQLUnpreferredCost();
      if (unpreferredCost != null) {
        preferences.withUnpreferredVehicleParkingTagCost(unpreferredCost.toSeconds());
      }
      var filters = getParkingFilters(environment, "car");
      preferences.withRequiredVehicleParkingTags(parseSelectFilters(filters));
      preferences.withBannedVehicleParkingTags(parseNotFilters(filters));
      var preferred = getParkingPreferred(environment, "car");
      preferences.withPreferredVehicleParkingTags(parseSelectFilters(preferred));
      preferences.withNotPreferredVehicleParkingTags(parseNotFilters(preferred));
    }
  }

  private static void setCarRentalPreferences(
    VehicleRentalPreferences.Builder preferences,
    GraphQLTypes.GraphQLCarRentalPreferencesInput args
  ) {
    if (args != null) {
      var allowedNetworks = args.getGraphQLAllowedNetworks();
      if (!CollectionUtils.isEmpty(allowedNetworks)) {
        preferences.withBannedNetworks(Set.copyOf(allowedNetworks));
      }
      var bannedNetworks = args.getGraphQLBannedNetworks();
      if (!CollectionUtils.isEmpty(bannedNetworks)) {
        preferences.withBannedNetworks(Set.copyOf(bannedNetworks));
      }
    }
  }

  private static void setScooterPreferences(
    ScooterPreferences.Builder preferences,
    GraphQLTypes.GraphQLScooterPreferencesInput args
  ) {
    if (args != null) {
      var speed = args.getGraphQLSpeed();
      if (speed != null) {
        preferences.withSpeed(speed);
      }
      var reluctance = args.getGraphQLReluctance();
      if (reluctance != null) {
        preferences.withReluctance(reluctance);
      }
      preferences.withRental(rental -> setScooterRentalPreferences(rental, args.getGraphQLRental())
      );
      setScooterOptimization(preferences, args.getGraphQLOptimization());
    }
  }

  private static void setScooterRentalPreferences(
    VehicleRentalPreferences.Builder preferences,
    GraphQLTypes.GraphQLScooterRentalPreferencesInput args
  ) {
    if (args != null) {
      var allowedNetworks = args.getGraphQLAllowedNetworks();
      if (!CollectionUtils.isEmpty(allowedNetworks)) {
        preferences.withBannedNetworks(Set.copyOf(allowedNetworks));
      }
      var bannedNetworks = args.getGraphQLBannedNetworks();
      if (!CollectionUtils.isEmpty(bannedNetworks)) {
        preferences.withBannedNetworks(Set.copyOf(bannedNetworks));
      }
      var destinationPolicy = args.getGraphQLDestinationScooterPolicy();
      if (destinationPolicy != null) {
        var allowed = destinationPolicy.getGraphQLAllowKeeping();
        if (allowed != null) {
          preferences.withAllowArrivingInRentedVehicleAtDestination(allowed);
        }
        var cost = destinationPolicy.getGraphQLKeepingCost();
        if (cost != null) {
          preferences.withArrivingInRentalVehicleAtDestinationCost(cost.toSeconds());
        }
      }
    }
  }

  private static void setScooterOptimization(
    ScooterPreferences.Builder preferences,
    GraphQLTypes.GraphQLScooterOptimizationInput args
  ) {
    if (args != null) {
      var type = args.getGraphQLType();
      var mappedType = type != null ? VehicleOptimizationTypeMapper.map(type) : null;
      if (mappedType != null) {
        preferences.withOptimizeType(mappedType);
      }
      var triangleArgs = args.getGraphQLTriangle();
      if (isScooterTriangleSet(triangleArgs)) {
        preferences.withForcedOptimizeTriangle(triangle -> {
          triangle
            .withSlope(triangleArgs.getGraphQLFlatness())
            .withSafety(triangleArgs.getGraphQLSafety())
            .withTime(triangleArgs.getGraphQLTime());
        });
      }
    }
  }

  private static boolean isScooterTriangleSet(
    GraphQLTypes.GraphQLTriangleScooterFactorsInput args
  ) {
    return (
      args != null &&
      args.getGraphQLFlatness() != null &&
      args.getGraphQLSafety() != null &&
      args.getGraphQLTime() != null
    );
  }

  private static void setWalkPreferences(
    WalkPreferences.Builder preferences,
    GraphQLTypes.GraphQLWalkPreferencesInput args
  ) {
    if (args != null) {
      var speed = args.getGraphQLSpeed();
      if (speed != null) {
        preferences.withSpeed(speed);
      }
      var reluctance = args.getGraphQLReluctance();
      if (reluctance != null) {
        preferences.withReluctance(reluctance);
      }
      var walkSafetyFactor = args.getGraphQLWalkSafetyFactor();
      if (walkSafetyFactor != null) {
        preferences.withSafetyFactor(walkSafetyFactor);
      }
      var boardCost = args.getGraphQLBoardCost();
      if (boardCost != null) {
        preferences.withBoardCost(boardCost.toSeconds());
      }
    }
  }

  private static void setAccessibilityPreferences(
    RouteRequest request,
    GraphQLTypes.GraphQLAccessibilityPreferencesInput preferenceArgs
  ) {
    if (preferenceArgs != null && preferenceArgs.getGraphQLWheelchair() != null) {
      request.setWheelchair(preferenceArgs.getGraphQLWheelchair().getGraphQLEnabled());
    }
  }

  /**
   * TODO this doesn't support multiple street modes yet
   */
  private static void setModes(
    JourneyRequest journey,
    GraphQLTypes.GraphQLPlanModesInput modesInput,
    DataFetchingEnvironment environment
  ) {
    var direct = modesInput.getGraphQLDirect();
    if (Boolean.TRUE.equals(modesInput.getGraphQLTransitOnly())) {
      journey.direct().setMode(StreetMode.NOT_SET);
    } else if (!CollectionUtils.isEmpty(direct)) {
      journey.direct().setMode(DirectModeMapper.map(direct.getFirst()));
    }

    var transit = modesInput.getGraphQLTransit();
    if (Boolean.TRUE.equals(modesInput.getGraphQLDirectOnly())) {
      journey.transit().disable();
    } else if (transit != null) {
      var access = transit.getGraphQLAccess();
      if (!CollectionUtils.isEmpty(access)) {
        journey.access().setMode(AccessModeMapper.map(access.getFirst()));
      }

      var egress = transit.getGraphQLEgress();
      if (!CollectionUtils.isEmpty(egress)) {
        journey.egress().setMode(EgressModeMapper.map(egress.getFirst()));
      }

      var transfer = transit.getGraphQLTransfer();
      if (!CollectionUtils.isEmpty(transfer)) {
        journey.transfer().setMode(TransferModeMapper.map(transfer.getFirst()));
      }
      validateStreetModes(journey);

      var transitModes = getTransitModes(environment);
      if (!CollectionUtils.isEmpty(transitModes)) {
        var filterRequestBuilder = TransitFilterRequest.of();
        var mainAndSubModes = transitModes
          .stream()
          .map(mode ->
            new MainAndSubMode(
              TransitModeMapper.map(
                GraphQLTypes.GraphQLTransitMode.valueOf((String) mode.get("mode"))
              )
            )
          )
          .toList();
        filterRequestBuilder.addSelect(
          SelectRequest.of().withTransportModes(mainAndSubModes).build()
        );
        journey.transit().setFilters(List.of(filterRequestBuilder.build()));
      }
    }
  }

  /**
   * TODO this doesn't support multiple street modes yet
   */
  private static void validateStreetModes(JourneyRequest journey) {
    Set<StreetMode> modes = new HashSet();
    modes.add(journey.access().mode());
    modes.add(journey.egress().mode());
    modes.add(journey.transfer().mode());
    if (modes.contains(StreetMode.BIKE) && modes.size() != 1) {
      throw new IllegalArgumentException(
        "If BICYCLE is used for access, egress or transfer, then it should be used for all."
      );
    }
  }

  /**
   * This methods returns list of modes and their costs from the argument structure:
   * modes.transit.transit. This methods circumvents a bug in graphql-codegen as getting a list of
   * input objects is not possible through using the generated types in {@link GraphQLTypes}.
   * <p>
   * TODO this ugliness can be removed when the bug gets fixed
   */
  private static List<Map<String, Object>> getTransitModes(DataFetchingEnvironment environment) {
    if (environment.containsArgument("modes")) {
      Map<String, Object> modesArgs = environment.getArgument("modes");
      if (modesArgs.containsKey("transit")) {
        Map<String, Object> transitArgs = (Map<String, Object>) modesArgs.get("transit");
        if (transitArgs.containsKey("transit")) {
          return (List<Map<String, Object>>) transitArgs.get("transit");
        }
      }
    }
    return List.of();
  }

  private static GenericLocation parseGenericLocation(
    GraphQLTypes.GraphQLPlanLabeledLocationInput locationInput
  ) {
    var stopLocation = locationInput.getGraphQLLocation().getGraphQLStopLocation();
    if (stopLocation.getGraphQLStopLocationId() != null) {
      var stopId = stopLocation.getGraphQLStopLocationId();
      if (FeedScopedId.isValidString(stopId)) {
        return new GenericLocation(
          locationInput.getGraphQLLabel(),
          FeedScopedId.parse(stopId),
          null,
          null
        );
      } else {
        throw new IllegalArgumentException("Stop id %s is not of valid format.".formatted(stopId));
      }
    }

    var coordinate = locationInput.getGraphQLLocation().getGraphQLCoordinate();
    return new GenericLocation(
      locationInput.getGraphQLLabel(),
      null,
      coordinate.getGraphQLLatitude(),
      coordinate.getGraphQLLongitude()
    );
  }
}
