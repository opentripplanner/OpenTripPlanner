package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.utils.collection.CollectionUtils.requireNullOrNonEmpty;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLTransitFilterSelectInput;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.CollectionUtils;

class SelectRequestMapper {

  static SelectRequest mapSelectRequest(GraphQLTransitFilterSelectInput input, String name) {
    var routes = input.getGraphQLRoutes();
    var agencies = input.getGraphQLAgencies();
    requireNullOrNonEmpty(routes, "preferences.transit.filters.%s.routes".formatted(name));
    requireNullOrNonEmpty(agencies, "preferences.transit.filters.%s.agencies".formatted(name));

    var selectRequestBuilder = SelectRequest.of();
    if (CollectionUtils.hasValue(routes)) {
      selectRequestBuilder.withRoutes(FeedScopedId.parse(routes));
    }

    if (CollectionUtils.hasValue(agencies)) {
      selectRequestBuilder.withAgencies(FeedScopedId.parse(agencies));
    }

    return selectRequestBuilder.build();
  }
}
