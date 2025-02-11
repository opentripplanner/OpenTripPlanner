package org.opentripplanner.service.osminfo;

import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.NamedArea;

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
   * Associate named area with a platform
   */
  void addPlatform(NamedArea area, Platform platform);

  /**
   * Find the platform the edge belongs to
   */
  Optional<Platform> findPlatform(Edge edge);


  /**
   * Find the platform which relates to an area
   */
  Optional<Platform> findPlatform(NamedArea area);
}
