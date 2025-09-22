package org.opentripplanner.service.streetdecorator.internal;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.service.streetdecorator.OsmStreetDecoratorRepository;
import org.opentripplanner.service.streetdecorator.model.EdgeLevelInfo;
import org.opentripplanner.service.streetdecorator.model.VertexLevelInfo;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class DefaultOsmStreetDecoratorRepository
  implements OsmStreetDecoratorRepository, Serializable {

  private final Map<Edge, EdgeLevelInfo> edgeInformation = new HashMap<>();

  @Inject
  public DefaultOsmStreetDecoratorRepository() {}

  @Override
  public void addEdgeLevelInformation(
    Edge edge,
    VertexLevelInfo lowerVertexInfo,
    VertexLevelInfo upperVertexInfo
  ) {
    Objects.requireNonNull(edge);
    Objects.requireNonNull(lowerVertexInfo);
    Objects.requireNonNull(upperVertexInfo);
    this.edgeInformation.put(edge, new EdgeLevelInfo(lowerVertexInfo, upperVertexInfo));
  }

  @Override
  public Optional<EdgeLevelInfo> findEdgeInformation(Edge edge) {
    return Optional.ofNullable(edgeInformation.get(edge));
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultOsmStreetDecoratorRepository.class)
      .addNum("Edges with level information", edgeInformation.size())
      .toString();
  }
}
