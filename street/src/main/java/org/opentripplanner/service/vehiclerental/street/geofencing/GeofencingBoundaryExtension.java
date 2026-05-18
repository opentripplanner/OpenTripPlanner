package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * Marks a vertex on the boundary of a geofencing zone. This is a data marker used by state-based
 * zone tracking in the routing algorithm — it does not enforce any restrictions itself. Applied to
 * both vertices of a boundary-crossing edge with opposite entering flags.
 *
 * @param zone the geofencing zone whose boundary this vertex lies on
 * @param entering true if traversing away from this vertex enters the zone; false if it exits
 */
public record GeofencingBoundaryExtension(GeofencingZone zone, boolean entering) {
  /**
   * Whether any boundary in the list marks an entry into a no-traversal zone for the given
   * network. Used by {@code VehicleRentalEdge} to fork at pickup on boundary vertices.
   */
  public static boolean hasNoTraversalEntry(
    List<GeofencingBoundaryExtension> boundaries,
    String network
  ) {
    if (boundaries.isEmpty() || network == null) {
      return false;
    }
    for (var boundary : boundaries) {
      if (!boundary.entering()) {
        continue;
      }
      if (!boundary.zone().id().getFeedId().equals(network)) {
        continue;
      }
      if (Boolean.TRUE.equals(boundary.zone().traversalBanned())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Resolve zone transitions for a boundary-crossing edge. For each boundary on fromVertex,
   * checks if toVertex has a paired boundary (same zone, opposite entering flag). Paired
   * boundaries indicate the edge crosses a zone boundary, adding or removing the zone from
   * the current set. The arriveBy flag is XOR'd with the entering flag to handle reverse
   * searches.
   *
   * @return the updated zone set, or {@code null} if no transitions occurred (no allocation)
   */
  @Nullable
  public static Set<GeofencingZone> resolveZoneTransitions(
    List<GeofencingBoundaryExtension> fromBoundaries,
    List<GeofencingBoundaryExtension> toBoundaries,
    Set<GeofencingZone> currentZones,
    boolean arriveBy
  ) {
    HashSet<GeofencingZone> newZones = null;

    for (var boundary : fromBoundaries) {
      boolean paired = false;
      for (var toBoundary : toBoundaries) {
        if (
          toBoundary.zone().equals(boundary.zone()) && toBoundary.entering() != boundary.entering()
        ) {
          paired = true;
          break;
        }
      }
      if (!paired) {
        continue;
      }
      if (newZones == null) {
        newZones = new HashSet<>(currentZones);
      }
      boolean effectiveEntering = boundary.entering() ^ arriveBy;
      if (effectiveEntering) {
        newZones.add(boundary.zone());
      } else {
        newZones.remove(boundary.zone());
      }
    }

    return newZones != null ? Set.copyOf(newZones) : null;
  }
}
