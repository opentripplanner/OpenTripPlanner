package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.utils.collection.CollectionUtils;
import org.opentripplanner.utils.collection.ListUtils;

class FilterMapper {

  static List<TransitFilter> mapFilters(List<GraphQLTypes.GraphQLPlanFilterInput> filters) {
    var filterRequests = new ArrayList<TransitFilter>();
    for (var filterInput : filters) {
      var selects = filterInput.getGraphQLSelect();
      var nots = filterInput.getGraphQLNot();
      CollectionUtils.requireNullOrNonEmpty(nots, "filters.not");
      CollectionUtils.requireNullOrNonEmpty(selects, "filters.select");

      var filterRequestBuilder = TransitFilterRequest.of();

      for (var selectInput : ListUtils.nullSafeImmutableList(selects)) {
        filterRequestBuilder.addSelect(SelectRequestMapper.mapSelectRequest(selectInput, "select"));
      }

      for (var selectInput : ListUtils.nullSafeImmutableList(nots)) {
        filterRequestBuilder.addNot(SelectRequestMapper.mapSelectRequest(selectInput, "not"));
      }

      filterRequests.add(filterRequestBuilder.build());
    }

    return filterRequests;
  }
}
