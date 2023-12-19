package org.opentripplanner.apis.transmodel.mapping;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.apis.transmodel.model.TransmodelPlaceType;
import org.opentripplanner.routing.graphfinder.PlaceType;

/**
 * Map to/from {@link TransmodelPlaceType}
 */
public class PlaceMapper {

  public static List<PlaceType> mapToDomain(List<TransmodelPlaceType> api) {
    if (api == null) {
      return null;
    }
    return api.stream().map(PlaceMapper::mapToDomain).distinct().collect(Collectors.toList());
  }

  private static PlaceType mapToDomain(TransmodelPlaceType api) {
    if (api == null) {
      return null;
    }

    return switch (api) {
      case QUAY, STOP_PLACE -> PlaceType.STOP;
      case BICYCLE_RENT -> PlaceType.VEHICLE_RENT;
      case BIKE_PARK -> PlaceType.BIKE_PARK;
      case CAR_PARK -> PlaceType.CAR_PARK;
    };
  }
}
