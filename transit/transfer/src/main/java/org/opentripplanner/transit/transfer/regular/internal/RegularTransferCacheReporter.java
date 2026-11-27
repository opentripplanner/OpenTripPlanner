package org.opentripplanner.transit.transfer.regular.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheReport;
import org.opentripplanner.transit.transfer.regular.api.report.UserPreferenceSource;

/**
 * Where the keys of the cache of the Raptor transfers come from, and how often requests use them.
 * A key is the preferences of a profile, of a warmup entry, or of a request (on demand). The
 * warmup entries are the keys created when the server starts. The usage is counted across all
 * snapshots, see {@link RegularTransferCacheStatistics}.
 * <p>
 * This class is thread-safe.
 */
final class RegularTransferCacheReporter {

  private final List<WarmupEntry> warmup;
  private final Map<AbstractUserPreferences<?>, WarmupEntry> entryByPreferences;
  private final RegularTransferCacheStatistics statistics;

  RegularTransferCacheReporter(
    List<WarmupEntry> warmup,
    RegularTransferCacheStatistics statistics
  ) {
    this.warmup = List.copyOf(warmup);
    var byPreferences = new HashMap<AbstractUserPreferences<?>, WarmupEntry>();
    warmup.forEach(entry -> byPreferences.put(entry.userPreferences(), entry));
    this.entryByPreferences = Map.copyOf(byPreferences);
    this.statistics = statistics;
  }

  /** The keys to create when the server starts: the profiles first, then the warmup entries. */
  List<WarmupEntry> warmup() {
    return warmup;
  }

  /** Count a request that used {@code key}. */
  void record(AbstractUserPreferences<?> key) {
    statistics.record(key);
  }

  /** The report of the keys used, with the profile each key is matched to by {@code matcher}. */
  RegularTransferCacheReport report(
    RegularTransferServiceCache<RaptorTransferStore> cache,
    ProfileMatcher matcher
  ) {
    return () -> statistics.entries(cache::contains, key -> keyInfo(key, matcher));
  }

  private RegularTransferCacheStatistics.KeyInfo keyInfo(
    AbstractUserPreferences<?> key,
    ProfileMatcher matcher
  ) {
    var entry = entryByPreferences.get(key);
    if (entry != null && entry.source() == UserPreferenceSource.PROFILE) {
      return new RegularTransferCacheStatistics.KeyInfo(entry.source(), entry.id(), entry.id());
    }
    var closestProfile = closestProfileOrNull(key, matcher);
    var closest = closestProfile == null ? null : entryByPreferences.get(closestProfile);
    return new RegularTransferCacheStatistics.KeyInfo(
      entry == null ? UserPreferenceSource.ON_DEMAND : entry.source(),
      entry == null ? null : entry.id(),
      closest == null ? null : closest.id()
    );
  }

  @Nullable
  private static AbstractUserPreferences<?> closestProfileOrNull(
    AbstractUserPreferences<?> key,
    ProfileMatcher matcher
  ) {
    try {
      return matcher.closestProfile(key);
    } catch (IllegalArgumentException e) {
      // There is no profile of the type of the key
      return null;
    }
  }
}
