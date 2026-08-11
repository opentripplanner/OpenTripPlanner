package org.opentripplanner.gbfs.network;

/**
 * Which phase computes and applies a network's geofencing zones.
 * <p>
 * The vocabulary mirrors {@link org.opentripplanner.street.Scope}, which OTP already defines for
 * this distinction: {@code REALTIME} changes to the street graph are done by updaters. This is a
 * separate enum rather than {@code Scope} itself because most of {@code Scope} is meaningless here.
 */
public enum GeofencingZonePhase {
  /** Zones are loaded and applied by the vehicle rental updater. */
  REALTIME,
  /** Zones are not processed for this network. */
  OFF,
}
