package org.opentripplanner.apis.gtfs.mapping.routerequest;

import graphql.schema.DataFetchingEnvironment;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;

public class ArgumentUtils {

  /**
   * This methods returns list of modes and their costs from the argument structure:
   * modes.transit.transit. This methods circumvents a bug in graphql-codegen as getting a list of
   * input objects is not possible through using the generated types in {@link GraphQLTypes}.
   * <p>
   * TODO this ugliness can be removed when the bug gets fixed
   */
  @Nullable
  static List<Map<String, Object>> getTransitModes(DataFetchingEnvironment environment) {
    if (environment.containsArgument("modes")) {
      Map<String, Object> modesArgs = environment.getArgument("modes");
      if (modesArgs.containsKey("transit")) {
        Map<String, Object> transitArgs = (Map<String, Object>) modesArgs.get("transit");
        if (transitArgs.containsKey("transit")) {
          return (List<Map<String, Object>>) transitArgs.get("transit");
        }
      }
    }
    return null;
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
  static Map<String, Object> getParking(DataFetchingEnvironment environment, String type) {
    return (
      (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ((Map<
                String,
                Object
              >) environment.getArgument("preferences")).get("street")).get(type)).get("parking")
    );
  }

  /**
   * This methods returns required/banned parking tags of the given type from argument structure:
   * preferences.street.type.parking.filters. This methods circumvents a bug in graphql-codegen as
   * getting a list of input objects is not possible through using the generated types in
   * {@link GraphQLTypes}.
   * <p>
   * TODO this ugliness can be removed when the bug gets fixed
   */
  static Collection<Map<String, Object>> getParkingFilters(
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
  static Collection<Map<String, Object>> getParkingPreferred(
    DataFetchingEnvironment environment,
    String type
  ) {
    var parking = getParking(environment, type);
    var preferred = parking != null && parking.containsKey("preferred")
      ? getParking(environment, type).get("preferred")
      : null;
    return preferred != null ? (Collection<Map<String, Object>>) preferred : List.of();
  }

  static Set<String> parseNotFilters(Collection<Map<String, Object>> filters) {
    return parseFilters(filters, "not");
  }

  static Set<String> parseSelectFilters(Collection<Map<String, Object>> filters) {
    return parseFilters(filters, "select");
  }

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
}
