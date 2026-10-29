package org.opentripplanner.transfer.configure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

/**
 * Finds the street parameters of the transfers that are different from the {@code routingDefaults}
 * of the router configuration.
 * <p>
 * The stored transfers are searched with the street parameters set in the block of the profile
 * type in {@code build-config.json}, see {@code TransferRequestConfig}. The router configuration is
 * not available when the graph is built, so the {@code routingDefaults} of {@code
 * router-config.json} is not used for them. If the two are different, the transfers are found with
 * other values than a routing request uses for the street search, and nothing else tells.
 * <p>
 * Only the parameters that can be set for the type are compared. The user preferences, for
 * example the speed, are not, they are set by each request.
 */
final class TransferRequestDifferences {

  /**
   * @param isComparable whether the parameter is compared for the two requests, for example the
   *                     costs of a stop are only compared if both requests use stops with unknown
   *                     accessibility
   */
  private record Parameter(
    String name,
    Function<RouteRequest, Object> value,
    BiPredicate<RouteRequest, RouteRequest> isComparable
  ) {}

  private static final List<Parameter> WALK = List.of(
    parameter("walk.stairsReluctance", r -> r.preferences().walk().stairsReluctance()),
    parameter("walk.stairsTimeFactor", r -> r.preferences().walk().stairsTimeFactor()),
    parameter("walk.escalator.speed", r -> r.preferences().walk().escalator().speed()),
    parameter("walk.escalator.reluctance", r -> r.preferences().walk().escalator().reluctance())
  );

  private static final List<Parameter> WHEELCHAIR = new ArrayList<>();

  static {
    addAccessibility("wheelchair.stop", r -> r.preferences().wheelchair().stop());
    addAccessibility("wheelchair.elevator", r -> r.preferences().wheelchair().elevator());
    WHEELCHAIR.add(
      parameter("wheelchair.inaccessibleStreetReluctance", r ->
        r.preferences().wheelchair().inaccessibleStreetReluctance()
      )
    );
    WHEELCHAIR.add(parameter("wheelchair.maxSlope", r -> r.preferences().wheelchair().maxSlope()));
    WHEELCHAIR.add(
      parameter("wheelchair.slopeExceededReluctance", r ->
        r.preferences().wheelchair().slopeExceededReluctance()
      )
    );
    WHEELCHAIR.add(
      parameter("wheelchair.stairsReluctance", r -> r.preferences().wheelchair().stairsReluctance())
    );
  }

  private static final List<Parameter> BICYCLE = List.of(
    parameter("bike.walking.stairsReluctance", r ->
      r.preferences().bike().walking().stairsReluctance()
    ),
    parameter("bike.walking.mountDismountCost", r ->
      r.preferences().bike().walking().mountDismountCost().toSeconds()
    ),
    parameter("bike.walking.mountDismountTime", r ->
      r.preferences().bike().walking().mountDismountTime()
    )
  );

  private TransferRequestDifferences() {}

  /**
   * A warning for each profile type that has parameters different from {@code routingDefaults}.
   * All the profiles of a type share the same request, so the first profile of the type is used.
   */
  static List<String> warnings(
    TransferParameters<RouteRequest> parameters,
    RouteRequest routingDefaults
  ) {
    var warnings = new ArrayList<String>();
    for (var type : TransferProfileType.values()) {
      parameters
        .profiles()
        .stream()
        .filter(profile -> profile.type() == type)
        .findFirst()
        .ifPresent(profile -> {
          var differences = of(type, profile.routingRequest(), routingDefaults);
          if (!differences.isEmpty()) {
            warnings.add(
              "The street parameters of the '%s' transfers are different from routingDefaults in router-config.json. The transfers are found with the values of transfers in build-config.json. Differences: %s".formatted(
                type.name().toLowerCase(),
                String.join("; ", differences)
              )
            );
          }
        });
    }
    return warnings;
  }

  /** The parameters of {@code type} that are different, as {@code name: value, value}. */
  static List<String> of(
    TransferProfileType type,
    RouteRequest transfers,
    RouteRequest routingDefaults
  ) {
    var result = new ArrayList<String>();
    for (var parameter : parametersOf(type)) {
      if (!parameter.isComparable().test(transfers, routingDefaults)) {
        continue;
      }
      var transferValue = parameter.value().apply(transfers);
      var defaultValue = parameter.value().apply(routingDefaults);
      if (!Objects.equals(transferValue, defaultValue)) {
        result.add(
          "%s: %s (transfers), %s (routingDefaults)".formatted(
            parameter.name(),
            transferValue,
            defaultValue
          )
        );
      }
    }
    return result;
  }

  private static List<Parameter> parametersOf(TransferProfileType type) {
    return switch (type) {
      case WALK -> WALK;
      case WHEELCHAIR -> WHEELCHAIR;
      case BICYCLE -> BICYCLE;
      case CAR -> List.of();
    };
  }

  private static Parameter parameter(String name, Function<RouteRequest, Object> value) {
    return new Parameter(name, value, (a, b) -> true);
  }

  private static void addAccessibility(
    String name,
    Function<RouteRequest, AccessibilityPreferences> preferences
  ) {
    WHEELCHAIR.add(
      parameter(name + ".onlyConsiderAccessible", r ->
        preferences.apply(r).onlyConsiderAccessible()
      )
    );
    BiPredicate<RouteRequest, RouteRequest> bothUseCosts = (a, b) ->
      !preferences.apply(a).onlyConsiderAccessible() &&
      !preferences.apply(b).onlyConsiderAccessible();
    WHEELCHAIR.add(
      new Parameter(name + ".unknownCost", r -> preferences.apply(r).unknownCost(), bothUseCosts)
    );
    WHEELCHAIR.add(
      new Parameter(
        name + ".inaccessibleCost",
        r -> preferences.apply(r).inaccessibleCost(),
        bothUseCosts
      )
    );
  }
}
