package org.opentripplanner.ext.carpooling.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Keeps at most a configured number of carpool access/egress candidates per transit stop.
 * <p>
 * The evaluator yields one candidate per trip and corridor stop, and Raptor keeps every candidate
 * with opening hours and checks each once per minute of the search window. A stop with few cars
 * hands all of them to Raptor. A stop with more than the cap keeps the first and the last car of
 * the window, the first car after the window, and one car per slot in between, the slots cut from
 * the window so that the total stays within the cap.
 * <p>
 * A candidate is a fixed-time leg with one end pinned by the request: for a depart-after request
 * the passenger cannot leave before the requested time, so the departure (from the origin for an
 * access, from the stop for an egress) is pinned; an arrive-by request pins the arrival. A
 * candidate whose pinned end precedes the request is dropped, Raptor could never use it. Within a
 * slot an <b>access</b> keeps the car reaching the stop earliest, which catches every bus a later
 * one catches, and an <b>egress</b> keeps the car leaving the stop latest, which any bus arriving
 * in the slot before it can catch; either way the avoidable waiting is bounded by one slot. An
 * arrive-by request mirrors the time axis, so the egress plays the access's part and the access
 * the egress's.
 * <p>
 * Not thread-safe; one instance per request.
 *
 * @param <T> the candidate type
 */
public final class PerStopCandidateCap<T> {

  private final int maxPerStop;
  /** Window slots plus the overflow slot for candidates past the window. */
  private final int slots;
  private final long slotSeconds;
  /** The access of a depart-after request or the egress of an arrive-by one: keeps the earliest free end. */
  private final boolean earliestFreeEnd;
  private final int sign;
  private final long anchor;
  private final long windowEnd;
  private final Map<Integer, StopCandidates> stops = new HashMap<>();
  private int added;
  private int unusable;

  /**
   * @param maxPerStop the most candidates a stop hands to Raptor, at least 4
   * @param access {@code true} for access candidates, {@code false} for egress
   * @param requestTime the requested departure (depart-after) or arrival (arrive-by) time
   * @param windowSeconds the request's search window, at least 1
   */
  public PerStopCandidateCap(
    int maxPerStop,
    boolean access,
    boolean arriveBy,
    int requestTime,
    int windowSeconds
  ) {
    if (maxPerStop < 4 || windowSeconds < 1) {
      throw new IllegalArgumentException(
        "maxPerStop must be at least 4 and windowSeconds positive"
      );
    }
    this.maxPerStop = maxPerStop;
    // The first and the last car take two places, the rest are slots, the last of them overflow.
    this.slots = maxPerStop - 2;
    this.slotSeconds = Math.ceilDiv(windowSeconds, slots - 1);
    this.earliestFreeEnd = access != arriveBy;
    this.sign = arriveBy ? -1 : 1;
    this.anchor = (long) sign * requestTime;
    this.windowEnd = anchor + windowSeconds;
  }

  /**
   * Offers a candidate.
   *
   * @param stop the candidate's transit stop index
   * @param departure when the passenger leaves, in the request time's unit
   * @param arrival when the passenger arrives
   */
  public void add(T candidate, int stop, int departure, int arrival) {
    added++;
    // Oriented so that time runs forward from the request in both directions.
    long pinned = sign > 0 ? departure : -((long) arrival);
    long free = sign > 0 ? arrival : -((long) departure);
    if (pinned < anchor) {
      unusable++;
      return;
    }
    stops
      .computeIfAbsent(stop, s -> new StopCandidates())
      .add(new Entry<>(candidate, pinned, free));
  }

  /** The surviving candidates, in no particular order. */
  public List<T> kept() {
    var out = new ArrayList<T>();
    for (var stop : stops.values()) {
      for (var entry : stop.kept()) {
        out.add(entry.value);
      }
    }
    return out;
  }

  /** Candidates offered so far. */
  public int added() {
    return added;
  }

  /** Candidates dropped because their pinned end precedes the request. */
  public int unusable() {
    return unusable;
  }

  /** Whether {@code entry} replaces {@code other} in the same slot. */
  private boolean beats(Entry<T> entry, Entry<T> other, boolean overflowSlot) {
    if (earliestFreeEnd) {
      return entry.free < other.free || (entry.free == other.free && entry.pinned > other.pinned);
    }
    if (overflowSlot) {
      return (
        entry.pinned < other.pinned || (entry.pinned == other.pinned && entry.free < other.free)
      );
    }
    return entry.pinned > other.pinned || (entry.pinned == other.pinned && entry.free < other.free);
  }

  /** The candidates of one stop: all of them while within the cap, slotted once over it. */
  private final class StopCandidates {

    @Nullable
    private List<Entry<T>> all = new ArrayList<>();

    @Nullable
    private Entry<T>[] slotted;

    @Nullable
    private Entry<T> first;

    @Nullable
    private Entry<T> last;

    @SuppressWarnings("unchecked")
    void add(Entry<T> entry) {
      if (all == null) {
        slot(entry);
        return;
      }
      all.add(entry);
      if (all.size() > maxPerStop) {
        var buffered = all;
        all = null;
        slotted = (Entry<T>[]) new Entry[slots];
        buffered.forEach(this::slot);
      }
    }

    private void slot(Entry<T> entry) {
      if (first == null || entry.pinned < first.pinned) {
        first = entry;
      }
      if (entry.pinned <= windowEnd && (last == null || entry.pinned > last.pinned)) {
        last = entry;
      }
      int i = (int) Math.min(slots - 1, (entry.pinned - anchor) / slotSeconds);
      if (slotted[i] == null || beats(entry, slotted[i], i == slots - 1)) {
        slotted[i] = entry;
      }
    }

    List<Entry<T>> kept() {
      if (all != null) {
        return all;
      }
      var out = new LinkedHashSet<Entry<T>>();
      out.add(first);
      if (last != null) {
        out.add(last);
      }
      for (var entry : slotted) {
        if (entry != null) {
          out.add(entry);
        }
      }
      return new ArrayList<>(out);
    }
  }

  private record Entry<T>(T value, long pinned, long free) {}
}
