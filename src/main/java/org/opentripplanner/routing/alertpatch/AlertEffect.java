package org.opentripplanner.routing.alertpatch;

/**
 * An enum containing effect options for an alert.
 */
public enum AlertEffect {
  /**
   * No service.
   */
  NO_SERVICE,
  /**
   * Reduced service.
   */
  REDUCED_SERVICE,
  /**
   * Significant delays.
   */
  SIGNIFICANT_DELAYS,
  /**
   * Detour.
   */
  DETOUR,
  /**
   * Additional service.
   */
  ADDITIONAL_SERVICE,
  /**
   * Modified service.
   */
  MODIFIED_SERVICE,
  /**
   * Other effect than the available options.
   */
  OTHER_EFFECT,
  /**
   * Unknown effect.
   */
  UNKNOWN_EFFECT,
  /**
   * Stop moved.
   */
  STOP_MOVED,
  /**
   * No effect.
   */
  NO_EFFECT,
  /**
   * Accessibility issue.
   */
  ACCESSIBILITY_ISSUE,
}
