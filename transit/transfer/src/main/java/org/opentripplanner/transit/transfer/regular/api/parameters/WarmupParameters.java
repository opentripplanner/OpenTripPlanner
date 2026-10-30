package org.opentripplanner.transit.transfer.regular.api.parameters;

import java.util.Objects;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * A warmup entry: user preferences that are common in routing requests, but are not worth a
 * discovery search of their own. The Raptor transfers for the preferences are created from the
 * stored paths of the {@code parent} profile when the server starts, so the first request with
 * these preferences does not wait for them.
 * <p>
 * A warmup entry is not a profile. No paths are searched for or stored for it, and it can not be
 * the parent of a profile. A request with preferences close to the warmup entry uses the paths of
 * the parent, like a request close to the parent.
 *
 * @param id              identifies the entry in the logs. It must be unique among all the
 *                        profiles and warmup entries.
 * @param parent          the id of the profile the Raptor transfers are created from
 * @param userPreferences the preferences to warm up. They must be unique among the profiles and
 *                        the warmup entries.
 */
public record WarmupParameters(
  String id,
  String parent,
  AbstractUserPreferences<?> userPreferences
) {
  public WarmupParameters {
    Objects.requireNonNull(id);
    Objects.requireNonNull(parent);
    Objects.requireNonNull(userPreferences);
    if (id.isBlank()) {
      throw new IllegalArgumentException("A warmup entry needs an id.");
    }
    if (parent.isBlank()) {
      throw new IllegalArgumentException("The warmup entry '" + id + "' needs a parent.");
    }
  }
}
