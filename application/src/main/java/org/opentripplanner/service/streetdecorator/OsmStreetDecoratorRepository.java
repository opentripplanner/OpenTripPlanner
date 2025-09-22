package org.opentripplanner.service.streetdecorator;

import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.service.streetdecorator.model.EdgeLevelInfo;
import org.opentripplanner.service.streetdecorator.model.VertexLevelInfo;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Store OSM level and incline data used for returning responses to requests.
 * <p>
 * This is a repository to support the {@link OsmStreetDecoratorService}.
 */
public interface OsmStreetDecoratorRepository extends Serializable {
  /**
   * Associate the edge with level information.
   */
  void addEdgeLevelInformation(
    Edge edge,
    VertexLevelInfo lowerVertexInfo,
    VertexLevelInfo upperVertexInfo
  );

  /**
   * Find level or incline information for a given edge.
   */
  Optional<EdgeLevelInfo> findEdgeInformation(Edge edge);
}
