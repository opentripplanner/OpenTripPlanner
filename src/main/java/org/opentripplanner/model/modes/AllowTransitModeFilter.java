package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.network.SubMode;
import org.opentripplanner.transit.model.network.TransitMode;

/**
 * Used to filter out modes for routing requests.
 */
public interface AllowTransitModeFilter extends Serializable {
  AllowAllModesFilter ALLOWED_ALL_TRANSIT_MODES = new AllowAllModesFilter();

  /**
   * Create a filter based on the given mainMode and subMode.
   * <ol>
   *   <li>If both main- and sub-mode is {@code null} all trips are matched.
   *   <li>If main-mode is set and sub-mode is {@code null} a filter matching the main mode is created.
   *   <li>If main-mode and sub-mode set, both main- and sub-mode must match.
   *   <li>If main-mode is {@code null} and sub-mode set a {@link IllegalArgumentException} is thrown.
   * </ol>
   */
  static AllowTransitModeFilter of(TransitMode mainMode, String subMode) {
    if (subMode == null) {
      if (mainMode == null) {
        return ALLOWED_ALL_TRANSIT_MODES;
      }
      return new AllowMainModeFilter(mainMode);
    }
    if (mainMode == null) {
      throw new IllegalArgumentException("Main mode is required with subMode: " + subMode);
    }
    return new AllowMainAndSubModeFilter(mainMode, SubMode.of(subMode));
  }

  /**
   * Create a filters based on the given mainModes.
   */
  static Set<AllowTransitModeFilter> ofMainModes(@Nonnull TransitMode... mainModes) {
    return Arrays.stream(mainModes).map(AllowMainModeFilter::new).collect(Collectors.toSet());
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes.
   */
  static Set<AllowTransitModeFilter> ofAllTransitModes() {
    return Set.of(ALLOWED_ALL_TRANSIT_MODES);
  }

  /** See {@link FilterMerger} */
  static Set<AllowTransitModeFilter> merge(Collection<AllowTransitModeFilter> filters) {
    return FilterMerger.merge(filters);
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  boolean allows(TransitMode transitMode, SubMode netexSubMode);
}
