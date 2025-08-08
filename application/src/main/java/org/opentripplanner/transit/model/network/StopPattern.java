package org.opentripplanner.transit.model.network;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.lang.MemEfficientArrayBuilder;

/**
 * This class represents what is called a JourneyPattern in Transmodel: the sequence of stops at
 * which a trip (GTFS) or vehicle journey (Transmodel) calls, irrespective of the day on which
 * service runs.
 * <p>
 * An important detail: Routes in GTFS are not a structurally important element, they just serve as
 * user-facing information. It is possible for the same journey pattern to appear in more than one
 * route.
 * <p>
 * OTP already has several classes that represent this same thing: A TripPattern in the context of
 * routing. It represents all trips with the same stop pattern A ScheduledStopPattern in the GTFS
 * loading process. A RouteVariant in the TransitIndex, which has a unique human-readable name and
 * belongs to a particular route.
 * <p>
 * We would like to combine all these different classes into one.
 * <p>
 * Any two trips with the same stops in the same order, and that operate on the same days, can be
 * combined using a TripPattern to simplify the graph. This saves memory and reduces search
 * complexity since we only consider the trip that departs soonest for each pattern. Field
 * calendarId has been removed. See issue #1320.
 * <p>
 * A StopPattern is very closely related to a TripPattern -- it essentially serves as the unique key
 * for a TripPattern. Should the route be included in the StopPattern?
 */
public final class StopPattern implements Serializable {

  private final StopLocation[] stops;
  private final PickDrop[] pickups;
  private final PickDrop[] dropoffs;

  private StopPattern(int size) {
    stops = new StopLocation[size];
    pickups = new PickDrop[size];
    dropoffs = new PickDrop[size];
  }

  private StopPattern(StopLocation[] stops, PickDrop[] pickups, PickDrop[] dropoffs) {
    this.stops = stops;
    this.pickups = pickups;
    this.dropoffs = dropoffs;
  }

  /** Assumes that stopTimes are already sorted by time. */
  public StopPattern(Collection<StopTime> stopTimes) {
    this(stopTimes.size());
    int size = stopTimes.size();
    if (size == 0) return;
    Iterator<StopTime> stopTimeIterator = stopTimes.iterator();

    for (int i = 0; i < size; ++i) {
      StopTime stopTime = stopTimeIterator.next();
      stops[i] = stopTime.getStop();
      // should these just be booleans? anything but 1 means pick/drop is allowed.
      // pick/drop messages could be stored in individual trips
      pickups[i] = computePickDrop(stopTime.getStop(), stopTime.getPickupType());
      dropoffs[i] = computePickDrop(stopTime.getStop(), stopTime.getDropOffType());
    }
  }

  /**
   * For creating StopTimes without StopTime, for example for unit testing.
   */
  public static StopPatternBuilder create(int length) {
    return new StopPatternBuilder(new StopPattern(length), null);
  }

  /**
   * This has package local access since a StopPattern is a part of a TripPattern. To change it
   * use the {@link TripPattern#copyPlannedStopPattern()} method.
   */
  StopPatternBuilder copyOf() {
    return new StopPatternBuilder(this, null);
  }

  StopPatternBuilder copyOf(StopPattern realTime) {
    return new StopPatternBuilder(this, realTime);
  }

  public int hashCode() {
    int hash = stops.length;
    hash += Arrays.hashCode(this.stops);
    hash *= 31;
    hash += Arrays.hashCode(this.pickups);
    hash *= 31;
    hash += Arrays.hashCode(this.dropoffs);
    return hash;
  }

  public boolean equals(Object other) {
    if (other instanceof StopPattern) {
      StopPattern that = (StopPattern) other;
      return (
        Arrays.equals(this.stops, that.stops) &&
        Arrays.equals(this.pickups, that.pickups) &&
        Arrays.equals(this.dropoffs, that.dropoffs)
      );
    } else {
      return false;
    }
  }

