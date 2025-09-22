package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.utils.collection.CollectionUtils.requireNullOrNonEmpty;

import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLTransitFilterSelectInput;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.CollectionUtils;

class SelectRequestMapper {

  static SelectRequest.Builder mapSelectRequest(
    GraphQLTransitFilterSelectInput input,
    String name
  ) {
    var routes = input.getGraphQLRoutes();
    var agencies = input.getGraphQLAgencies();
    requireNullOrNonEmpty(routes, "preferences.transit.filters.%s.routes".formatted(name));
    requireNullOrNonEmpty(agencies, "preferences.transit.filters.%s.agencies".formatted(name));

    if (CollectionUtils.isEmpty(routes) && CollectionUtils.isEmpty(agencies)) {
      var type = GraphQLUtils.typeName(input);
      throw new IllegalArgumentException(
        "%s must contain at least one element in either 'routes or 'agencies'.".formatted(type)
      );
    }

    var selectRequestBuilder = SelectRequest.of();

    if (CollectionUtils.hasValue(routes)) {
      selectRequestBuilder.withRoutes(FeedScopedId.parse(routes));
    }

    if (CollectionUtils.hasValue(agencies)) {
      selectRequestBuilder.withAgencies(FeedScopedId.parse(agencies));
    }

    return selectRequestBuilder;
  }
}
