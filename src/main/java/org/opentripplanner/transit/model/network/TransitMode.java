package org.opentripplanner.transit.model.network;

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
  MONORAIL;

  private static final Set<TransitMode> ON_STREET_MODES = EnumSet.of(COACH, BUS, TROLLEYBUS);
  private static final Set<TransitMode> NO_AIRPLANE_MODES = EnumSet.complementOf(
    EnumSet.of(AIRPLANE)
  );

  public static Set<TransitMode> transitModesExceptAirplane() {
    return NO_AIRPLANE_MODES;
  }

  public boolean onStreet() {
    return ON_STREET_MODES.contains(this);
  }
}
