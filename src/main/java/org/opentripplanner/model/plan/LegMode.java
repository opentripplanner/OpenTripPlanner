package org.opentripplanner.model.plan;

import java.util.EnumSet;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public enum LegMode {
  WALK,
  BICYCLE,
  SCOOTER,
  CAR,
  TRAM,
  SUBWAY,
  RAIL,
  BUS,
  COACH,
  FERRY,
  CABLE_CAR,
  GONDOLA,
  FUNICULAR,
  AIRPLANE,
  TROLLEYBUS,
  MONORAIL;

  private static final EnumSet<LegMode> TRANSIT_MODES = EnumSet.of(
    AIRPLANE,
    BUS,
    CABLE_CAR,
    COACH,
    FERRY,
    FUNICULAR,
    GONDOLA,
    RAIL,
    SUBWAY,
    TRAM,
    TROLLEYBUS,
    MONORAIL
  );

  public static LegMode fromTransitMode(TransitMode transitMode) {
    return switch (transitMode) {
      case RAIL -> RAIL;
      case MONORAIL -> MONORAIL;
      case COACH -> COACH;
      case BUS -> BUS;
      case SUBWAY -> SUBWAY;
      case TRAM -> TRAM;
      case FERRY -> FERRY;
      case AIRPLANE -> AIRPLANE;
      case CABLE_CAR -> CABLE_CAR;
      case GONDOLA -> GONDOLA;
      case FUNICULAR -> FUNICULAR;
      case TROLLEYBUS -> TROLLEYBUS;
    };
  }

  public static LegMode fromAStarTraverseMode(TraverseMode mode) {
    return switch (mode) {
      case WALK -> WALK;
      case BICYCLE -> BICYCLE;
      case SCOOTER -> SCOOTER;
      case CAR -> CAR;
      case FLEX -> throw new IllegalStateException();
    };
  }

  public boolean isTransit() {
    return TRANSIT_MODES.contains(this);
  }
}
