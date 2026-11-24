package org.opentripplanner.model.plan.itineraryreference;

import java.util.Set;

/**
 * The rental networks a client allows and bans for a rental vehicle.
 *
 * @param allowed The networks that may be used. If empty, all networks may be used.
 * @param banned The networks that may not be used.
 */
public record RentalNetworks(Set<String> allowed, Set<String> banned) {
  public RentalNetworks {
    allowed = Set.copyOf(allowed);
    banned = Set.copyOf(banned);
  }
}
