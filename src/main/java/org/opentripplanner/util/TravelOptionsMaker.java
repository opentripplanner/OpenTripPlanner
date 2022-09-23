package org.opentripplanner.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * Class which creates "Travel by" options list from supported transit modes and which extra modes
 * are supported (bike sharing, bike &amp; ride, park &amp; ride)
 * <p>
 * This list is then returned to the client which shows it in UI. Created by mabu on 28.7.2015.
 */
public final class TravelOptionsMaker {

  private static final List<TravelOption> staticTravelOptions;

  static {
    staticTravelOptions = new ArrayList<>(3);
    staticTravelOptions.add(new TravelOption(ApiRequestMode.WALK.toString()));
    staticTravelOptions.add(new TravelOption(ApiRequestMode.BICYCLE.toString()));
    staticTravelOptions.add(new TravelOption(ApiRequestMode.CAR.toString()));
  }

  public static List<TravelOption> makeOptions(Graph graph, TransitService transitService) {
    return makeOptions(
      transitService.getTransitModes(),
      graph.hasBikeSharing,
      graph.hasBikeRide,
      graph.hasParkRide
    );
  }

  public static List<TravelOption> makeOptions(
    Set<TransitMode> transitModes,
    boolean hasBikeSharing,
    boolean hasBikeRide,
    boolean hasParkRide
  ) {
    List<TravelOption> travelOptions = new ArrayList<>(16);

    //Adds Transit, and all the transit modes
    if (!transitModes.isEmpty()) {
      travelOptions.add(
        new TravelOption(
          String.join(",", ApiRequestMode.TRANSIT.toString(), ApiRequestMode.WALK.toString()),
          ApiRequestMode.TRANSIT.toString()
        )
      );

      for (TransitMode transitMode : transitModes) {
        travelOptions.add(
          new TravelOption(
            String.join(",", transitMode.toString(), ApiRequestMode.WALK.toString()),
            transitMode.toString()
          )
        );
      }
    }

    travelOptions.addAll(staticTravelOptions);

    if (hasBikeSharing) {
      travelOptions.add(
        new TravelOption(
          String.join(",", ApiRequestMode.WALK.toString(), "BICYCLE_RENT"),
          "BICYCLERENT"
        )
      );
    }

    //If transit modes exists.
    if (!transitModes.isEmpty()) {
      //Adds bicycle transit mode
      travelOptions.add(
        new TravelOption(
          String.join(",", ApiRequestMode.TRANSIT.toString(), ApiRequestMode.BICYCLE.toString()),
          String.join("_", ApiRequestMode.TRANSIT.toString(), ApiRequestMode.BICYCLE.toString())
        )
      );
      if (hasBikeSharing) {
        travelOptions.add(
          new TravelOption(
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
          new TravelOption(
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
          new TravelOption(
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
        new TravelOption(
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
