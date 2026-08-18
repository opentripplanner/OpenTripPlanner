package org.opentripplanner.graph_builder.module.osm.parameters;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.CompoundRefTagGroup;

/**
 * @param boardingAreaRefTags    The ref tags to extract boarding locations from.
 * @param elevatorRefTags        Groups of OSM tags whose values are combined into elevator ids.
 * @param edgeNamer              Controls naming of edges.
 * @param maxAreaNodes           Maximum number of nodes of an area to try to generate visibility
 *                               lines from.
 * @param areaVisibility         Whether to create visibility lines crossing walkable areas.
 * @param platformEntriesLinking Whether platform entries should be linked
 * @param staticParkAndRide      Whether we should create car P+R stations from OSM data.
 * @param staticBikeParkAndRide  Whether we should create bike P+R stations from OSM data.
 * @param includeInclinedEdgeLevelInfo Whether level info for inclined edges should be stored.
 * @param includeOsmStationEntrances Whether we should create station entrances from OSM data.
 */
public record OsmProcessingParameters(
  Set<String> boardingAreaRefTags,
  List<CompoundRefTagGroup> elevatorRefTags,
  EdgeNamer edgeNamer,
  int maxAreaNodes,
  boolean areaVisibility,
  boolean platformEntriesLinking,
  boolean staticParkAndRide,
  boolean staticBikeParkAndRide,
  boolean includeInclinedEdgeLevelInfo,
  boolean includeOsmStationEntrances
) {
  public OsmProcessingParameters {
    boardingAreaRefTags = Set.copyOf(Objects.requireNonNull(boardingAreaRefTags));
    elevatorRefTags = List.copyOf(Objects.requireNonNull(elevatorRefTags));
    Objects.requireNonNull(edgeNamer);
  }
}
