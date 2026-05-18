package org.opentripplanner.service.vehiclerental.street.geofencing;

import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.search.state.State;

/**
 * Strategy for enforcing geofencing zone restrictions during street routing. Each zone type
 * (restricted zone, business area) has its own implementation with zone-type-specific logic
 * for fork, block, and drop decisions.
 *
 * <p>Implementations are stateless singletons — all context comes through {@link #evaluate}.
 * The zone is not stored on the enforcement; zone properties needed for evaluation are passed
 * via the zone parameter or queried from the state's zone set.
 *
 * @see RestrictedZoneEnforcement
 * @see BusinessAreaEnforcement
 */
interface GeofencingEnforcement {
  /**
   * Evaluate what should happen when a renting state encounters this zone's boundary.
   * Returns the resulting State[] directly — the enforcement is both decision-maker and
   * state-builder. Returns null to indicate no enforcement action (pass through to normal
   * traversal).
   *
   * @param zone     the geofencing zone at this boundary
   * @param entering true if the state is entering the zone (in geographic/forward terms,
   *                 already adjusted for arriveBy via XOR)
   * @param arriveBy true if this is an arrive-by search
   * @param state    the current routing state
   * @param edge     edge traversal function — traverse the current edge in a given mode
   * @return State[] if enforcement acted, null to pass through
   */
  @Nullable
  State[] evaluate(
    GeofencingZone zone,
    boolean entering,
    boolean arriveBy,
    State state,
    EdgeTraversal edge
  );

  /**
   * Resolve the appropriate enforcement implementation for a zone.
   */
  static GeofencingEnforcement forZone(GeofencingZone zone) {
    if (zone.isBusinessArea()) {
      return BusinessAreaEnforcement.INSTANCE;
    }
    return RestrictedZoneEnforcement.INSTANCE;
  }
}
