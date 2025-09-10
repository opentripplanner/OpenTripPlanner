package org.opentripplanner.service.streetdecorator;

import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.service.streetdecorator.model.EdgeInformation;
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
    OsmLevel lowerVertex,
    long lowerVertexOsmId,
    OsmLevel upperVertex,
    long upperVertexOsmId
  );

  /**
   * Associate the edge with incline information.
   */
  void addEdgeInclineInformation(Edge edge, long lowerVertexOsmId, long upperVertexOsmId);

  /**
   * Find level or incline information for a given edge.
   */
  Optional<EdgeInformation> findEdgeInformation(Edge edge);
}
