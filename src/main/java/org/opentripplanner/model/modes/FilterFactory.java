package org.opentripplanner.model.modes;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.transit.model.network.MainAndSubMode;
import org.opentripplanner.transit.model.network.TransitMode;

/**
 * This utility merge a set of MainMode and MainAndSubMode Filters into a more optimized
 * set of filters using an EnumSet for MainModes and BitSet for subModes. For further
 * details on the implementation se the comments in the code.
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
  static Set<AllowTransitModeFilter> create(Collection<MainAndSubMode> allowedModes) {
    var filters = allowedModes.stream().map(FilterFactory::of).toList();

    if (filters.isEmpty()) {
      throw new IllegalArgumentException("Can not match an empty set of modes!");
    }

    if (filters.size() == 1) {
      return Set.copyOf(filters);
    }

    Map<Class<?>, List<AllowTransitModeFilter>> map = filters
      .stream()
      .collect(Collectors.groupingBy(Object::getClass));

    if (map.containsKey(AllowAllModesFilter.class)) {
      return Set.of(ALLOWED_ALL_TRANSIT_MODES);
    }

    var result = new HashSet<AllowTransitModeFilter>();
    var mainModeFilters = stream(map, AllowMainModeFilter.class).collect(Collectors.toSet());

    // All main modes are included
    if (mainModeFilters.size() == TransitMode.values().length) {
      return Set.of(ALLOWED_ALL_TRANSIT_MODES);
    }
    // Merge filters if there are more than 2 mainMode filters
    else if (mainModeFilters.size() > 2) {
      result.add(new AllowMainModesFilter(mainModeFilters));
    }
    // It there is just 1 or 2 main-mode filters, then add them. Iterating over 2 filter is
    // probably as fast as looking up 2 enums in an EnumSet.
    else if (mainModeFilters.size() > 0) {
      result.addAll(mainModeFilters);
    }

    var subModeFiltersByMainMode = stream(map, AllowMainAndSubModeFilter.class)
      .collect(Collectors.groupingBy(AllowMainAndSubModeFilter::mainMode));

    var mainModes = mainModeFilters.isEmpty()
      ? EnumSet.noneOf(TransitMode.class)
      : EnumSet.copyOf(mainModeFilters.stream().map(AllowMainModeFilter::mainMode).toList());

    for (TransitMode mainMode : subModeFiltersByMainMode.keySet()) {
      // Skip sub-mode filters if a main mode filter exist (all main modes are accepted)
      if (mainModes.contains(mainMode)) {
        continue;
      }

      var subModeFilters = subModeFiltersByMainMode.get(mainMode);

      // If there are more than 2 subMode filters for the same main mode, merge them together
      if (subModeFilters.size() > 2) {
        result.add(new AllowMainAndSubModesFilter(subModeFilters));
      }
      // It there is just 1 or 2 sub-mode filters, then add them. Iterating over 2 filter is
      // probably as fast as looking up 2 indexes in a bitset.
      else if (subModeFilters.size() > 0) {
        result.addAll(subModeFilters);
      }
    }
    return result;
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
