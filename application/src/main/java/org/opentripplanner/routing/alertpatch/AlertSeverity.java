package org.opentripplanner.routing.alertpatch;

/**
 * An enum containing severity options for an alert.
 */
public enum AlertSeverity {
  /**
   * Info alerts are used for informational messages that should not have a significant effect on
   * user's journey, for example: A single entrance to a metro station is temporarily closed.
   */
  INFO,
  /**
   * Situation has unknown impact on trips.
   */
  UNKNOWN_SEVERITY,
  /**
   * Situation has a very slight impact on trips.
   */
  VERY_SLIGHT,
  /**
   * Situation has a slight impact on trips.
   */
  SLIGHT,
  /**
   * Warning alerts are used when a single stop or route has a disruption that can affect user's
   * journey, for example: All trams on a specific route are running with irregular schedules.
   */
  WARNING,
  /**
   * Severe alerts are used when a significant part of public transport services is affected, for
   * example: All train services are cancelled due to technical problems.
   */
  SEVERE,
  /**
   * Situation has a very severe impact on trips.
   */
  VERY_SEVERE;

  /**
   * Return the sorting index for severity ordering.
   *
   * <p>This index is based on the enum declaration order via {@link #ordinal()}. A lower index
   * means lower severity when sorting.
   */
  public int sortingIndex() {
    return ordinal();
  }
}
