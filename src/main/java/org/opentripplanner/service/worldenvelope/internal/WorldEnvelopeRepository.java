package org.opentripplanner.service.worldenvelope.internal;

import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;

/**
 * Provide a container for the envelope. This is used set and pass the immutable envelope into the
 * application context.
 * <p>
 * The envelope is bound to the model at graph build time. Ba aware
 * that, before that the envelope is not set, and accessing it will cause
 * a null pointer exception.
 * <p>
 * This class serve both the role as model and service.
 * <ol>
 *   <li>As model it provide a place to store and update the envelope</li>
 *   <li>
 *     As service it provide read-only access the the envelope for APIs. It implements the
 *     {@link WorldEnvelopeService} to provide this role.
 *   </li>
 * </ol>
 * <p>
 * THIS CLASS IS THREAD-SAFE.
 */
public class WorldEnvelopeRepository implements Serializable, WorldEnvelopeService {

  /**
   * The volatile keyword is key to make this propagate to other threads.
   */
  private volatile WorldEnvelope envelope = null;

  @Inject
  public WorldEnvelopeRepository() {}

  @Override
  public Optional<WorldEnvelope> envelope() {
    return Optional.ofNullable(envelope);
  }

  public void setEnvelope(@Nonnull WorldEnvelope envelope) {
    this.envelope = envelope;
  }
}
