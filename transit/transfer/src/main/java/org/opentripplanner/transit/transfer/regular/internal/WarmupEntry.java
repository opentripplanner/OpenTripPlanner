package org.opentripplanner.transit.transfer.regular.internal;

import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * The user preferences to create the Raptor transfers for when the server starts: the
 * preferences of a profile, or of a warmup entry.
 *
 * @param parent the parent profile of a warmup entry, {@code null} for a profile
 */
record WarmupEntry(
  String id,
  @Nullable String parent,
  AbstractUserPreferences<?> userPreferences
) {}
