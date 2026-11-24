package org.opentripplanner.model.plan.itineraryreference;

import java.util.Objects;

/**
 * The preferences a client can set for a bike rental access or egress, stored in an
 * {@link ItineraryReference}.
 */
public record BikeRentalPreferences(
  RidingPreferences riding,
  BikeWalkingPreferences walking,
  RentalNetworks networks,
  RentalDestinationPolicy destinationPolicy
) {
  public BikeRentalPreferences {
    Objects.requireNonNull(riding);
    Objects.requireNonNull(walking);
    Objects.requireNonNull(networks);
    Objects.requireNonNull(destinationPolicy);
  }
}
