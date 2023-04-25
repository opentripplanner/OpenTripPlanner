package org.opentripplanner.graph_builder.module.osm;

import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;

/**
 * Allows for arbitrary custom naming of edges.
 */
/**
 * Whether we should create car P+R stations from OSM data. The default value is true. In normal
 * operation it is set by the JSON graph build configuration, but it is also initialized to "true"
 * here to provide the default behavior in tests.
 */
/**
 * Whether we should create bike P+R stations from OSM data. (default false)
 */
/**
 * Whether ways tagged foot/bicycle=discouraged should be marked as inaccessible
 */
public record OpenStreetMapOptions(
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
