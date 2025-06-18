package org.opentripplanner.apis.transmodel.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;

public class TransitFilterNewWayMapper {

  /** This is a utility class, only static methods */
  private TransitFilterNewWayMapper() {}

  @SuppressWarnings("unchecked")
  static List<TransitFilter> mapFilterNewWay(List<Map<String, ?>> filters) {
    var filterRequests = new ArrayList<TransitFilter>();

    for (var filterInput : filters) {
      var filterRequestBuilder = TransitFilterRequest.of();

      if (filterInput.containsKey("select")) {
        for (var selectInput : (List<Map<String, List<?>>>) filterInput.get("select")) {
          filterRequestBuilder.addSelect(SelectRequestMapper.mapSelectRequest(selectInput));
        }
      }

      if (filterInput.containsKey("not")) {
        for (var selectInput : (List<Map<String, List<?>>>) filterInput.get("not")) {
          filterRequestBuilder.addNot(SelectRequestMapper.mapSelectRequest(selectInput));
        }
      }

      filterRequests.add(filterRequestBuilder.build());
    }

    return filterRequests;
  }
}
