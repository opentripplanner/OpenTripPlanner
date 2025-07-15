package org.opentripplanner.service.osminfo;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.edge.Area;
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
   * Associate named area with a platform
   */
  void addPlatform(Area area, Platform platform);

  /**
   * Add a known turn restriction
   */
  void addTurnRestriction(TurnRestriction turnRestriction);

  /**
   * Find the platform the edge belongs to
   */
  Optional<Platform> findPlatform(Edge edge);

  /**
   * Find the platform which relates to an area
   */
  Optional<Platform> findPlatform(Area area);

  Collection<TurnRestriction> listTurnRestrictions();
}
