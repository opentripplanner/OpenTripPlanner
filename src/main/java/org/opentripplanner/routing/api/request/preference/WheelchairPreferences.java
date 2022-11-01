package org.opentripplanner.routing.api.request.preference;

import java.util.Objects;
import org.opentripplanner.routing.api.request.framework.Units;

/**
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public record WheelchairPreferences(
  AccessibilityPreferences trip,
  AccessibilityPreferences stop,
  AccessibilityPreferences elevator,
  double inaccessibleStreetReluctance,
  double maxSlope,
  double slopeExceededReluctance,
  double stairsReluctance
) {
  /**
   * Default reluctance for traversing a street that is tagged with wheelchair=no. Since most
   * streets have no accessibility information, we don't have a separate cost for unknown.
   */
  private static final int DEFAULT_INACCESSIBLE_STREET_RELUCTANCE = 25;

  /**
   * ADA max wheelchair ramp slope is a good default.
   */
  private static final double DEFAULT_MAX_SLOPE = 0.083;

  private static final int DEFAULT_SLOPE_EXCEEDED_RELUCTANCE = 1;

  private static final int DEFAULT_STAIRS_RELUCTANCE = 100;

  /**
   * It's very common for elevators in OSM to have unknown wheelchair accessibility since they are
   * assumed to be so for that reason they only have a small default penalty for unknown
   * accessibility
   */
  private static final AccessibilityPreferences DEFAULT_ELEVATOR_FEATURE = AccessibilityPreferences.ofCost(
    20,
    3600
  );

  public static final WheelchairPreferences DEFAULT = new WheelchairPreferences(
    AccessibilityPreferences.ofOnlyAccessible(),
    AccessibilityPreferences.ofOnlyAccessible(),
    DEFAULT_ELEVATOR_FEATURE,
    DEFAULT_INACCESSIBLE_STREET_RELUCTANCE,
    DEFAULT_MAX_SLOPE,
    DEFAULT_SLOPE_EXCEEDED_RELUCTANCE,
    DEFAULT_STAIRS_RELUCTANCE
  );

  public WheelchairPreferences(
    AccessibilityPreferences trip,
    AccessibilityPreferences stop,
    AccessibilityPreferences elevator,
    double inaccessibleStreetReluctance,
    double maxSlope,
    double slopeExceededReluctance,
    double stairsReluctance
  ) {
    this.trip = Objects.requireNonNull(trip);
    this.stop = Objects.requireNonNull(stop);
    this.elevator = Objects.requireNonNull(elevator);
    this.inaccessibleStreetReluctance = Units.reluctance(inaccessibleStreetReluctance);
    this.maxSlope = Units.ratio(maxSlope);
    this.slopeExceededReluctance = Units.reluctance(slopeExceededReluctance);
    this.stairsReluctance = Units.reluctance(stairsReluctance);
  }
}
