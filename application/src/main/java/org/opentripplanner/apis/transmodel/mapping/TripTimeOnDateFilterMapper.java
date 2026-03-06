package org.opentripplanner.apis.transmodel.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateFilterRequest;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateSelectRequest;

/**
 * Maps GraphQL {@code EstimatedCallFilterInput} to a list of {@link TripTimeOnDateFilterRequest}
 * objects by extracting the {@code select} and {@code not} criteria from the GraphQL input.
 * <p>
 * Each filter has {@code select} and {@code not} arrays of select criteria.
 * <p>
 * Empty lists are not allowed and will result in an {@link IllegalArgumentException}.
 */
public class TripTimeOnDateFilterMapper {

  private final FeedScopedIdMapper idMapper;

  public TripTimeOnDateFilterMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  @SuppressWarnings("unchecked")
  public List<TripTimeOnDateFilterRequest> mapFilters(List<Map<String, ?>> filters) {
    validateNonEmpty("filters", filters);

    var filterRequests = new ArrayList<TripTimeOnDateFilterRequest>();

    for (var filterInput : filters) {
      var filterRequestBuilder = TripTimeOnDateFilterRequest.of();

      if (filterInput.containsKey("select")) {
        var select = (List<Map<String, List<?>>>) filterInput.get("select");
        validateNonEmpty("select", select);
        for (var it : select) {
          filterRequestBuilder.addSelect(mapSelectRequest(it));
        }
      }
      if (filterInput.containsKey("not")) {
        var not = (List<Map<String, List<?>>>) filterInput.get("not");
        validateNonEmpty("not", not);
        for (var it : not) {
          filterRequestBuilder.addNot(mapSelectRequest(it));
        }
      }
      if (!filterInput.containsKey("select") && !filterInput.containsKey("not")) {
        throw new IllegalArgumentException("A filter must have at least one of 'select' or 'not'.");
      }
      filterRequests.add(filterRequestBuilder.build());
    }
    return filterRequests;
  }

  @SuppressWarnings("unchecked")
  private TripTimeOnDateSelectRequest mapSelectRequest(Map<String, List<?>> input) {
    var builder = TripTimeOnDateSelectRequest.of();

    if (input.containsKey("lines")) {
      var lines = (List<String>) input.get("lines");
      validateNonEmpty("lines", lines);
      builder.withRoutes(idMapper.parseListNullSafe(lines));
    }

    if (input.containsKey("authorities")) {
      var authorities = (List<String>) input.get("authorities");
      validateNonEmpty("authorities", authorities);
      builder.withAgencies(idMapper.parseListNullSafe(authorities));
    }

    if (input.containsKey("transportModes")) {
      var transportModes = (List<Map<String, ?>>) input.get("transportModes");
      validateNonEmpty("transportModes", transportModes);

      var tModes = new ArrayList<MainAndSubMode>();
      for (Map<String, ?> modeWithSubModes : transportModes) {
        var mainMode = (TransitMode) modeWithSubModes.get("transportMode");
        if (modeWithSubModes.containsKey("transportSubModes")) {
          var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubModes.get(
            "transportSubModes"
          );
          for (var subMode : transportSubModes) {
            tModes.add(new MainAndSubMode(mainMode, SubMode.of(subMode.getValue())));
          }
        } else {
          tModes.add(new MainAndSubMode(mainMode));
        }
      }
      builder.withTransportModes(tModes);
    }

    return builder.build();
  }

  private static void validateNonEmpty(String fieldName, List<?> values) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("'%s' cannot be an empty list".formatted(fieldName));
    }
  }
}
