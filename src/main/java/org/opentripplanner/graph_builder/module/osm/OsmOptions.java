package org.opentripplanner.graph_builder.module.osm;

import java.util.Set;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;

/**
 * @param customNamer           Allows for arbitrary custom naming of edges.
 * @param staticParkAndRide     Whether we should create car P+R stations from OSM data.
 * @param staticBikeParkAndRide Whether we should create bike P+R stations from OSM data.
 * @param banDiscouragedWalking Whether ways tagged foot/bicycle=discouraged should be marked as
 *                              inaccessible
 */
public record OsmOptions(
  Set<String> boardingAreaRefTags,
  CustomNamer customNamer,
  int maxAreaNodes,
  boolean areaVisibility,
  boolean platformEntriesLinking,
  boolean staticParkAndRide,
  boolean staticBikeParkAndRide,
  boolean banDiscouragedWalking,
  boolean banDiscouragedBiking
) {}
