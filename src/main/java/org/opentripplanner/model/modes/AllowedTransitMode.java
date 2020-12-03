package org.opentripplanner.model.modes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to filter out modes for routing requests. If both mainMode and subMode are specified, they
 * must match exactly. If subMode is set to null, that means that all possible subModes are accepted.
 * This class is separated from the TransitMode class because the meanings of the fields are slightly
 * different.
 */
public class AllowedTransitMode {

  private final TransitMainMode mainMode;

  private final String subMode;

  public static AllowedTransitMode fromMainModeEnum(TransitMainMode mainMode) {
    return new AllowedTransitMode(mainMode, null);
  }

  public AllowedTransitMode(TransitMainMode mainMode, String subMode) {
    this.mainMode = mainMode;
    this.subMode = subMode;
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  public boolean allows(TransitMode transitMode) {
    return mainMode == transitMode.getMainMode() && (
        subMode == null || subMode.equals(transitMode.getSubMode())
    );
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes.
   */
  public static Set<AllowedTransitMode> getAllTransitModes() {
    return Arrays
        .stream(TransitMainMode.values())
        .map(m -> new AllowedTransitMode(m,
            null
        ))
        .collect(Collectors.toSet());
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes except airplane.
   */
  public static Set<AllowedTransitMode> getAllTransitModesExceptAirplane() {
    return Arrays
        .stream(TransitMainMode.values())
        .filter(t -> !t.equals(TransitMainMode.AIRPLANE))
        .map(m -> new AllowedTransitMode(m,
            null
        ))
        .collect(Collectors.toSet());
  }
}
