package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VehicleRentalConfig {

  static void mapRental(NodeAdapter c, VehicleRentalPreferences.Builder preferences) {
    var vehicleRental = c.of("rental").since(V2_3).summary("Vehicle rental options").asObject();
    mapRentalPreferences(vehicleRental, preferences);
  }

  private static void mapRentalPreferences(
    NodeAdapter c,
    VehicleRentalPreferences.Builder builder
  ) {
    var dft = builder.original();
    builder
      .withDropOffCost(
        c
          .of("dropOffCost")
          .since(V2_0)
          .summary("Cost to drop-off a rented vehicle.")
          .asInt(dft.dropOffCost().toSeconds())
      )
      .withDropOffTime(
        c
          .of("dropOffTime")
          .since(V2_0)
          .summary("Time to drop-off a rented vehicle.")
          .asDuration(dft.dropOffTime())
      )
      .withPickupCost(
        c
          .of("pickupCost")
          .since(V2_0)
          .summary("Cost to rent a vehicle.")
          .asInt(dft.pickupCost().toSeconds())
      )
      .withPickupTime(
        c
          .of("pickupTime")
          .since(V2_0)
          .summary("Time to rent a vehicle.")
          .asDuration(dft.pickupTime())
      )
      .withUseAvailabilityInformation(
        c
          .of("useAvailabilityInformation")
          .since(V2_0)
          .summary(
            "Whether or not vehicle rental availability information will be used to plan vehicle rental trips."
          )
          .asBoolean(dft.useAvailabilityInformation())
      )
      .withArrivingInRentalVehicleAtDestinationCost(
        c
          .of("keepingAtDestinationCost")
          .since(V2_2)
          .summary(
            "The cost of arriving at the destination with the rented vehicle, to discourage doing so."
          )
          .asInt(dft.arrivingInRentalVehicleAtDestinationCost().toSeconds())
      )
      .withAllowArrivingInRentedVehicleAtDestination(
        c
          .of("allowKeepingAtDestination")
          .since(V2_2)
          .summary(
            "If a vehicle should be allowed to be kept at the end of a station-based rental."
          )
          .asBoolean(dft.allowArrivingInRentedVehicleAtDestination())
      )
      .withAllowedNetworks(
        c
          .of("allowedNetworks")
          .since(V2_1)
          .summary(
            "The vehicle rental networks which may be used. If empty all networks may be used."
          )
          .asStringSet(dft.allowedNetworks())
      )
      .withBannedNetworks(
        c
          .of("bannedNetworks")
          .since(V2_1)
          .summary(
            "The vehicle rental networks which may not be used. If empty, no networks are banned."
          )
          .asStringSet(dft.bannedNetworks())
      );
  }
}
