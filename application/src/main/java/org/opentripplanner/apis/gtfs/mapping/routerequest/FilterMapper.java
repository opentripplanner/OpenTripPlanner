package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.utils.collection.CollectionUtils;
import org.opentripplanner.utils.collection.ListUtils;

class FilterMapper {

  static List<TransitFilter> mapFilters(
    List<MainAndSubMode> modes,
    List<GraphQLTypes.GraphQLTransitFilterInput> filters
  ) {
    var filterRequests = new ArrayList<TransitFilter>();
    for (var filterInput : filters) {
      var includes = filterInput.getGraphQLInclude();
      var excludes = filterInput.getGraphQLExclude();
      CollectionUtils.requireNullOrNonEmpty(includes, "filters.include");
      CollectionUtils.requireNullOrNonEmpty(excludes, "filters.exclude");

      if (CollectionUtils.isEmpty(excludes) && CollectionUtils.isEmpty(includes)) {
        throw new IllegalArgumentException("Filter must contain at least one 'select' or 'not'.");
      }

      var filterRequestBuilder = TransitFilterRequest.of()
        .addSelect(SelectRequest.of().withTransportModes(modes).build());

      for (var selectInput : ListUtils.nullSafeImmutableList(includes)) {
        filterRequestBuilder.addSelect(
          SelectRequestMapper.mapSelectRequest(selectInput, "include")
        );
      }

      for (var selectInput : ListUtils.nullSafeImmutableList(excludes)) {
        filterRequestBuilder.addNot(SelectRequestMapper.mapSelectRequest(selectInput, "exclude"));
      }

      filterRequests.add(filterRequestBuilder.build());
    }

    return filterRequests;
  }
}
