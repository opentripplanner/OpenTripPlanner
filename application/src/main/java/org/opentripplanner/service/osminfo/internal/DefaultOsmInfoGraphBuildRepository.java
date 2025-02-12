package org.opentripplanner.service.osminfo.internal;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.NamedArea;

public class DefaultOsmInfoGraphBuildRepository
  implements OsmInfoGraphBuildRepository, Serializable {

  private final Map<Edge, Platform> platforms = new HashMap<>();
  private final Map<NamedArea, Platform> areaPlatforms = new HashMap<>();

  @Inject
  public DefaultOsmInfoGraphBuildRepository() {}

  @Override
  public void addPlatform(Edge edge, Platform platform) {
    Objects.requireNonNull(edge);
    Objects.requireNonNull(platform);
    this.platforms.put(edge, platform);
  }

  @Override
  public void addPlatform(NamedArea area, Platform platform) {
    Objects.requireNonNull(area);
    Objects.requireNonNull(platform);
    this.areaPlatforms.put(area, platform);
  }

  @Override
  public Optional<Platform> findPlatform(Edge edge) {
    return Optional.ofNullable(platforms.get(edge));
  }

  @Override
  public Optional<Platform> findPlatform(NamedArea area) {
    return Optional.ofNullable(areaPlatforms.get(area));
  }

  @Override
  public String toString() {
    return (
      "DefaultOsmInfoGraphBuildRepository{platforms size = " +
      platforms.size() +
      areaPlatforms.size() +
      "}"
    );
  }
}
