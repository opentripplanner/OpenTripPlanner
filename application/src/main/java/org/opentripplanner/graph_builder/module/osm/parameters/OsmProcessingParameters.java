package org.opentripplanner.graph_builder.module.osm.parameters;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;

/**
 * @param boardingAreaRefTags    The ref tags to extract boarding locations from.
 * @param edgeNamer              Controls naming of edges.
 * @param maxAreaNodes           Maximum number of nodes of an area to try to generate visibility
 *                               lines from.
 * @param areaVisibility         Whether to create visibility lines crossing walkable areas.
 * @param platformEntriesLinking Whether platform entries should be linked
 * @param staticParkAndRide      Whether we should create car P+R stations from OSM data.
 * @param staticBikeParkAndRide  Whether we should create bike P+R stations from OSM data.
 * @param includeOsmSubwayEntrances Whether we should create subway entrances from OSM data.
 */
public record OsmProcessingParameters(
  Set<String> boardingAreaRefTags,
  EdgeNamer edgeNamer,
  int maxAreaNodes,
  boolean areaVisibility,
  boolean platformEntriesLinking,
  boolean staticParkAndRide,
  boolean staticBikeParkAndRide,
  boolean includeOsmSubwayEntrances
) {
  public OsmProcessingParameters {
    boardingAreaRefTags = Set.copyOf(Objects.requireNonNull(boardingAreaRefTags));
    Objects.requireNonNull(edgeNamer);
  }
}
