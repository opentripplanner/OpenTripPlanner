package org.opentripplanner.service.osminfo.internal;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.model.OsmWayReferences;
import org.opentripplanner.street.model.edge.Edge;

public class DefaultOsmInfoGraphBuildRepository
  implements OsmInfoGraphBuildRepository, Serializable {

  private final Map<Edge, OsmWayReferences> references = new HashMap<>();

  @Inject
  public DefaultOsmInfoGraphBuildRepository() {}

  @Override
  public void addReferences(Edge edge, OsmWayReferences info) {
    Objects.requireNonNull(edge);
    Objects.requireNonNull(info);
    this.references.put(edge, info);
  }

  @Override
  public Optional<OsmWayReferences> findReferences(Edge edge) {
    return Optional.ofNullable(references.get(edge));
  }

  @Override
  public String toString() {
    return "DefaultOsmInfoGraphBuildRepository{references size = " + references.size() + "}";
  }
}
