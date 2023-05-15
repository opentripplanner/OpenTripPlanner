package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VehicleRentalConfig {
  static void mapRentalPreferences(
    NodeAdapter c,
    VehicleRentalPreferences.Builder builder
  ) {
    var dft = builder.original();
    builder
      .withDropoffCost(
        c
          .of("dropOffCost")
          .since(V2_0)
          .summary("Cost to drop-off a rented vehicle.")
          .asInt(dft.dropoffCost())
      )
      .withDropoffTime(
        c
          .of("dropOffTime")
          .since(V2_0)
          .summary("Time to drop-off a rented vehicle.")
          .asInt(dft.dropoffTime())
      )
      .withPickupCost(
        c
          .of("pickupCost")
          .since(V2_0)
          .summary("Cost to rent a vehicle.")
          .asInt(dft.pickupCost())
      )
      .withPickupTime(
        c
          .of("pickupTime")
          .since(V2_0)
          .summary("Time to rent a vehicle.")
          .asInt(dft.pickupTime())
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
          .asDouble(dft.arrivingInRentalVehicleAtDestinationCost())
      );
  }

  static void setVehicleRentalRequestOptions(NodeAdapter c, RouteRequest request) {
    VehicleRentalRequest vehicleRentalRequest = request.journey().rental();

    vehicleRentalRequest.setAllowedNetworks(
      c
        .of("allowedNetworks")
        .since(V2_1)
        .summary(
          "The vehicle rental networks which may be used. If empty all networks may be used."
        )
        .asStringSet(vehicleRentalRequest.allowedNetworks())
    );
    vehicleRentalRequest.setBannedNetworks(
      c
        .of("bannedNetworks")
        .since(V2_1)
        .summary(
          "he vehicle rental networks which may not be used. If empty, no networks are banned."
        )
        .asStringSet(vehicleRentalRequest.bannedNetworks())
    );

    request
      .journey()
      .rental()
      .setAllowArrivingInRentedVehicleAtDestination(
        c
          .of("allowKeepingAtDestination")
          .since(V2_2)
          .summary(
            "If a vehicle should be allowed to be kept at the end of a station-based rental."
          )
          .asBoolean(request.journey().rental().allowArrivingInRentedVehicleAtDestination())
      );
  }

  static void setVehicleRental(NodeAdapter c, RouteRequest request,
                               RoutingPreferences.Builder preferences) {
    var vehicleRental = c
      .of("vehicleRental")
      .since(V2_3)
      .summary("Vehicle rental options")
      .asObject();
    preferences.withRental(it -> mapRentalPreferences(vehicleRental, it));
    setVehicleRentalRequestOptions(vehicleRental, request);
  }
}
