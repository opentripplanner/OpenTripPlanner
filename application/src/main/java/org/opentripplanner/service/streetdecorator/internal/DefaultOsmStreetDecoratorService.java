package org.opentripplanner.service.streetdecorator.internal;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.service.streetdecorator.OsmStreetDecoratorRepository;
import org.opentripplanner.service.streetdecorator.OsmStreetDecoratorService;
import org.opentripplanner.service.streetdecorator.model.EdgeInformation;
import org.opentripplanner.street.model.edge.Edge;

public class DefaultOsmStreetDecoratorService implements OsmStreetDecoratorService {

  private final OsmStreetDecoratorRepository repository;

  @Inject
  public DefaultOsmStreetDecoratorService(OsmStreetDecoratorRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<EdgeInformation> findEdgeInformation(Edge edge) {
    return repository.findEdgeInformation(edge);
  }

  @Override
  public String toString() {
    return "DefaultOsmStreetDecoratorService{ repository=" + repository + '}';
  }
}
