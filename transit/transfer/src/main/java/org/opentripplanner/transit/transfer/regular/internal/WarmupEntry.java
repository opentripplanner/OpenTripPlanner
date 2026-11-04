package org.opentripplanner.transit.transfer.regular.internal;

import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.report.UserPreferenceSource;

/**
 * The user preferences to create the Raptor transfers for when the server starts: the
 * preferences of a profile, or of a warmup entry.
 *
 * @param source {@link UserPreferenceSource#PROFILE} or {@link UserPreferenceSource#WARMUP}
 */
record WarmupEntry(
  String id,
  UserPreferenceSource source,
  AbstractUserPreferences<?> userPreferences
) {}
