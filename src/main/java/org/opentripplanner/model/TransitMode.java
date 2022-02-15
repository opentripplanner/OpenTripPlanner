package org.opentripplanner.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

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

  private static final Set<TransitMode> ON_STREET_MODES = EnumSet.of(
          COACH, BUS, TROLLEYBUS
  );
  private static final Set<TransitMode> NO_AIRPLANE_MODES = EnumSet.complementOf(EnumSet.of(AIRPLANE));


  public boolean onStreet() {
    return ON_STREET_MODES.contains(this);
  }

  public static Set<TransitMode> transitModesExceptAirplane() {
    return NO_AIRPLANE_MODES;
  }
}
