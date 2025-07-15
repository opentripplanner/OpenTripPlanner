package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
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
        var typeName = GraphQLUtils.typeName(filterInput);
        throw new IllegalArgumentException(
          "%s must contain at least one 'include' or 'exclude'.".formatted(typeName)
        );
      }

      var filterRequestBuilder = TransitFilterRequest.of();

      // in the GTFS API modes can only be included but not excluded.
      if (CollectionUtils.isEmpty(includes)) {
        var modeSelect = SelectRequest.of().withTransportModes(modes).build();
        filterRequestBuilder.addSelect(modeSelect);
      } else {
        // for every inclusion we also need to add the modes, otherwise the filter will not work
        for (var selectInput : ListUtils.nullSafeImmutableList(includes)) {
          var builder = SelectRequestMapper.mapSelectRequest(selectInput, "include");
          builder.withTransportModes(modes);
          filterRequestBuilder.addSelect(builder.build());
        }
      }

      for (var selectInput : ListUtils.nullSafeImmutableList(excludes)) {
        filterRequestBuilder.addNot(
          SelectRequestMapper.mapSelectRequest(selectInput, "exclude").build()
        );
      }

      filterRequests.add(filterRequestBuilder.build());
    }

    return filterRequests;
  }
}
