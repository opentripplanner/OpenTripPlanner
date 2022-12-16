package org.opentripplanner.service.worldenvelope.internal;

import java.io.Serializable;
import java.util.Optional;
import javax.inject.Inject;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;

public class DefaultWorldEnvelopeService implements Serializable, WorldEnvelopeService {

  /**
   * The volatile keyword is key to make this propagate to other threads.
   */
  private WorldEnvelopeRepository repository = null;

  @Inject
  public DefaultWorldEnvelopeService(WorldEnvelopeRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<WorldEnvelope> envelope() {
    return repository.retrieveEnvelope();
  }
}
