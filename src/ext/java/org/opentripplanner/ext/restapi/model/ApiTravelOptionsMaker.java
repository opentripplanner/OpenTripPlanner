package org.opentripplanner.ext.restapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * Class which creates "Travel by" options list from supported transit modes and which extra modes
 * are supported (bike sharing, bike &amp; ride, park &amp; ride)
 * <p>
 * This list is then returned to the client which shows it in UI. Created by mabu on 28.7.2015.
 */
public final class ApiTravelOptionsMaker {

  private static final List<ApiTravelOption> staticTravelOptions;

  static {
    staticTravelOptions = new ArrayList<>(3);
    staticTravelOptions.add(new ApiTravelOption(ApiRequestMode.WALK.toString()));
    staticTravelOptions.add(new ApiTravelOption(ApiRequestMode.BICYCLE.toString()));
    staticTravelOptions.add(new ApiTravelOption(ApiRequestMode.CAR.toString()));
  }

  public static List<ApiTravelOption> makeOptions(
    Graph graph,
    VehicleRentalService vehicleRentalService,
    TransitService transitService
  ) {
    var service = graph.getVehicleParkingService();
    return makeOptions(
      transitService.getTransitModes(),
      vehicleRentalService.hasRentalBikes(),
      service.hasBikeParking(),
      service.hasCarParking()
    );
  }

  public static List<ApiTravelOption> makeOptions(
    Set<TransitMode> transitModes,
    boolean hasBikeSharing,
    boolean hasBikeRide,
    boolean hasParkRide
  ) {
    List<ApiTravelOption> travelOptions = new ArrayList<>(16);

    //Adds Transit, and all the transit modes
    if (!transitModes.isEmpty()) {
      travelOptions.add(
        new ApiTravelOption(
          String.join(",", ApiRequestMode.TRANSIT.toString(), ApiRequestMode.WALK.toString()),
          ApiRequestMode.TRANSIT.toString()
        )
      );

      for (TransitMode transitMode : transitModes) {
        travelOptions.add(
          new ApiTravelOption(
            String.join(",", transitMode.toString(), ApiRequestMode.WALK.toString()),
            transitMode.toString()
          )
        );
      }
    }

    travelOptions.addAll(staticTravelOptions);

    if (hasBikeSharing) {
      travelOptions.add(
        new ApiTravelOption(
          String.join(",", ApiRequestMode.WALK.toString(), "BICYCLE_RENT"),
          "BICYCLERENT"
        )
      );
    }

    //If transit modes exists.
    if (!transitModes.isEmpty()) {
      //Adds bicycle transit mode
      travelOptions.add(
        new ApiTravelOption(
          String.join(",", ApiRequestMode.TRANSIT.toString(), ApiRequestMode.BICYCLE.toString()),
          String.join("_", ApiRequestMode.TRANSIT.toString(), ApiRequestMode.BICYCLE.toString())
        )
      );
      if (hasBikeSharing) {
        travelOptions.add(
          new ApiTravelOption(
            String.join(
              ",",
              ApiRequestMode.TRANSIT.toString(),
              ApiRequestMode.WALK.toString(),
              "BICYCLE_RENT"
            ),
            "TRANSIT_BICYCLERENT"
          )
        );
      }
      if (hasParkRide) {
        travelOptions.add(
          new ApiTravelOption(
            String.join(
              ",",
              "CAR_PARK",
              ApiRequestMode.WALK.toString(),
              ApiRequestMode.TRANSIT.toString()
            ),
            "PARKRIDE"
          )
        );
      }
      if (hasBikeRide) {
        travelOptions.add(
          new ApiTravelOption(
            String.join(
              ",",
              "BICYCLE_PARK",
              ApiRequestMode.WALK.toString(),
              ApiRequestMode.TRANSIT.toString()
            ),
            "BIKERIDE"
          )
        );
      }
      travelOptions.add(
        new ApiTravelOption(
          String.join(
            ",",
            ApiRequestMode.CAR.toString(),
            ApiRequestMode.WALK.toString(),
            ApiRequestMode.TRANSIT.toString()
          ),
          "KISSRIDE"
        )
      );
    }

    return travelOptions;
  }
}
