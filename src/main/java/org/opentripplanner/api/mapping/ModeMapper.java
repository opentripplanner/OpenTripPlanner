package org.opentripplanner.api.mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.LegMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class ModeMapper {

  public static String mapToApi(LegMode domain) {
    if (domain == null) {
      return null;
    }

    return switch (domain) {
      case AIRPLANE -> "AIRPLANE";
      case BICYCLE -> "BICYCLE";
      case BUS -> "BUS";
      case CAR -> "CAR";
      case CABLE_CAR -> "CABLE_CAR";
      case COACH -> "COACH";
      case FERRY -> "FERRY";
      case FUNICULAR -> "FUNICULAR";
      case GONDOLA -> "GONDOLA";
      case RAIL -> "RAIL";
      case SUBWAY -> "SUBWAY";
      case TRAM -> "TRAM";
      case WALK -> "WALK";
      case SCOOTER -> "SCOOTER";
      case TROLLEYBUS -> "TROLLEYBUS";
      case MONORAIL -> "MONORAIL";
    };
  }

  public static List<String> mapToApi(Set<TransitMode> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(ModeMapper::mapToApi).collect(Collectors.toList());
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
    };
  }
}
