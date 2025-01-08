package org.opentripplanner.service.osminfo;

import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Store OSM data used during graph build, but discard it after it is complete.
 * <p>
 * This is a repository to support the {@link OsmInfoGraphBuildService}.
 */
public interface OsmInfoGraphBuildRepository extends Serializable {
  /**
   * Associate the edge with a platform
   */
  void addPlatform(Edge edge, Platform platform);

  /**
   * Find the platform the edge belongs to
   */
  Optional<Platform> findPlatform(Edge edge);
}
