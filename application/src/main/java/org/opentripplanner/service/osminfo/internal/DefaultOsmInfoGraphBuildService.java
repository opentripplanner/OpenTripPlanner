package org.opentripplanner.service.osminfo.internal;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildService;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.edge.Edge;

public class DefaultOsmInfoGraphBuildService implements OsmInfoGraphBuildService {

  private final OsmInfoGraphBuildRepository repository;

  @Inject
  public DefaultOsmInfoGraphBuildService(OsmInfoGraphBuildRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Platform> findPlatform(Edge edge) {
    return repository.findPlatform(edge);
  }

  @Override
  public String toString() {
    return "DefaultOsmInfoGraphBuildService{ repository=" + repository + '}';
  }
}
