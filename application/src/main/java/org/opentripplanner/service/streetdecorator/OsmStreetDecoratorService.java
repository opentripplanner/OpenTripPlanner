package org.opentripplanner.service.streetdecorator;

import java.util.Optional;
import org.opentripplanner.service.streetdecorator.model.EdgeInformation;
import org.opentripplanner.street.model.edge.Edge;

/**
 * The responsibility of this service is to provide level and incline information from
 * Open Street Map, which is NOT in the OTP street graph.
 *
 * THIS SERVICE IS AVAILABLE WHEN THE USER MAKES A REQUEST.
 */
public interface OsmStreetDecoratorService {
  /**
   * Find level or incline information for a given edge.
   */
  Optional<EdgeInformation> findEdgeInformation(Edge edge);
}
