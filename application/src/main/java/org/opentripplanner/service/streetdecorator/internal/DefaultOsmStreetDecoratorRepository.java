package org.opentripplanner.service.streetdecorator.internal;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.service.streetdecorator.OsmStreetDecoratorRepository;
import org.opentripplanner.service.streetdecorator.model.EdgeInclineInformation;
import org.opentripplanner.service.streetdecorator.model.EdgeInformation;
import org.opentripplanner.service.streetdecorator.model.EdgeLevelInformation;
import org.opentripplanner.service.streetdecorator.model.VertexLevelInformation;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class DefaultOsmStreetDecoratorRepository
  implements OsmStreetDecoratorRepository, Serializable {

  private final Map<Edge, EdgeInformation> edgeInformation = new HashMap<>();

  @Inject
  public DefaultOsmStreetDecoratorRepository() {}

  @Override
  public void addEdgeLevelInformation(
    Edge edge,
    OsmLevel lowerVertex,
    long lowerVertexOsmId,
    OsmLevel upperVertex,
    long upperVertexOsmId
  ) {
    Objects.requireNonNull(edge);
    Objects.requireNonNull(lowerVertex);
    Objects.requireNonNull(upperVertex);
    this.edgeInformation.put(
        edge,
        new EdgeLevelInformation(
          new VertexLevelInformation(
            lowerVertex.floorNumber,
            lowerVertex.shortName,
            lowerVertexOsmId
          ),
          new VertexLevelInformation(
            upperVertex.floorNumber,
            upperVertex.shortName,
            upperVertexOsmId
          )
        )
      );
  }

  @Override
  public void addEdgeInclineInformation(Edge edge, long lowerVertexOsmId, long upperVertexOsmId) {
    Objects.requireNonNull(edge);
    this.edgeInformation.put(edge, new EdgeInclineInformation(lowerVertexOsmId, upperVertexOsmId));
  }

  @Override
  public Optional<EdgeInformation> findEdgeInformation(Edge edge) {
    return Optional.ofNullable(edgeInformation.get(edge));
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultOsmStreetDecoratorRepository.class)
      .addNum("Edges with level information", edgeInformation.size())
      .toString();
  }
}
