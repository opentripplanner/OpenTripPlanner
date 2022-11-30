package org.opentripplanner.transit.model.basic;

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
}
