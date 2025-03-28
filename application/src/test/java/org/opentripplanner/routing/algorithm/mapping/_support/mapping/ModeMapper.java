package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.basic.TransitMode;

@Deprecated
class ModeMapper {

  public static String mapToApi(TraverseMode domain) {
    if (domain == null) {
      return null;
    }

    return switch (domain) {
      case BICYCLE -> "BICYCLE";
      case CAR -> "CAR";
      case WALK -> "WALK";
      case SCOOTER -> "SCOOTER";
      case FLEX -> throw new IllegalStateException();
    };
  }

  public static String mapToApi(TransitMode domain) {
    if (domain == null) {
      return null;
    }

    return switch (domain) {
      case AIRPLANE -> "AIRPLANE";
      case BUS -> "BUS";
      case CABLE_CAR -> "CABLE_CAR";
      case COACH -> "COACH";
      case FERRY -> "FERRY";
      case FUNICULAR -> "FUNICULAR";
      case GONDOLA -> "GONDOLA";
      case RAIL -> "RAIL";
      case SUBWAY -> "SUBWAY";
      case TRAM -> "TRAM";
      case TROLLEYBUS -> "TROLLEYBUS";
      case MONORAIL -> "MONORAIL";
      case CARPOOL -> "CARPOOL";
      case TAXI -> "TAXI";
    };
  }
}
