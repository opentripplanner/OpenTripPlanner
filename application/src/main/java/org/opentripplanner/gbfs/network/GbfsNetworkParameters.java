package org.opentripplanner.gbfs.network;

import java.util.Objects;

/**
 * Behavior configured for a single GBFS network, shared by the vehicle rental graph builder and
 * the vehicle rental service directory.
 * <p>
 * The network name is not part of this record: it is the key these parameters are looked up by in
 * {@link GbfsNetworkOverrides}, and the same type describes the {@code defaults} block, which has
 * no network of its own.
 *
 * @param geofencingZoneScope the scope of this network's geofencing zones, i.e. which phase
 *   computes and applies them. Configured as {@code applyGeofencingZones}.
 * @param requireDropOffInsideBusinessArea whether a rented vehicle must be dropped off before
 *   leaving the operator's business area, i.e. whether the router forces a drop-off at its border.
 *   Has no effect when {@code geofencingZoneScope} is {@link GeofencingZoneScope#OFF}.
 * @param allowKeepingVehicleAtDestination whether a vehicle rented from a station may be kept at
 *   the destination rather than returned to another station
 */
public record GbfsNetworkParameters(
  GeofencingZoneScope geofencingZoneScope,
  boolean requireDropOffInsideBusinessArea,
  boolean allowKeepingVehicleAtDestination
) {
  /**
   * The values used when neither a {@code defaults} block nor a network entry specifies a field.
   */
  public static final GbfsNetworkParameters DEFAULT = new GbfsNetworkParameters(
    GeofencingZoneScope.OFF,
    true,
    false
  );

  public GbfsNetworkParameters {
    Objects.requireNonNull(geofencingZoneScope);
  }
}
