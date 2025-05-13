package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import java.util.List;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VehicleParkingConfig {

  static void mapParking(NodeAdapter c, VehicleParkingPreferences.Builder preferences) {
    var vehicleParking = c
      .of("parking")
      .since(V2_5)
      .summary("Preferences for parking a vehicle.")
      .asObject();
    mapParkingPreferences(vehicleParking, preferences);
  }

  private static void mapParkingPreferences(
    NodeAdapter c,
    VehicleParkingPreferences.Builder builder
  ) {
    builder
      .withUnpreferredVehicleParkingTagCost(
        c
          .of("unpreferredVehicleParkingTagCost")
          .since(V2_3)
          .summary("What cost to add if a parking facility doesn't contain a preferred tag.")
          .description("See `preferredVehicleParkingTags`.")
          .asInt(VehicleParkingPreferences.DEFAULT.unpreferredVehicleParkingTagCost().toSeconds())
      )
      .withBannedVehicleParkingTags(
        c
          .of("bannedVehicleParkingTags")
          .since(V2_1)
          .summary(
            "Tags with which a vehicle parking will not be used. If empty, no tags are banned."
          )
          .description(
            """
            Vehicle parking tags can originate from different places depending on the origin of the parking (OSM or RT feed).
            """
          )
          .asStringSet(List.of())
      )
      .withRequiredVehicleParkingTags(
        c
          .of("requiredVehicleParkingTags")
          .since(V2_1)
          .summary(
            "Tags without which a vehicle parking will not be used. If empty, no tags are required."
          )
          .description(
            """
            Vehicle parking tags can originate from different places depending on the origin of the parking (OSM or RT feed).
            """
          )
          .asStringSet(List.of())
      )
      .withTime(
        c
          .of("time")
          .since(V2_0)
          .summary("Time to park a vehicle.")
          .asDuration(VehicleParkingPreferences.DEFAULT.time())
      )
      .withCost(
        c
          .of("cost")
          .since(V2_0)
          .summary("Cost to park a vehicle.")
          .asInt(VehicleParkingPreferences.DEFAULT.cost().toSeconds())
      )
      .withPreferredVehicleParkingTags(
        c
          .of("preferredVehicleParkingTags")
          .since(V2_3)
          .summary(
            "Vehicle parking facilities that don't have one of these tags will receive an extra cost and will therefore be penalised."
          )
          .description(
            """
            Vehicle parking tags can originate from different places depending on the origin of the parking (OSM or RT feed).
            """
          )
          .asStringSet(List.of())
      );
  }
}
