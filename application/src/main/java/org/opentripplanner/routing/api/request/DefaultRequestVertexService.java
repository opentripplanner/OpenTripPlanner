package org.opentripplanner.routing.api.request;

import java.util.Set;
import org.opentripplanner.street.model.vertex.Vertex;

public class DefaultRequestVertexService implements RequestVertexService {

  public static final DefaultRequestVertexService DEFAULT = new DefaultRequestVertexService(
    Set.of(),
    Set.of()
  );

  private final Set<Vertex> from;
  private final Set<Vertex> to;

  public DefaultRequestVertexService(Set<Vertex> from, Set<Vertex> to) {
    this.from = from;
    this.to = to;
  }

  @Override
  public Set<Vertex> from() {
    return from;
  }

  @Override
  public Set<Vertex> to() {
    return to;
  }
}
