package org.opentripplanner.service.shape.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A map whose entries are grouped by feed, so that all entries of a feed can be removed at once,
 * and which can be frozen into immutable copies cheaply.
 * <p>
 * A mutable instance is only used on the single writer thread. It keeps its own mutable maps and
 * tracks the feeds that were changed since the latest {@link #freeze()} (the dirty feeds).
 * Freezing copies only the dirty feeds; the immutable maps of the unchanged feeds are shared with
 * the previous frozen instance. The cost of a freeze is therefore proportional to the number of
 * entries in the feeds changed since the previous one, and writing never copies anything.
 *
 * @param <K> the key, which belongs to exactly one feed
 * @param <V> the value
 */
final class FeedPartitionedMap<K, V> {

  private final Map<String, Map<K, V>> valuesByKeyByFeed;
  private final Map<String, Map<K, V>> frozenValuesByKeyByFeed = new HashMap<>();
  private final Set<String> dirtyFeeds = new HashSet<>();

  private final boolean readOnly;

  FeedPartitionedMap() {
    this(new HashMap<>(), false);
  }

  private FeedPartitionedMap(Map<String, Map<K, V>> valuesByKeyByFeed, boolean readOnly) {
    this.valuesByKeyByFeed = valuesByKeyByFeed;
    this.readOnly = readOnly;
  }

  static <K, V> FeedPartitionedMap<K, V> empty() {
    return new FeedPartitionedMap<>(Map.of(), true);
  }

  /**
   * The value for the given key, or {@code null} if there is none.
   *
   * @param feedId the id of the feed the key belongs to
   */
  @Nullable
  V get(String feedId, K key) {
    var valuesByKey = valuesByKeyByFeed.get(feedId);
    if (valuesByKey == null) {
      return null;
    }
    return valuesByKey.get(key);
  }

  /**
   * Store the value for the given key, replacing any existing one.
   *
   * @param feedId the id of the feed the key belongs to
   */
  void put(String feedId, K key, V value) {
    validateNotReadOnly();
    valuesByKeyByFeed.computeIfAbsent(feedId, k -> new HashMap<>()).put(key, value);
    dirtyFeeds.add(feedId);
  }

  /**
   * Remove all entries of the given feed. A feed without entries is not marked as dirty.
   */
  void clear(String feedId) {
    validateNotReadOnly();
    if (valuesByKeyByFeed.remove(feedId) != null) {
      dirtyFeeds.add(feedId);
    }
  }

  /**
   * Create a read-only copy of the current state. Later changes to this instance are not visible in
   * the copy.
   */
  FeedPartitionedMap<K, V> freeze() {
    validateNotReadOnly();
    for (var feedId : dirtyFeeds) {
      var valuesByKey = valuesByKeyByFeed.get(feedId);
      if (valuesByKey == null) {
        frozenValuesByKeyByFeed.remove(feedId);
      } else {
        frozenValuesByKeyByFeed.put(feedId, Map.copyOf(valuesByKey));
      }
    }
    dirtyFeeds.clear();
    return new FeedPartitionedMap<>(Map.copyOf(frozenValuesByKeyByFeed), true);
  }

  private void validateNotReadOnly() {
    if (readOnly) {
      throw new IllegalStateException("A read-only FeedPartitionedMap can not be modified");
    }
  }
}
