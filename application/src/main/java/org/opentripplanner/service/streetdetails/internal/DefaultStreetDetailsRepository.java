package org.opentripplanner.service.streetdetails.internal;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class DefaultStreetDetailsRepository implements StreetDetailsRepository, Serializable {

  private final Map<Edge, InclinedEdgeLevelInfo> inclinedLevelInformation = new HashMap<>();
  private final Map<Edge, Level> horizontalLevelInformation = new HashMap<>();

  @Inject
  public DefaultStreetDetailsRepository() {}

  @Override
  public void addInclinedEdgeLevelInfo(Edge edge, InclinedEdgeLevelInfo inclinedEdgeLevelInfo) {
    Objects.requireNonNull(edge);
    Objects.requireNonNull(inclinedEdgeLevelInfo);
    this.inclinedLevelInformation.put(edge, inclinedEdgeLevelInfo);
  }

  @Override
  public Optional<InclinedEdgeLevelInfo> findInclinedEdgeLevelInfo(Edge edge) {
    return Optional.ofNullable(inclinedLevelInformation.get(edge));
  }

  @Override
  public void addHorizontalEdgeLevelInfo(Edge edge, Level level) {
    Objects.requireNonNull(edge);
    Objects.requireNonNull(level);
    this.horizontalLevelInformation.put(edge, level);
  }

  @Override
  public Optional<Level> findHorizontalEdgeLevelInfo(Edge edge) {
    return Optional.ofNullable(horizontalLevelInformation.get(edge));
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultStreetDetailsRepository.class)
      .addNum("Inclined edges with level info", inclinedLevelInformation.size())
      .addNum("Horizontal edges with level info", horizontalLevelInformation.size())
      .toString();
  }
}
