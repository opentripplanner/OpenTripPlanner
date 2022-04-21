package org.opentripplanner.model.modes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.TransitMode;

/**
 * Used to filter out modes for routing requests. If both mainMode and subMode are specified, they
 * must match exactly. If subMode is set to null, that means that all possible subModes are
 * accepted. This class is separated from the TransitMode class because the meanings of the fields
 * are slightly different.
 */
public class AllowedTransitMode {

  private final TransitMode mainMode;

  private final String subMode;

  private static final String UNKNOWN = "unknown";

  public AllowedTransitMode(TransitMode mainMode, String subMode) {
    this.mainMode = mainMode;
    this.subMode = subMode;
  }

  public static AllowedTransitMode fromMainModeEnum(TransitMode mainMode) {
    return new AllowedTransitMode(mainMode, null);
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes.
   */
  public static Set<AllowedTransitMode> getAllTransitModes() {
    return Arrays
      .stream(TransitMode.values())
      .map(m -> new AllowedTransitMode(m, null))
      .collect(Collectors.toSet());
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes except airplane.
   */
  public static Set<AllowedTransitMode> getAllTransitModesExceptAirplane() {
    return TransitMode
      .transitModesExceptAirplane()
      .stream()
      .map(m -> new AllowedTransitMode(m, null))
      .collect(Collectors.toSet());
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  public boolean allows(TransitMode transitMode, String netexSubMode) {
    boolean mainModeMatch = mainMode == transitMode;
    boolean submodeMatch =
      subMode == null ||
      subMode.equals(netexSubMode) ||
      (UNKNOWN.equals(subMode) && netexSubMode == null);
    return mainModeMatch && submodeMatch;
  }

  public TransitMode getMainMode() {
    return mainMode;
  }

  /**
   * Is the sub-mode set for this main mode
   */
  public boolean hasSubMode() {
    return subMode != null;
  }
}
