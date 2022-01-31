package org.opentripplanner.model.modes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.TransitMode;

/**
 * Used to filter out modes for routing requests. If both mainMode and subMode are specified, they
 * must match exactly. If subMode is set to null, that means that all possible subModes are accepted.
 * This class is separated from the TransitMode class because the meanings of the fields are slightly
 * different.
 */
public class AllowedTransitMode {

  public static enum FilterType {
    LINE,
    SERVICE_JOURNEY;
  }

  private final FilterType filterType;

  private final TransitMode mainMode;

  private final String subMode;

  public static AllowedTransitMode fromMainModeEnum(TransitMode mainMode) {
    return new AllowedTransitMode(mainMode, null, FilterType.LINE);
  }

  public static AllowedTransitMode fromMainModeEnum(TransitMode mainMode, FilterType filterType) {
    return new AllowedTransitMode(mainMode, null, filterType);
  }

  public AllowedTransitMode(TransitMode mainMode, String subMode, FilterType filterType) {
    this.mainMode = mainMode;
    this.subMode = subMode;
    this.filterType = filterType;
  }

  /**
   * Check if this filter allows the provided TransitMode and NeTEx submode
   */
  public boolean allows(TransitMode transitMode, String netexSubMode) {
    return mainMode == transitMode && (
        subMode == null || subMode.equals(netexSubMode)
    );
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  public boolean allowsMode(TransitMode transitMode) {
    return mainMode == transitMode;
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes.
   */
  public static Set<AllowedTransitMode> getAllTransitModes() {
    return Arrays
        .stream(TransitMode.values())
        .map(m -> new AllowedTransitMode(m, null, FilterType.LINE))
        .collect(Collectors.toSet());
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes except airplane.
   */
  public static Set<AllowedTransitMode> getAllTransitModesExceptAirplane() {
    return TransitMode.transitModesExceptAirplane().stream()
        .map(m -> new AllowedTransitMode(m, null, FilterType.LINE))
        .collect(Collectors.toSet());
  }

  public FilterType getFilterType() {
    return filterType;
  }
}
