package org.opentripplanner.service.vehiclerental.street.geofencing;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.search.state.State;

/**
 * Strategy for enforcing geofencing zone restrictions during routing. Strategies override only the
 * positions they care about; defaults return {@code null}.
 *
 * <h3>Boundary structure</h3>
 *
 * <p>
 * A zone boundary is a set of street edges. Each boundary edge has one endpoint inside the zone
 * (the inside vertex) and one outside (the outside vertex).
 *
 * <h3>Approaching vs crossing</h3>
 *
 * <p>
 * For each side of the boundary, a state can be in one of two positions: about to traverse the
 * boundary edge ("approaching" — current edge ends at a boundary vertex, next edge will cross), or
 * having just traversed it ("crossing" — current edge crossed). Fork-and-drop decisions happen at
 * the approaching position because the fork's branches inherit the state's vertex:
 *
 * <ul>
 *   <li>A drop event in the rendered itinerary lands on the back-edge of the renting state. Forking
 *       at the crossing position would place the drop on the boundary edge itself — on the wrong side.
 *       Approaching keeps the drop entirely outside.</li>
 *   <li>The ride branch continues exploration from its vertex. Forking at the crossing position means
 *       exploration starts already inside the zone.</li>
 * </ul>
 */
sealed interface GeofencingBoundaryEnforcement
  permits BusinessAreaEnforcement, RestrictedZoneEnforcement
{
  /** Forward search: current edge ends at the outside vertex; next edge will enter the zone. */
  default State @Nullable [] forwardApproachingEntry(
    GeofencingZone zone,
    State state,
    EdgeTraversal edge
  ) {
    return null;
  }

  /** Forward search: current edge ends at the inside vertex; next edge will exit the zone. */
  default State @Nullable [] forwardApproachingExit(
    GeofencingZone zone,
    State state,
    EdgeTraversal edge
  ) {
    return null;
  }

  /** Forward search: current edge crosses outward (from the inside vertex to the outside). */
  default State @Nullable [] forwardCrossingExit(
    GeofencingZone zone,
    State state,
    EdgeTraversal edge
  ) {
    return null;
  }

  /** ArriveBy search: current edge crosses outward (from the inside vertex to the outside). */
  default State @Nullable [] arriveByCrossingExit(
    GeofencingZone zone,
    State state,
    EdgeTraversal edge
  ) {
    return null;
  }

  /**
   * ArriveBy search at a paired boundary on the rider's forward path between drop point and
   * destination. Direction varies by zone type — see {@link WalkerBoundaryHandler}.
   */
  default State @Nullable [] arriveByAtBoundary(
    GeofencingZone zone,
    State state,
    EdgeTraversal edge
  ) {
    return null;
  }

  /** Resolve the enforcement implementation for a zone. */
  static GeofencingBoundaryEnforcement forZone(GeofencingZone zone) {
    if (zone.isBusinessArea()) {
      return BusinessAreaEnforcement.INSTANCE;
    }
    return RestrictedZoneEnforcement.INSTANCE;
  }
}
