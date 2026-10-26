package org.opentripplanner.transit.transfer.regular.api;

import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

/**
 * One key of the cache of the Raptor transfers, a normalized user preferences, in the
 * {@link RegularTransferCacheReport}.
 *
 * @param type              the profile type of the preferences
 * @param kind              how the key came to be, see {@link Kind}
 * @param id                the id of the profile or the warmup entry, {@code null} if the key was
 *                          created on demand
 * @param closestProfileId  the id of the profile closest to the key, the paths of the transfers
 *                          are read from. {@code null} if there is no profile
 * @param userPreferences   the user preferences of the key, as text
 * @param hits              the number of requests that used the key
 * @param cached            whether the Raptor transfers of the key are in the cache now
 */
public record TransferCacheReportEntry(
  TransferProfileType type,
  Kind kind,
  @Nullable String id,
  @Nullable String closestProfileId,
  String userPreferences,
  long hits,
  boolean cached
) {
  /** How a key of the cache came to be. */
  public enum Kind {
    /** The preferences of a profile, the Raptor transfers are created when the server starts. */
    PROFILE,
    /** The preferences of a warmup entry, created when the server starts. */
    WARMUP,
    /** The preferences of a request that is not a profile or a warmup entry. */
    ON_DEMAND,
  }
}
