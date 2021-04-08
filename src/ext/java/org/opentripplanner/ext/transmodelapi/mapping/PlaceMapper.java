package org.opentripplanner.ext.transmodelapi.mapping;

import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.routing.graphfinder.PlaceType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Map to/from {@link TransmodelPlaceType}
 */
public class PlaceMapper {

  public static List<PlaceType> mapToDomain(List<TransmodelPlaceType> api) {
    if (api == null) {  return null;  }
    return api.stream().map(PlaceMapper::mapToDomain).distinct().collect(Collectors.toList());
  }

  private static PlaceType mapToDomain(TransmodelPlaceType api){
    if (api == null) {  return null;  }

    switch (api) {
      case QUAY:
      case STOP_PLACE:
        return PlaceType.STOP;
      case BICYCLE_RENT:
        return PlaceType.BICYCLE_RENT;
      case BIKE_PARK:
        return PlaceType.BIKE_PARK;
      case CAR_PARK:
        return PlaceType.CAR_PARK;
      default:
        throw new IllegalArgumentException("Unknown place type: " + api);
    }
  }
}
