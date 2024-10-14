package org.opentripplanner.ext.restapi.mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class ModeMapper {

  public static String mapToApi(Leg domain) {
    if (domain == null) {
      return null;
    }
    if (domain instanceof StreetLeg sl) {
      return mapToApi(sl.getMode());
    }
    if (domain instanceof TransitLeg tl) {
      return mapToApi(tl.getMode());
    }
    throw new IllegalStateException("Unhandled leg type: " + domain);
  }

  public static List<String> mapToApi(Set<TransitMode> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(ModeMapper::mapToApi).collect(Collectors.toList());
  }

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
