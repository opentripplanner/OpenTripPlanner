package org.opentripplanner.transit.transfer.regular.api.parameters;

import java.util.Objects;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * A warmup entry: user preferences that are common in routing requests, but are not worth a
 * discovery search of their own. The Raptor transfers for the preferences are created when the
 * server starts, from the stored paths of the profile with the closest user preferences, like for
 * a request, so the first request with these preferences does not wait for them.
 * <p>
 * A warmup entry is not a profile. No paths are searched for or stored for it.
 *
 * @param id              identifies the entry. It must be unique among all the profiles and
 *                        warmup entries.
 * @param userPreferences the preferences to warm up. They must be unique among the profiles and
 *                        the warmup entries.
 */
public record WarmupParameters(String id, AbstractUserPreferences<?> userPreferences) {
  public WarmupParameters {
    Objects.requireNonNull(id);
    Objects.requireNonNull(userPreferences);
    if (id.isBlank()) {
      throw new IllegalArgumentException("A warmup entry needs an id.");
    }
  }
}
