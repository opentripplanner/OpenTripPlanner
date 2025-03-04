package org.opentripplanner.apis.transmodel.mapping.preferences;

import java.util.List;
import java.util.Set;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;

public class RentalPreferencesMapper {

  public static void mapRentalPreferences(
    VehicleRentalPreferences.Builder rental,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("whiteListed.rentalNetworks", (List<String> networks) ->
      rental.withAllowedNetworks(Set.copyOf(networks))
    );

    callWith.argument("banned.rentalNetworks", (List<String> networks) ->
      rental.withBannedNetworks(Set.copyOf(networks))
    );
    callWith.argument(
      "useBikeRentalAvailabilityInformation",
      rental::withUseAvailabilityInformation
    );
  }
}
