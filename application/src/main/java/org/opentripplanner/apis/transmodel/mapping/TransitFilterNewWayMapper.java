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
  static List<TransitFilter> mapFilter(List<Map<String, ?>> filters) {
    var filterRequests = new ArrayList<TransitFilter>();

    for (var filterInput : filters) {
      var filterRequestBuilder = TransitFilterRequest.of();

      if (filterInput.containsKey("select")) {
        var select = (List<Map<String, List<?>>>) filterInput.get("select");
        for (var it : select) {
          filterRequestBuilder.addSelect(SelectRequestMapper.mapSelectRequest(it));
        }
      }
      if (filterInput.containsKey("not")) {
        var not = (List<Map<String, List<?>>>) filterInput.get("not");
        for (var it : not) {
          filterRequestBuilder.addNot(SelectRequestMapper.mapSelectRequest(it));
        }
      }
      filterRequests.add(filterRequestBuilder.build());
    }
    return filterRequests;
  }
}
