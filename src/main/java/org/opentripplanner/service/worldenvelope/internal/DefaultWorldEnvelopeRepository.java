package org.opentripplanner.service.worldenvelope.internal;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;

@Singleton
public class DefaultWorldEnvelopeRepository implements WorldEnvelopeRepository, Serializable {

  /**
   * The volatile keyword is used to eventually propagate to other threads when updated.
   * This is thread-safe, because we can use an old instance for a while - it does not hurt.
   */
  private volatile WorldEnvelope envelope = null;

  @Inject
  public DefaultWorldEnvelopeRepository() {}

  @Override
  public Optional<WorldEnvelope> retrieveEnvelope() {
    return Optional.ofNullable(envelope);
  }

  @Override
  public void saveEnvelope(@Nonnull WorldEnvelope envelope) {
    this.envelope = envelope;
  }
}
