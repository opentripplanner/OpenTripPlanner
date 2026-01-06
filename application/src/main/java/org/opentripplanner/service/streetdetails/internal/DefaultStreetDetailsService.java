package org.opentripplanner.service.streetdetails.internal;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.model.edge.Edge;

public class DefaultStreetDetailsService implements StreetDetailsService {

  private final StreetDetailsRepository repository;

  @Inject
  public DefaultStreetDetailsService(StreetDetailsRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<InclinedEdgeLevelInfo> findInclinedEdgeLevelInfo(Edge edge) {
    return repository.findInclinedEdgeLevelInfo(edge);
  }

  @Override
  public Optional<Level> findHorizontalEdgeLevelInfo(Edge edge) {
    return repository.findHorizontalEdgeLevelInfo(edge);
  }

  @Override
  public String toString() {
    return "DefaultStreetDetailsService{ repository=" + repository + '}';
  }
}
