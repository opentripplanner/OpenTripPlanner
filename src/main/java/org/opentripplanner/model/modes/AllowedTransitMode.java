package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Set;
import org.opentripplanner.transit.model.network.TransitMode;

/**
 * Used to filter out modes for routing requests. If both mainMode and subMode are specified, they
 * must match exactly. If subMode is set to null, that means that all possible subModes are
 * accepted. This class is separated from the TransitMode class because the meanings of the fields
 * are slightly different.
 */
public interface AllowedTransitMode extends Serializable {
  AllowedAllTransitModes ALLOWED_ALL_TRANSIT_MODES = new AllowedAllTransitModes();

  static AllowedTransitMode fromMainModeEnum(TransitMode mainMode) {
    return new AllowedMainTransitMode(mainMode);
  }

  static AllowedTransitMode of(TransitMode mainMode, String subMode) {
    if (subMode == null) {
      return of(mainMode);
    }
    if (mainMode == null) {
      throw new IllegalArgumentException("Main mode is requiered with submode: " + subMode);
    }
    return new AllowedMainAndSubTransitMode(mainMode, subMode);
  }

  static AllowedTransitMode of(TransitMode mainMode) {
    if (mainMode == null) {
      return ALLOWED_ALL_TRANSIT_MODES;
    }
    return new AllowedMainTransitMode(mainMode);
  }

  /**
   * Returns a matcher for trips matching the given main mode with no submode set. If main mode is
   * BUS, all busses without any sub-mode is included.
   */
  static AllowedTransitMode ofUnknownSubModes(TransitMode mainMode) {
    return new AllowedUnknownSubTransitMode(mainMode);
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes.
   */
  static Set<AllowedTransitMode> ofAllTransitModes() {
    return Set.of(ALLOWED_ALL_TRANSIT_MODES);
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  boolean allows(TransitMode transitMode, String netexSubMode);
}
