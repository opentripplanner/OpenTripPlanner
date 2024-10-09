package org.opentripplanner.transit.model.basic;

/**
 * Use this class to describe if a feature is accessible {@code POSSIBLE}, not accessible
 * {@code NOT_POSSIBLE} or if we do not know {@code NO_INFORMATION}.
 * <p>
 * This can be used during routing to add extra cost or prune the search, depending on the
 * user preferences.
 */
public enum Accessibility {
  POSSIBLE,
  NOT_POSSIBLE,
  NO_INFORMATION,
}
