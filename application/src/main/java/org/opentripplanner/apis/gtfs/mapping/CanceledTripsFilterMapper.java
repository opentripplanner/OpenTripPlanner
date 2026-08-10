package org.opentripplanner.apis.gtfs.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TripOnServiceDateSelectRequest;
import org.opentripplanner.utils.collection.CollectionUtils;

public class CanceledTripsFilterMapper {

  public static TripOnServiceDateRequest mapToTripOnServiceDateRequest(
    DataFetchingEnvironment env
  ) {
    var filters = mapList(env.getArgument("filters"), "filters");
    if (CollectionUtils.isEmpty(filters)) {
      return TripOnServiceDateRequest.of().build();
    }

    var filterRequests = filters.stream().map(CanceledTripsFilterMapper::toFilterRequest).toList();
    return TripOnServiceDateRequest.of().withFilters(filterRequests).build();
  }

  private static FilterRequest<TripOnServiceDateSelectRequest> toFilterRequest(
    Map<String, Object> filter
  ) {
    var includes = mapList(filter.get("include"), "filters.include");
    var excludes = mapList(filter.get("exclude"), "filters.exclude");
    CollectionUtils.requireNullOrNonEmpty(includes, "filters.include");
    CollectionUtils.requireNullOrNonEmpty(excludes, "filters.exclude");

    var filterRequestBuilder = FilterRequest.<TripOnServiceDateSelectRequest>of();
    var includeSelect = toSelectRequest(includes);
    if (includeSelect != null) {
      filterRequestBuilder.addSelect(includeSelect);
    }
    var excludeSelect = toSelectRequest(excludes);
    if (excludeSelect != null) {
      filterRequestBuilder.addNot(excludeSelect);
    }
    return filterRequestBuilder.build();
  }

  @Nullable
  private static TripOnServiceDateSelectRequest toSelectRequest(List<Map<String, Object>> inputs) {
    var modes = toTransitModes(inputs);
    var serviceDateRanges = toServiceDateRanges(inputs);
    if (modes == null && serviceDateRanges == null) {
      return null;
    }

    var builder = TripOnServiceDateSelectRequest.of();
    if (modes != null) {
      builder.withTransportModes(MainAndSubMode.ofTransitModes(modes));
    }
    if (serviceDateRanges != null) {
      builder.withServiceDateRanges(serviceDateRanges);
    }
    return builder.build();
  }

  @Nullable
  private static Set<TransitMode> toTransitModes(List<Map<String, Object>> inputs) {
    if (inputs == null) {
      return null;
    }
    if (
      inputs
        .stream()
        .anyMatch(
          input ->
            list(input.get("modes"), "filters.*.modes") != null &&
            list(input.get("modes"), "filters.*.modes").isEmpty()
        )
    ) {
      throw new InvalidInputException(
        "Mode filter must be either null or have at least one entry."
      );
    }
    var modes = inputs
      .stream()
      .flatMap(include -> {
        var modeValues = list(include.get("modes"), "filters.*.modes");
        if (modeValues == null) {
          return Stream.of();
        }
        return modeValues.stream().map(CanceledTripsFilterMapper::mapTransitMode);
      })
      .collect(Collectors.toSet());

    return modes.isEmpty() ? null : modes;
  }

  @Nullable
  private static List<LocalDateRange> toServiceDateRanges(List<Map<String, Object>> inputs) {
    if (inputs == null) {
      return null;
    }
    if (
      inputs
        .stream()
        .anyMatch(
          input ->
            mapList(input.get("serviceDateRanges"), "filters.*.serviceDateRanges") != null &&
            mapList(input.get("serviceDateRanges"), "filters.*.serviceDateRanges").isEmpty()
        )
    ) {
      throw new InvalidInputException(
        "Service date range filter must be either null or have at least one entry."
      );
    }

    var ranges = inputs
      .stream()
      .flatMap(input -> {
        var rangeInputs = mapList(input.get("serviceDateRanges"), "filters.*.serviceDateRanges");
        if (rangeInputs == null) {
          return Stream.of();
        }
        return rangeInputs.stream().map(CanceledTripsFilterMapper::mapLocalDateRange);
      })
      .toList();

    return ranges.isEmpty() ? null : ranges;
  }

  private static LocalDateRange mapLocalDateRange(Map<String, Object> rangeInput) {
    return LocalDateRange.ofExclusiveEnd(
      localDate(rangeInput.get("start"), "filters.*.serviceDateRanges.start"),
      localDate(rangeInput.get("end"), "filters.*.serviceDateRanges.end")
    );
  }

  private static TransitMode mapTransitMode(Object value) {
    var mode = switch (value) {
      case Enum<?> enumValue -> enumValue.name();
      case String stringValue -> stringValue;
      default -> throw new InvalidInputException("Invalid mode value: " + value);
    };
    try {
      return TransitMode.valueOf(mode);
    } catch (IllegalArgumentException e) {
      throw new InvalidInputException("Unknown transit mode: " + mode);
    }
  }

  @Nullable
  private static LocalDate localDate(@Nullable Object value, String path) {
    if (value == null) {
      return null;
    }
    return switch (value) {
      case LocalDate localDate -> localDate;
      case String stringValue -> LocalDate.parse(stringValue);
      default -> throw new InvalidInputException("Expected LocalDate at '" + path + "'.");
    };
  }

  @Nullable
  private static List<Object> list(@Nullable Object value, String path) {
    if (value == null) {
      return null;
    }
    if (value instanceof List<?> list) {
      return new ArrayList<>(list);
    }
    throw new InvalidInputException("Expected list at '" + path + "'.");
  }

  @Nullable
  private static List<Map<String, Object>> mapList(@Nullable Object value, String path) {
    var list = list(value, path);
    if (list == null) {
      return null;
    }
    var result = new ArrayList<Map<String, Object>>(list.size());
    for (var item : list) {
      if (!(item instanceof Map<?, ?> map)) {
        throw new InvalidInputException("Expected object entries in list at '" + path + "'.");
      }
      result.add(stringObjectMap(map, path));
    }
    return result;
  }

  private static Map<String, Object> stringObjectMap(Map<?, ?> source, String path) {
    var result = new LinkedHashMap<String, Object>(source.size());
    for (var entry : source.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new InvalidInputException("Expected string keys in object at '" + path + "'.");
      }
      result.put(key, entry.getValue());
    }
    return result;
  }
}
