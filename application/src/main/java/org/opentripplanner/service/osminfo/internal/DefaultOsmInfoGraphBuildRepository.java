package org.opentripplanner.service.osminfo.internal;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class DefaultOsmInfoGraphBuildRepository
  implements OsmInfoGraphBuildRepository, Serializable {

  private final Map<Edge, Platform> platforms = new HashMap<>();
  private final Map<Area, Platform> areaPlatforms = new HashMap<>();
  private final Set<TurnRestriction> turnRestrictions = new HashSet<>();

  @Inject
  public DefaultOsmInfoGraphBuildRepository() {}

  @Override
  public void addPlatform(Edge edge, Platform platform) {
    Objects.requireNonNull(edge);
    Objects.requireNonNull(platform);
    this.platforms.put(edge, platform);
  }

  @Override
  public void addPlatform(Area area, Platform platform) {
    Objects.requireNonNull(area);
    Objects.requireNonNull(platform);
    this.areaPlatforms.put(area, platform);
  }

  @Override
  public void addTurnRestriction(TurnRestriction turnRestriction) {
    Objects.requireNonNull(turnRestriction);
    this.turnRestrictions.add(turnRestriction);
  }

  @Override
  public Optional<Platform> findPlatform(Edge edge) {
    return Optional.ofNullable(platforms.get(edge));
  }

  @Override
  public Optional<Platform> findPlatform(Area area) {
    return Optional.ofNullable(areaPlatforms.get(area));
  }

  @Override
  public Collection<TurnRestriction> listTurnRestrictions() {
    return turnRestrictions;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DefaultOsmInfoGraphBuildRepository.class)
      .addNum("Linear platforms", platforms.size())
      .addNum("Area platforms", areaPlatforms.size())
      .toString();
  }
}
