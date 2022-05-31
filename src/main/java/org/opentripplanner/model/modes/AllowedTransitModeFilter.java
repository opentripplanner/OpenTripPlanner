package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Set;
import org.opentripplanner.transit.model.network.TransitMode;

/**
 * Used to filter out modes for routing requests.
 */
public interface AllowedTransitModeFilter extends Serializable {
  AllowedFilterAllTransitModes ALLOWED_ALL_TRANSIT_MODES = new AllowedFilterAllTransitModes();

  static AllowedTransitModeFilter fromMainModeEnum(TransitMode mainMode) {
    return new AllowedMainTransitModeFilter(mainMode);
  }

  /**
   * Create a filter based on the given mainMode and subMode.
   * <ol>
   *   <li>If both main- and sub-mode is {@code null} all trips are matched.
   *   <li>If main-mode is set and sub-mode is {@code null} a filter matching the main mode is created.
   *   <li>If main-mode and sub-mode set, both main- and sub-mode must match.
   *   <li>If main-mode is {@code null} and sub-mode set a {@link IllegalArgumentException} is thrown.
   * </ol>
   */
  static AllowedTransitModeFilter of(TransitMode mainMode, String subMode) {
    if (subMode == null) {
      return of(mainMode);
    }
    if (mainMode == null) {
      throw new IllegalArgumentException("Main mode is required with subMode: " + subMode);
    }
    return new AllowedMainAndSubTransitModeFilter(mainMode, subMode);
  }

  /**
   * Same as {@code #of(TransitMode, null)} (no sub-mode)
   */
  static AllowedTransitModeFilter of(TransitMode mainMode) {
    if (mainMode == null) {
      return ALLOWED_ALL_TRANSIT_MODES;
    }
    return new AllowedMainTransitModeFilter(mainMode);
  }

  /**
   * Returns a filter for trips matching trips the given main-mode with no sub-mode set. If
   * main-mode is BUS, all busses without any sub-mode is included.
   */
  static AllowedTransitModeFilter ofUnknownSubModes(TransitMode mainMode) {
    return new AllowedUnknownSubTransitModeFilter(mainMode);
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes.
   */
  static Set<AllowedTransitModeFilter> ofAllTransitModes() {
    return Set.of(ALLOWED_ALL_TRANSIT_MODES);
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  boolean allows(TransitMode transitMode, String netexSubMode);
}
