package org.opentripplanner.apis.transmodel.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;

public class FilterMapper {

  /**
   * Map filters from graphql to a list of {@link FilterRequest}'s.
   * @param filters the graphql input filters
   * @param selectRequestMapper a function mapping an individual selector to a {@link TSelectRequest}
   * @param <TSelectRequest> the type of selector contained in the select/not lists
   */
  @SuppressWarnings("unchecked")
  public static <TSelectRequest> List<FilterRequest<TSelectRequest>> mapFilters(
    List<Map<String, ?>> filters,
    Function<Map<String, List<?>>, TSelectRequest> selectRequestMapper
  ) {
    validateFieldNotEmpty("filters", filters);

    var filterRequests = new ArrayList<FilterRequest<TSelectRequest>>();

    for (var filterInput : filters) {
      var filterRequestBuilder = FilterRequest.<TSelectRequest>of();
      if (filterInput.containsKey("select")) {
        var select = (List<Map<String, List<?>>>) filterInput.get("select");
        validateFieldNotEmpty("select", select);
        for (var it : select) {
          filterRequestBuilder.addSelect(selectRequestMapper.apply(it));
        }
      }
      if (filterInput.containsKey("not")) {
        var not = (List<Map<String, List<?>>>) filterInput.get("not");
        validateFieldNotEmpty("not", not);
        for (var it : not) {
          filterRequestBuilder.addNot(selectRequestMapper.apply(it));
        }
      }
      if (!filterInput.containsKey("select") && !filterInput.containsKey("not")) {
        throw new InvalidInputException("A filter must have at least one of 'select' or 'not'.");
      }
      filterRequests.add(filterRequestBuilder.build());
    }
    return filterRequests;
  }

  private static void validateFieldNotEmpty(String fieldName, List<?> values) {
    if (values.isEmpty()) {
      throw new InvalidInputException("'%s' cannot be an empty list".formatted(fieldName));
    }
  }
}
