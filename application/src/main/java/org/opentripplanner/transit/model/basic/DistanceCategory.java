package org.opentripplanner.transit.model.basic;

/**
 * The distance category of a transit service: how far it travels and which kind of journeys it
 * is meant for. This lets clients tell apart services that share a main mode, e.g. suburban,
 * regional and long-distance trains, which all have the main mode {@link TransitMode#RAIL}.
 * <p>
 * The category is not part of the input data, but derived from the NeTEx submode or the GTFS
 * extended route type, see {@link org.opentripplanner.transit.service.DistanceCategoryHelper}.
 * Every trip has a category; there is deliberately no "unknown" value, as it would complicate the
 * clients.
 */
public enum DistanceCategory {
  /**
   * Services within a city or its suburbs, e.g. urban railways, trams, shuttles and local buses.
   */
  LOCAL,
  /** Services connecting the towns and cities of a region. */
  REGIONAL,
  /** National and international services, including high speed rail and night trains. */
  LONG_DISTANCE,
}
