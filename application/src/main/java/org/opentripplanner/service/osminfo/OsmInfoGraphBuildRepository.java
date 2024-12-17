package org.opentripplanner.service.osminfo;

import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.service.osminfo.model.OsmWayReferences;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Store OSM data used during graph build, but after the OSM Graph Builder is done.
 * <p>
 * This is a repository to support the {@link OsmInfoGraphBuildService}.
 */
public interface OsmInfoGraphBuildRepository extends Serializable {
  /**
   * TODO Add doc
   */
  void addReferences(Edge edge, OsmWayReferences info);

  /**
   * TODO Add doc
   */
  Optional<OsmWayReferences> findReferences(Edge edge);
}
