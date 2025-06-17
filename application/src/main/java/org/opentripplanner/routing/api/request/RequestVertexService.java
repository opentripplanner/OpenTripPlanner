package org.opentripplanner.routing.api.request;

import java.util.Set;
import org.opentripplanner.street.model.vertex.Vertex;

public interface RequestVertexService {
  /**
   * Temporary vertices for the origin. Can contain multiple vertices when the origin is a station, for
   * example.
   * <p>
   * Returns an empty set for default requests.
   */
  public Set<Vertex> from();

  /**
   * Temporary vertices for the destination. Can contain multiple vertices when the destination is a station,
   * for example.
   * <p>
   * Returns an empty set for default requests.
   */
  public Set<Vertex> to();
}
