package org.opentripplanner.transit.model.basic;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Equivalent to GTFS route_type or to NeTEx TransportMode.
 */
public enum TransitMode {
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

  public static Set<TransitMode> transitModesExceptAirplane() {
    return NO_AIRPLANE_MODES;
  }

  /**
   * This method returns the list of modes that are considered 'transit' by users, removing
   * carpool.
   */
  public static TransitMode[] modesConsideredTransitByUsers() {
    return Arrays
      .stream(TransitMode.values())
      .filter(m -> m != CARPOOL)
      .toArray(TransitMode[]::new);
  }

  public boolean onStreet() {
    return ON_STREET_MODES.contains(this);
  }
}
