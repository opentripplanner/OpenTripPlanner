package org.opentripplanner.transit.model.network;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wrap a string and create an index for it, so we can use BitSet for matching a
 * trip netexSubMode. SubModes are used in trip filtering for every request.
 * <p>
 * Naming, this class is named SubMode, not NetexSubMode because we want to migrate gtfsRouteType
 * into the same concept.
 * <p>
 * This class is thread-safe, and the performance overhead should affect the graph built time,
 * but not routing.
 */
public class SubMode implements Serializable {

  private static final int NONE_EXISTING_SUBMODE_INDEX = 1_000_000;

  private static final Logger LOG = LoggerFactory.getLogger(SubMode.class);
  private static final AtomicInteger COUNTER = new AtomicInteger(0);
  private static final Map<String, SubMode> ALL = new ConcurrentHashMap<>();

  public static final SubMode UNKNOWN = getOrBuildAndCashForever("unknown");

  private final String name;
  private final int index;

  private SubMode(String name, int index) {
    this.name = name;
    this.index = index;
  }

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
    return ALL.computeIfAbsent(name, n -> new SubMode(n, NONE_EXISTING_SUBMODE_INDEX));
  }

  /**
   * Make sure to use this during graph build or creating new Trips in realTime updates. Do NOT
   * use this in an OTP routing request - that may lead to memory leaks.
   * <p>
   * The builders in the transit model take care of calling this method, so there is noreson to
   * call this method outside the transit model package.
   */
  public static SubMode getOrBuildAndCashForever(String name) {
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

  public String name() {
    return name;
  }

  public int index() {
    return index;
  }

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
    return index;
  }

  @Override
  public String toString() {
    return name();
  }
}
