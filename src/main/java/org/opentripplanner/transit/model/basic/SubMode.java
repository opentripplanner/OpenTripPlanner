package org.opentripplanner.transit.model.basic;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps a string and creates an index for it, so we can use a BitSet for matching a
 * trip netex SubMode. SubModes are used in trip filtering for every request.
 * <p>
 * Naming; This class is named SubMode, not NetexSubMode because we want to migrate gtfsRouteType
 * into the same concept.
 * <p>
 * This class is thread-safe, and the performance overhead should affect the graph built time,
 * but not routing.
 */
public record SubMode(String name, int index) implements Serializable {
  private static final int NON_EXISTING_SUB_MODE_INDEX = 1_000_000;

  private static final Logger LOG = LoggerFactory.getLogger(SubMode.class);

  /**
   * Note! This caches all sub-modes used in the static scheduled transit data. When
   * serializing the static fields need to be explicitly serialized, it is not enough
   * to serialize the instances.
   */
  private static final Map<String, SubMode> ALL = new ConcurrentHashMap<>();
  private static final AtomicInteger COUNTER = new AtomicInteger(0);

  public static final SubMode UNKNOWN = getOrBuildAndCacheForever("unknown");

  /**
   * This method is safe to use in a request scope. Usually you want to fetch an instant to
   * pass in as a request parameter. This method will fetch an existing object - not creating
   * a duplicate object, if at least one Route, Trip or Stop has this submode. If it does not
   * exist in the model a new is created, but not put on the "deduplication" map.
   */
  public static SubMode of(String name) {
    if (name == null) {
      return UNKNOWN;
    }
    var subMode = ALL.get(name);
    return subMode != null ? subMode : new SubMode(name, NON_EXISTING_SUB_MODE_INDEX);
  }

  /**
   * Make sure to use this during graph build or when creating new Trips in realTime
   * updates. Do NOT use this in an OTP routing request - that will lead to memory leaks.
   * <p>
   * The builders in the transit model take care of calling this method, so there is no
   * reason to call this method outside the transit model package.
   */
  public static SubMode getOrBuildAndCacheForever(String name) {
    if (name == null) {
      return UNKNOWN;
    }
    if (ALL.size() == 1000) {
      LOG.error("There are 1000 subModes in use, there might be a memory leak.");
    }
    return ALL.computeIfAbsent(name, it -> new SubMode(it, COUNTER.getAndIncrement()));
  }

  /**
   * Return all SubModes besed on the BitSet of SubMode indexes.
   */
  public static Set<SubMode> getByIndex(BitSet subModes) {
    var set = new HashSet<SubMode>();
    for (SubMode value : ALL.values()) {
      if (subModes.get(value.index())) {
        set.add(value);
      }
    }
    return set;
  }

  public static List<SubMode> listAllCachedSubModes() {
    return List.copyOf(ALL.values());
  }

  public static void deserializeSubModeCache(Collection<SubMode> subModes) {
    int maxIndex = 0;
    for (SubMode it : subModes) {
      ALL.put(it.name(), it);
      maxIndex = Math.max(maxIndex, it.index);
    }
    COUNTER.set(maxIndex + 1);
  }

  /**
   * Only name is used in the equals, not index, so we need to override the
   * default record implementation of equals.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var other = (SubMode) o;
    return name.equals(other.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name();
  }
}
