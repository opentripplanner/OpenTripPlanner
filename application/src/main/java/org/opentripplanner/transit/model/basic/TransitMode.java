package org.opentripplanner.transit.model.basic;

import java.util.EnumSet;
import java.util.Set;
import org.opentripplanner.framework.doc.DocumentedEnum;

/**
 * Equivalent to GTFS route_type or to NeTEx TransportMode.
 */
public enum TransitMode implements DocumentedEnum<TransitMode> {
  RAIL,
  COACH,
  SUBWAY,
  BUS,
  TRAM,
  FERRY,
  AIRPLANE,
  CABLE_CAR,
  GONDOLA,
  FUNICULAR,
  TROLLEYBUS,
  MONORAIL,
  CARPOOL,
  TAXI;

  private static final Set<TransitMode> ON_STREET_MODES = EnumSet.of(
    COACH,
    BUS,
    TROLLEYBUS,
    CARPOOL,
    TAXI
  );
  private static final Set<TransitMode> NO_AIRPLANE_MODES = EnumSet.complementOf(
    EnumSet.of(AIRPLANE)
  );

  private static final Set<TransitMode> NO_CARPOOL_MODES = EnumSet.complementOf(
    EnumSet.of(CARPOOL)
  );

  public static Set<TransitMode> transitModesExceptAirplane() {
    return NO_AIRPLANE_MODES;
  }

  /**
   * This method returns the list of modes that are considered 'transit' by users, removing
   * carpool.
   */
  public static TransitMode[] modesConsideredTransitByUsers() {
    return NO_CARPOOL_MODES.toArray(TransitMode[]::new);
  }

  public boolean onStreet() {
    return ON_STREET_MODES.contains(this);
  }

  @Override
  public String typeDescription() {
    return "Routing modes for transit, including rail, bus, ferry, etc. Equivalent to [GTFS `route_type`](https://developers.google.com/transit/gtfs/reference/#routestxt) or to NeTEx TransportMode. ";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case RAIL -> "Used for intercity or long-distance travel.";
      case COACH -> "Used for long-distance bus routes.";
      case SUBWAY -> "Subway or Metro, used for any underground rail system within a metropolitan area.";
      case BUS -> "Used for short- and long-distance bus routes.";
      case TRAM -> "Tram, streetcar or light rail. Used for any light rail or street level system within a metropolitan area.";
      case FERRY -> "Used for short- and long-distance boat service.";
      case AIRPLANE -> "Taking an airplane";
      case CABLE_CAR -> "Used for street-level rail cars where the cable runs beneath the vehicle.";
      case GONDOLA -> "Gondola or suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.";
      case FUNICULAR -> "Used for any rail system that moves on steep inclines with a cable traction system.";
      case TROLLEYBUS -> "Used for trolleybus systems which draw power from overhead wires using poles on the roof of the vehicle.";
      case MONORAIL -> "Used for any rail system that runs on a single rail.";
      case CARPOOL -> """
      Private car trips shared with others.

      This is currently not specified in GTFS so we use the mode type values 1550-1560 which are in the range of private taxis.
      """;
      case TAXI -> "Using a taxi service";
    };
  }
}
