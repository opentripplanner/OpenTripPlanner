package org.opentripplanner.model.plan.itineraryreference;

import java.util.Objects;

/**
 * The preferences a client can set for a scooter rental access or egress, stored in an
 * {@link ItineraryReference}.
 */
public record ScooterRentalPreferences(
  RidingPreferences riding,
  RentalNetworks networks,
  RentalDestinationPolicy destinationPolicy
) {
  public ScooterRentalPreferences {
    Objects.requireNonNull(riding);
    Objects.requireNonNull(networks);
    Objects.requireNonNull(destinationPolicy);
  }
}
