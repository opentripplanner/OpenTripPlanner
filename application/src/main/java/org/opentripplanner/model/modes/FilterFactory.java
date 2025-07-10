package org.opentripplanner.model.modes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * This utility merges a set of MainMode and MainAndSubMode Filters into a more optimized
 * set of filters using an EnumSet for MainModes and BitSet for subModes. For further
 * details on the implementation see the comments in the code.
 */
class FilterFactory {

  private static final AllowAllModesFilter ALLOWED_ALL_TRANSIT_MODES = new AllowAllModesFilter();

  /** Utility class, prevent instantiation */
  private FilterFactory() {}

  static AllowTransitModeFilter of(MainAndSubMode mode) {
    if (mode.subMode() == null) {
      return new AllowMainModeFilter(mode.mainMode());
    } else {
      return new AllowMainAndSubModeFilter(mode);
    }
  }

  /**
   * Merge a set of:
   * <ul>
   *   <li>{@link AllowMainModeFilter} and
   *   <li>{@link AllowMainAndSubModeFilter}
   * </ul>
   */
  static AllowTransitModeFilter create(Collection<MainAndSubMode> allowedModes) {
    var filters = allowedModes.stream().distinct().map(FilterFactory::of).toList();

    if (filters.isEmpty()) {
      throw new IllegalArgumentException("Can not match an empty set of modes!");
    }

    if (filters.size() == 1) {
      return filters.get(0);
    }

    Map<Class<?>, List<AllowTransitModeFilter>> map = filters
      .stream()
      .collect(Collectors.groupingBy(Object::getClass));

    if (map.containsKey(AllowAllModesFilter.class)) {
      return ALLOWED_ALL_TRANSIT_MODES;
    }

    // We uses a list here not a set to preserve the order. Duplicates are removed before
    // returning the results - preserving the order.
    var result = new ArrayList<AllowTransitModeFilter>();
    var mainModeFilters = stream(map, AllowMainModeFilter.class).collect(Collectors.toSet());

    if (mainModeFilters.size() == TransitMode.values().length) {
      return ALLOWED_ALL_TRANSIT_MODES;
    } else if (mainModeFilters.size() == 1) {
      result.addAll(mainModeFilters);
    } else if (mainModeFilters.size() > 1) {
      result.add(new AllowMainModesFilter(mainModeFilters));
    }
    // else ignore empty list

    var subModeFiltersByMainMode = stream(map, AllowMainAndSubModeFilter.class).collect(
      Collectors.groupingBy(AllowMainAndSubModeFilter::mainMode)
    );

    var mainModes = mainModeFilters.isEmpty()
      ? EnumSet.noneOf(TransitMode.class)
      : EnumSet.copyOf(mainModeFilters.stream().map(AllowMainModeFilter::mainMode).toList());

    for (TransitMode mainMode : subModeFiltersByMainMode.keySet()) {
      // Skip sub-mode filters if a main mode filter exist (all main modes are accepted)
      if (mainModes.contains(mainMode)) {
        continue;
      }

      var subModeFilters = subModeFiltersByMainMode.get(mainMode);

      if (subModeFilters.size() == 1) {
        result.addAll(subModeFilters);
      } else if (subModeFilters.size() > 1) {
        result.add(new AllowMainAndSubModesFilter(subModeFilters));
      }
      // esle ignore empty
    }

    if (result.size() == 1) {
      return result.get(0);
    }

    // Remove duplicates and preserve order
    return new FilterCollection(result.stream().distinct().toList());
  }

  @SuppressWarnings("unchecked")
  private static <T> Stream<T> stream(
    Map<Class<?>, List<AllowTransitModeFilter>> map,
    Class<T> type
  ) {
    var list = map.get(type);
    return list == null ? Stream.empty() : list.stream().map(it -> (T) it);
  }
}
