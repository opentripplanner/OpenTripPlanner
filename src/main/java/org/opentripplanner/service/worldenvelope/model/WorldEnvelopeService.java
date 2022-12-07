package org.opentripplanner.service.worldenvelope.model;

import java.util.Optional;

/**
 * This service provide information about the geographical bounding-box and center coordinates
 * for OTP. The information is computed once, based on static data and returned in the
 * {@link WorldEnvelope}.
 */
public interface WorldEnvelopeService {
  /**
   * The envelope hold all information about the OTP street and transit geographical
   * bounding-box and center coordinates(two options).
   * <p>
   * If no envelope is created, this return empty.
   */
  Optional<WorldEnvelope> envelope();
}
