package org.opentripplanner.transit.transfer.regular.api.maxduration;

/**
 * A mode of transport a stop can be allowed for, because trips that allow the mode visit it. It is
 * used to give the transfers between such stops a longer max duration, see
 * {@link MaxDurationsPreference}. For example, a ferry that carries cars and bicycles makes its
 * stops {@link #CAR} and {@link #BICYCLE} allowed.
 */
public enum AllowedMode {
  CAR,
  BICYCLE,
}
