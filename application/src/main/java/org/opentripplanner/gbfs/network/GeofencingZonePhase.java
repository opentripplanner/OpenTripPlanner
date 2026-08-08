package org.opentripplanner.gbfs.network;

/**
 * Which phase computes and applies a network's geofencing zones.
 * <p>
 * The vocabulary mirrors {@link org.opentripplanner.street.Scope}, which OTP already defines for
 * this distinction: {@code PERMANENT} changes to the street graph are done during graph building,
 * {@code REALTIME} changes are done by updaters. This is a separate enum rather than {@code Scope}
 * itself because {@code Scope.REQUEST} is meaningless here.
 * <p>
 * The two phases are mutually exclusive by construction, so a network cannot have its zones
 * applied twice.
 */
public enum GeofencingZonePhase {
  /** Zones are loaded and applied by the vehicle rental graph builder at graph build time. */
  PERMANENT,
  /** Zones are loaded and applied by the vehicle rental updater at runtime. */
  REALTIME,
  /** Zones are not processed for this network. */
  OFF,
}
