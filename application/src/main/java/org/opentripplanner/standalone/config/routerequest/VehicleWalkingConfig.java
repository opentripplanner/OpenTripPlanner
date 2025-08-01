package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VehicleWalkingConfig {

  static void mapVehicleWalking(NodeAdapter c, VehicleWalkingPreferences.Builder preferences) {
    var vehicleWalking = c
      .of("walk")
      .since(V2_5)
      .summary("Preferences for walking a vehicle.")
      .asObject();
    mapVehicleWalkingPreferences(vehicleWalking, preferences);
  }

  private static void mapVehicleWalkingPreferences(
    NodeAdapter c,
    VehicleWalkingPreferences.Builder builder
  ) {
    var dft = builder.original();
    builder
      .withSpeed(
        c
          .of("speed")
          .since(V2_1)
          .summary(
            "The user's vehicle walking speed in meters/second. Defaults to approximately 3 MPH."
          )
          .asDouble(dft.speed())
      )
      .withReluctance(
        c
          .of("reluctance")
          .since(V2_1)
          .summary(
            "A multiplier for how bad walking with a vehicle is, compared to being in transit for equal lengths of time."
          )
          .asDouble(dft.reluctance())
      )
      .withMountDismountTime(
        c
          .of("mountDismountTime")
          .since(V2_0)
          .summary("The time it takes the user to hop on or off a vehicle.")
          .description(
            """
            Time it takes to rent or park a vehicle have their own parameters and this is not meant
            for controlling the duration of those events.
            """
          )
          .asDuration(dft.mountDismountTime())
      )
      .withMountDismountCost(
        c
          .of("mountDismountCost")
          .since(V2_0)
          .summary("The cost of hopping on or off a vehicle.")
          .description(
            """
            There are different parameters for the cost of renting or parking a vehicle and this is
            not meant for controlling the cost of those events.
            """
          )
          .asInt(dft.mountDismountCost().toSeconds())
      )
      .withStairsReluctance(
        c
          .of("stairsReluctance")
          .since(V2_3)
          .summary(
            "How bad is it to walk the vehicle up/down a flight of stairs compared to walking it on flat ground."
          )
          .asDouble(dft.stairsReluctance())
      );
  }
}
