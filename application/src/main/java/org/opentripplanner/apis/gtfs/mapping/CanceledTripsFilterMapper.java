package org.opentripplanner.apis.gtfs.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.collection.CollectionUtils;

public class CanceledTripsFilterMapper {

  public static TripOnServiceDateRequest mapToTripOnServiceDateRequest(
    DataFetchingEnvironment env
  ) {
    var filters = new GraphQLTypes.GraphQLQueryTypeCanceledTripsArgs(
      env.getArguments()
    ).getGraphQLFilters();
    if (CollectionUtils.isEmpty(filters)) {
      return TripOnServiceDateRequest.of().build();
    }
    if (filters.size() > 1) {
      throw new IllegalArgumentException("Only one filter is allowed for now.");
    }
    var filter = filters.getFirst();
    var includes = filter.getGraphQLInclude();
    var excludes = filter.getGraphQLExclude();
    CollectionUtils.requireNullOrNonEmpty(includes, "filters.include");
    CollectionUtils.requireNullOrNonEmpty(excludes, "filters.exclude");
    var modesToInclude = CanceledTripsFilterMapper.toTransitModes(includes);
    var modesToExclude = CanceledTripsFilterMapper.toTransitModes(excludes);
    var modesToIncludeFilter = FilterValues.ofNullIsEverything("modesToInclude", modesToInclude);
    var modesToExcludeFilter = FilterValues.ofNullIsEverything("modesToExclude", modesToExclude);
    return TripOnServiceDateRequest.of()
      .withIncludeModes(modesToIncludeFilter)
      .withExcludeModes(modesToExcludeFilter)
      .build();
  }

  @Nullable
  private static Set<TransitMode> toTransitModes(
    List<GraphQLTypes.GraphQLCanceledTripsFilterSelectInput> inputs
  ) {
    if (inputs == null) {
      return null;
    }
    if (
      inputs
        .stream()
        .anyMatch(input -> input.getGraphQLModes() != null && input.getGraphQLModes().isEmpty())
    ) {
      throw new IllegalArgumentException(
        "Mode filter must be either null or have at least one entry."
      );
    }
    return inputs
      .stream()
      .flatMap(include -> {
        var modes = include.getGraphQLModes();
        if (modes == null) {
          return Stream.of();
        }
        return include.getGraphQLModes().stream().map(TransitModeMapper::map);
      })
      .collect(Collectors.toSet());
  }
}
