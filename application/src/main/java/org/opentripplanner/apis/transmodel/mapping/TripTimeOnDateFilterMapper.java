package org.opentripplanner.apis.transmodel.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateSelectRequest;

/**
 * Maps GraphQL {@code EstimatedCallFilterInput} to a list of {@link FilterRequest} objects by
 * extracting the {@code select} and {@code not} criteria from the GraphQL input.
 * <p>
 * Each filter has {@code select} and {@code not} arrays of select criteria.
 * <p>
 * Empty lists are not allowed and will result in an {@link InvalidInputException}. A list field
 * that is an explicit {@code null} is treated as if it was omitted.
 */
public class TripTimeOnDateFilterMapper {

  private final FeedScopedIdMapper idMapper;

  public TripTimeOnDateFilterMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  @SuppressWarnings("unchecked")
  public List<FilterRequest<TripTimeOnDateSelectRequest>> mapFilters(List<Map<String, ?>> filters) {
    validateFieldNotEmpty("filters", filters);

    var filterRequests = new ArrayList<FilterRequest<TripTimeOnDateSelectRequest>>();

    for (var filterInput : filters) {
      var select = (List<Map<String, List<?>>>) filterInput.get("select");
      var not = (List<Map<String, List<?>>>) filterInput.get("not");
      if (select == null && not == null) {
        throw new InvalidInputException("A filter must have at least one of 'select' or 'not'.");
      }

      var filterRequestBuilder = FilterRequest.<TripTimeOnDateSelectRequest>of();

      if (select != null) {
        validateFieldNotEmpty("select", select);
        for (var it : select) {
          filterRequestBuilder.addSelect(mapSelectRequest(it));
        }
      }
      if (not != null) {
        validateFieldNotEmpty("not", not);
        for (var it : not) {
          filterRequestBuilder.addNot(mapSelectRequest(it));
        }
      }
      filterRequests.add(filterRequestBuilder.build());
    }
    return filterRequests;
  }

  @SuppressWarnings("unchecked")
  private TripTimeOnDateSelectRequest mapSelectRequest(Map<String, List<?>> input) {
    validateNotEmpty(input);

    var builder = TripTimeOnDateSelectRequest.of();

    var lines = (List<String>) input.get("lines");
    if (lines != null) {
      validateFieldNotEmpty("lines", lines);
      builder.withRoutes(idMapper.parseListNullSafe(lines));
    }

    var authorities = (List<String>) input.get("authorities");
    if (authorities != null) {
      validateFieldNotEmpty("authorities", authorities);
      builder.withAgencies(idMapper.parseListNullSafe(authorities));
    }

    var transportModes = (List<Map<String, ?>>) input.get("transportModes");
    if (transportModes != null) {
      validateFieldNotEmpty("transportModes", transportModes);

      var tModes = new ArrayList<MainAndSubMode>();
      for (Map<String, ?> modeWithSubModes : transportModes) {
        validateMainModePresent(modeWithSubModes);

        var mainMode = TransportModeInputMapper.mapTransitMode(
          modeWithSubModes.get("transportMode")
        );
        var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubModes.get(
          "transportSubModes"
        );
        if (transportSubModes != null) {
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

  private static void validateNotEmpty(Map<String, List<?>> input) {
    if (
      input.get("lines") == null &&
      input.get("authorities") == null &&
      input.get("transportModes") == null
    ) {
      throw new InvalidInputException("A selector cannot be empty");
    }
  }

  private static void validateFieldNotEmpty(String fieldName, List<?> values) {
    if (values.isEmpty()) {
      throw new InvalidInputException("'%s' cannot be an empty list".formatted(fieldName));
    }
  }

  private static void validateMainModePresent(Map<String, ?> transportModeWithSubModes) {
    if (!transportModeWithSubModes.containsKey("transportMode")) {
      throw new InvalidInputException("'transportMode' is required in a transport mode selector.");
    }
  }
}
