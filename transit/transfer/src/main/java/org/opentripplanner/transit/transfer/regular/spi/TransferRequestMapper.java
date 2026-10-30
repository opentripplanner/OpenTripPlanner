package org.opentripplanner.transit.transfer.regular.spi;

import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * Maps between the {@link AbstractUserPreferences user preferences} the regular-transfer module
 * stores and caches transfers under, and the request {@code R} the {@link TransferPathProvider}
 * searches with. {@code R} stays opaque to the module, this mapper is the only place that knows
 * how the two relate.
 * <p>
 * Implementations are immutable and thread-safe.
 *
 * @param <R> the request type
 */
public interface TransferRequestMapper<R> {
  /**
   * Create the request to search and re-cost paths with: {@code base} with {@code userPreferences}
   * applied on top. The preferences override the equivalent values of the base request. Used when
   * the transfers are generated, and when the Raptor transfers are derived from the stored paths.
   */
  R toRequest(AbstractUserPreferences<?> userPreferences, R base);

  /**
   * The user preferences of a request: the key the Raptor transfers of the request are looked up
   * under at routing time. The {@link TransferProfileType} of the preferences is decided by the
   * request, a request that has no profile type is not accepted.
   *
   * @throws IllegalArgumentException if the request has no profile type
   */
  AbstractUserPreferences<?> toUserPreferences(R request);

  /**
   * The user preferences of the given {@code type}, with the values of the request. Used for a
   * profile without preferences of its own, where the type is the type of the profile.
   */
  AbstractUserPreferences<?> toUserPreferences(TransferProfileType type, R request);
}
