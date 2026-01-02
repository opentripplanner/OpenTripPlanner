package org.opentripplanner.osm.model;

/**
 * The source of the information for an {@link OsmLevel}.
 */
public enum OsmLevelSource {
  LEVEL_TAG,
  LAYER_TAG,
  /**
   * If a level can not be parsed from OSM, the default level is used. Displaying a default level
   * in the API can be confusing. This enum allows separating parsed data from the default level.
   */
  DEFAULT,
}
