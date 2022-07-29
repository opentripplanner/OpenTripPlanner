package org.opentripplanner.transit.model.site;

/**
 * Categorization for the grouping
 */
public enum GroupOfStationsPurpose {
  /**
   * Group of prominent stop places within a town or city(centre)
   */
  GENERALIZATION,
  /**
   * Stop places in proximity to each other which have a natural geospatial- or public transport
   * related relationship.
   */
  CLUSTER,
}
