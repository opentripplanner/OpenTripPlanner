package org.opentripplanner.graph_builder.module.osm.parameters;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.graph_builder.services.osm.WayNamer;

/**
 * @param boardingAreaRefTags    The ref tags to extract boarding locations from.
 * @param wayNamer               Controls naming of edges.
 * @param maxAreaNodes           Maximum number of nodes of an area to try to generate visibility
 *                               lines from.
 * @param areaVisibility         Whether to create visibility lines crossing walkable areas.
 * @param platformEntriesLinking Whether platform entries should be linked
 * @param staticParkAndRide      Whether we should create car P+R stations from OSM data.
 * @param staticBikeParkAndRide  Whether we should create bike P+R stations from OSM data.
 * @param banDiscouragedWalking  Whether ways tagged foot=discouraged should be marked as
 *                               inaccessible.
 * @param banDiscouragedBiking   Whether ways tagged bicycle=discouraged should be marked as
 *                               inaccessible.
 */
public record OsmProcessingParameters(
  Set<String> boardingAreaRefTags,
  WayNamer wayNamer,
  int maxAreaNodes,
  boolean areaVisibility,
  boolean platformEntriesLinking,
  boolean staticParkAndRide,
  boolean staticBikeParkAndRide,
  boolean banDiscouragedWalking,
  boolean banDiscouragedBiking
) {
  public OsmProcessingParameters {
    Objects.requireNonNull(boardingAreaRefTags);
    Objects.requireNonNull(wayNamer);
  }
}