  /**
   * Checks that stops equal without taking into account if pickup or dropoff is allowed.
   */
  public boolean stopsEqual(Object other) {
    if (other instanceof StopPattern that) {
      return Arrays.equals(this.stops, that.stops);
    } else {
      return false;
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("StopPattern: ");
    for (int i = 0, j = stops.length; i < j; ++i) {
      sb.append(String.format("%s_%s%s ", stops[i].getCode(), pickups[i], dropoffs[i]));
    }
    return sb.toString();
  }

  public int getSize() {
    return stops.length;
  }

  /**
   * Checks that all stops ar non-routable.
   */
  public boolean isAllStopsNonRoutable() {
    return (
      Arrays.stream(pickups).allMatch(PickDrop::isNotRoutable) &&
      Arrays.stream(dropoffs).allMatch(PickDrop::isNotRoutable)
    );
  }

  /** Find the given stop position in the sequence, return -1 if not found. */
  int findStopPosition(StopLocation stop) {
    for (int i = 0; i < stops.length; ++i) {
      if (stops[i] == stop) {
        return i;
      }
    }
    return -1;
  }

  int findBoardingPosition(StopLocation stop) {
    return findStopPosition(0, stops.length - 1, s -> s == stop);
  }

  int findAlightPosition(StopLocation stop) {
    return findStopPosition(1, stops.length, s -> s == stop);
  }

  int findBoardingPosition(Station station) {
    return findStopPosition(0, stops.length - 1, station::includes);
  }

  int findAlightPosition(Station station) {
    return findStopPosition(1, stops.length, station::includes);
  }

  /** Get a copy of the internal collection of stops. */
  List<StopLocation> getStops() {
    return List.of(stops);
  }

  public StopLocation getStop(int stopPosInPattern) {
    return stops[stopPosInPattern];
  }

  PickDrop getPickup(int stopPosInPattern) {
    return pickups[stopPosInPattern];
  }

  PickDrop getDropoff(int stopPosInPattern) {
    return dropoffs[stopPosInPattern];
  }

  /** Returns whether passengers can alight at a given stop */
  boolean canAlight(int stopPosInPattern) {
    return dropoffs[stopPosInPattern].isRoutable();
  }

  /**
   * Returns whether passengers can alight at a given stop. This is an inefficient method iterating
   * over the stops, do not use it in routing.
   */
  boolean canAlight(StopLocation stop) {
    // We skip the last stop, not allowed for boarding
    for (int i = 0; i < stops.length - 1; ++i) {
      if (stop == stops[i] && canAlight(i)) {
        return true;
      }
    }
    return false;
  }

  /** Returns whether passengers can board at a given stop */
  boolean canBoard(int stopPosInPattern) {
    return pickups[stopPosInPattern].isRoutable();
  }

  /**
   * Returns whether passengers can board at a given stop. This is an inefficient method iterating
   * over the stops, do not use it in routing.
   */
  boolean canBoard(StopLocation stop) {
    // We skip the last stop, not allowed for boarding
    for (int i = 0; i < stops.length - 1; ++i) {
      if (stop == stops[i] && canBoard(i)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Raptor should not be allowed to board or alight flex stops because they have fake coordinates
   * (centroids) and might not have times.
   */
  private static PickDrop computePickDrop(StopLocation stop, PickDrop pickDrop) {
    if (stop instanceof RegularStop) {
      return pickDrop;
    } else {
      return PickDrop.NONE;
    }
  }

  /**
   * Find the given stop position in the sequence according to match Predicate, return -1 if not
   * found.
   */
  private int findStopPosition(
    final int start,
    final int end,
    final Predicate<StopLocation> match
  ) {
    for (int i = start; i < end; ++i) {
      if (match.test(stops[i])) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Check if given stop and next stop on this stop pattern and other are equal.
   *
   * @param other Other instance of stop pattern with list of stops.
   * @param index Given index for stop
   * @return true if stop and next stop are equal on both stop patterns, else false
   */
  boolean sameStops(StopPattern other, int index) {
    var otherOrigin = other.getStop(index);
    var otherDestination = other.getStop(index + 1);
    var origin = getStop(index);
    var destination = getStop(index + 1);

    return origin.equals(otherOrigin) && destination.equals(otherDestination);
  }

  /**
   * Check if Station is equal on given stop and next stop for this trip pattern and other.
   *
   * @param other Other instance of stop pattern with list of stops.
   * @param index Given index for stop
   * @return true if the stops have the same stations, else false. If any station is null then
   * false.
   */
  boolean sameStations(StopPattern other, int index) {
    var otherOrigin = other.getStop(index).getParentStation();
    var otherDestination = other.getStop(index + 1).getParentStation();
    var origin = getStop(index).getParentStation();
    var destionation = getStop(index + 1).getParentStation();

    var sameOrigin = Optional.ofNullable(origin)
      .map(o -> o.equals(otherOrigin))
      .orElse(getStop(index).equals(other.getStop(index)));

    var sameDestination = Optional.ofNullable(destionation)
      .map(o -> o.equals(otherDestination))
      .orElse(getStop(index + 1).equals(other.getStop(index + 1)));

    return sameOrigin && sameDestination;
  }

  public static class StopPatternBuilder {

    public final MemEfficientArrayBuilder<StopLocation> stops;
    public final MemEfficientArrayBuilder<PickDrop> pickups;
    public final MemEfficientArrayBuilder<PickDrop> dropoffs;
    private final StopPattern original;

    @Nullable
    private final StopPattern realTime;

    public StopPatternBuilder(StopPattern original, StopPattern realTime) {
      stops = MemEfficientArrayBuilder.of(original.stops);
      pickups = MemEfficientArrayBuilder.of(original.pickups);
      dropoffs = MemEfficientArrayBuilder.of(original.dropoffs);
      this.original = original;
      this.realTime = realTime;
    }

    /**
     * Updates the pickup types at given stop indices.
     *
     * @return StopPatternBuilder
     */
    public StopPatternBuilder updatePickups(Map<Integer, PickDrop> updatedPickups) {
      for (var entry : updatedPickups.entrySet()) {
        pickups.with(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Updates the pickup types at given stop indices.
     *
     * @return StopPatternBuilder
     */
    public StopPatternBuilder updateDropoffs(Map<Integer, PickDrop> updatedDropoffs) {
      for (var entry : updatedDropoffs.entrySet()) {
        dropoffs.with(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Replace the stop {@code old} in the stop pattern with ${code newStop}.
     */
    public StopPatternBuilder replaceStop(FeedScopedId old, StopLocation newStop) {
      Objects.requireNonNull(old);
      Objects.requireNonNull(newStop);
      for (int i = 0; i < stops.size(); i++) {
        if (stops.getOrOriginal(i).getId().equals(old)) {
          stops.with(i, newStop);
        }
      }
      return this;
    }

    /**
     * Replace the stops at given stop indices
     */
    public StopPatternBuilder replaceStops(Map<Integer, StopLocation> newStops) {
      Objects.requireNonNull(newStops);
      for (var entry : newStops.entrySet()) {
        stops.with(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * We want to deduplicate this as much as we can, since this is done
     * millions of times during real-time updates.
     */
    public StopPattern build() {
      if (stops.isNotModified() && dropoffs.isNotModified() && pickups.isNotModified()) {
        return original;
      }

      if (realTime != null) {
        var newStopPattern = new StopPattern(
          stops.build(realTime.stops),
          pickups.build(realTime.pickups),
          dropoffs.build(realTime.dropoffs)
        );
        return realTime.equals(newStopPattern) ? realTime : newStopPattern;
      }

      return new StopPattern(stops.build(), pickups.build(), dropoffs.build());
    }
  }
}
