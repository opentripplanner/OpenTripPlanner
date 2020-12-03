package org.opentripplanner.model.modes;

/**
 * Equivalent to GTFS route_type or to NeTEx TransportMode. Used as part of the TransitMode class.
 */
public enum TransitMainMode {
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
  /**
   * Not yet supported.
   */
  FLEXIBLE
}
