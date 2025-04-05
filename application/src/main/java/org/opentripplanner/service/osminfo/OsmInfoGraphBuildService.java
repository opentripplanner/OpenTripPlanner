package org.opentripplanner.service.osminfo;

import java.util.Optional;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.Edge;

/**
 * The responsibility of this service is to provide information from Open Street Map, which
 * is NOT in the OTP street graph. The graph build happens in phases, and some data is read in
 * from the OSM files, but needed later on. For example, we might need info from OSM to link street
 * edges/vertexes with transit stops/platforms. We do not want to put data in the OTP street graph
 * unless it is relevant for routing. So, for information that is read by the OsmGraphBuilder, but
 * needed later on, we have this service.
 *
 * THIS SERVICE IS ONLY AVAILABLE DURING GRAPH BUILD, NOT DURING ROUTING. *
 */
public interface OsmInfoGraphBuildService {
  /**
   * Find the platform the given edge is part of.
   * <p>
   * TODO: node platforms should be supported as well.
   */
  Optional<Platform> findPlatform(Edge edge);

  /**
   * Find the platform which relates to an area
   */
  Optional<Platform> findPlatform(Area area);
}
