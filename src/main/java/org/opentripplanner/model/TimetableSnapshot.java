package org.opentripplanner.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Part of concurrency control for stoptime updates.
 * <p>
 * All updates should be performed on a snapshot before it is handed off to any searches. A single
 * snapshot should be used for an entire search, and should remain unchanged for that duration to
 * provide a consistent view not only of trips that have been boarded, but of relative arrival and
 * departure times of other trips that have not necessarily been boarded.
 * <p>
 * At this point, only one writing thread at a time is supported.
 * <p>
 */
public class TimetableSnapshot {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshot.class);

  /**
   * A set of all timetables which have been modified and are waiting to be indexed. When
   * <code>dirty</code> is <code>null</code>, it indicates that the snapshot is read-only.
   */
  private final Set<Timetable> dirtyTimetables = new HashSet<>();

  /**
   * The timetables for different days, for each TripPattern (each sequence of stops on a particular
   * Route) for which we have an updated Timetable. The keys include both TripPatterns from the
   * scheduled GTFS, and TripPatterns added by realtime messages and tracked by the
   * TripPatternCache. Note that the keys will not include all scheduled TripPatterns, only those
   * for which we've got an update. We use a HashMap rather than a Map so we can clone it. If this
   * turns out to be slow/spacious we can use an array with integer pattern indexes. The SortedSet
   * members are copy-on-write.
   * FIXME: this could be made into a flat hashtable with compound keys.
   */
  private HashMap<TripPattern, SortedSet<Timetable>> timetables = new HashMap();

  /**
   * <p>
   * Map containing the current trip pattern given a trip id and a service date, if it has been
   * changed from the scheduled pattern with an update, for which the stopPattern is different.
   * </p>
   * <p>
   * This is a HashMap and not a Map so the clone function is available.
   */
  private HashMap<TripIdAndServiceDate, TripPattern> realtimeAddedTripPattern = new HashMap<>();

  /**
   * This maps contains all of the new or updated TripPatterns added by realtime data indexed on
   * stop. This has to be kept in order for them to be included in the stop times api call on a
   * specific stop.
   * <p>
   * This is a SetMultimap, so that each pattern can only be added once.
   * <p>
   * TODO Find a generic way to keep all realtime indexes.
   */
  private SetMultimap<StopLocation, TripPattern> patternsForStop = HashMultimap.create();

  /**
   * Boolean value indicating that timetable snapshot is read only if true. Once it is true, it
   * shouldn't be possible to change it to false anymore.
   */
  private boolean readOnly = false;

  /**
   * Boolean value indicating that this timetable snapshot contains changes compared to the state of
   * the last commit if true.
   */
  private boolean dirty = false;

  /**
   * Returns an updated timetable for the specified pattern if one is available in this snapshot, or
   * the originally scheduled timetable if there are no updates in this snapshot.
   */
  public Timetable resolve(TripPattern pattern, LocalDate serviceDate) {
    SortedSet<Timetable> sortedTimetables = timetables.get(pattern);

    if (sortedTimetables != null && serviceDate != null) {
      for (Timetable timetable : sortedTimetables) {
        if (timetable != null && timetable.isValidFor(serviceDate)) {
          return timetable;
        }
      }
    }

    return pattern.getScheduledTimetable();
  }

  public void removeRealtimeUpdatedTripTimes(
    TripPattern tripPattern,
    FeedScopedId tripId,
    LocalDate serviceDate
  ) {
    SortedSet<Timetable> sortedTimetables = this.timetables.get(tripPattern);
    if (sortedTimetables != null) {
      TripTimes tripTimesToRemove = null;
      for (Timetable timetable : sortedTimetables) {
        if (timetable.isValidFor(serviceDate)) {
          final TripTimes tripTimes = timetable.getTripTimes(tripId);
          if (tripTimes == null) {
            LOG.debug("No triptimes to remove for trip {}", tripId);
          } else if (tripTimesToRemove != null) {
            LOG.debug("Found two triptimes to remove for trip {}", tripId);
          } else {
            tripTimesToRemove = tripTimes;
          }
        }
      }

      if (tripTimesToRemove != null) {
        for (Timetable sortedTimetable : sortedTimetables) {
          boolean isDirty = sortedTimetable.getTripTimes().remove(tripTimesToRemove);
          if (isDirty) {
            dirtyTimetables.add(sortedTimetable);
          }
        }
      }
    }
  }

  /**
   * Get the current trip pattern given a trip id and a service date, if it has been changed from
   * the scheduled pattern with an update, for which the stopPattern is different.
   *
   * @param tripId      trip id
   * @param serviceDate service date
   * @return trip pattern created by the updater; null if trip is on the original trip pattern
   */
  public TripPattern getRealtimeAddedTripPattern(FeedScopedId tripId, LocalDate serviceDate) {
    TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(tripId, serviceDate);
    return realtimeAddedTripPattern.get(tripIdAndServiceDate);
  }

  /**
   * @return if any trip patterns were added.
   */
  public boolean hasRealtimeAddedTripPatterns() {
    return !realtimeAddedTripPattern.isEmpty();
  }

  /**
   * Update the trip times of one trip in a timetable of a trip pattern. If the trip of the trip
   * times does not exist yet in the timetable, add it.
   *
   * @param pattern          trip pattern
   * @param updatedTripTimes updated trip times
   * @param serviceDate      service day for which this update is valid
   * @return whether the update was actually applied
   */
  public Result<UpdateSuccess, UpdateError> update(
    TripPattern pattern,
    TripTimes updatedTripTimes,
    LocalDate serviceDate
  ) {
    // Preconditions
    Objects.requireNonNull(pattern);
    Objects.requireNonNull(serviceDate);

    if (readOnly) {
      throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
    }

    Timetable tt = resolve(pattern, serviceDate);
    // we need to perform the copy of Timetable here rather than in Timetable.update()
    // to avoid repeatedly copying in case several updates are applied to the same timetable
    if (!dirtyTimetables.contains(tt)) {
      Timetable old = tt;
      tt = new Timetable(tt, serviceDate);
      SortedSet<Timetable> sortedTimetables = timetables.get(pattern);
      if (sortedTimetables == null) {
        sortedTimetables = new TreeSet<>(new SortedTimetableComparator());
      } else {
        SortedSet<Timetable> temp = new TreeSet<>(new SortedTimetableComparator());
        temp.addAll(sortedTimetables);
        sortedTimetables = temp;
      }
      if (old.getServiceDate() != null) {
        sortedTimetables.remove(old);
      }
      sortedTimetables.add(tt);
      timetables.put(pattern, sortedTimetables);
      dirtyTimetables.add(tt);
      dirty = true;
    }

    // Assume all trips in a pattern are from the same feed, which should be the case.
    // Find trip index
    int tripIndex = tt.getTripIndex(updatedTripTimes.getTrip().getId());
    if (tripIndex == -1) {
      // Trip not found, add it
      tt.addTripTimes(updatedTripTimes);
    } else {
      // Set updated trip times of trip
      tt.setTripTimes(tripIndex, updatedTripTimes);
    }

    if (pattern.isCreatedByRealtimeUpdater()) {
      // Remember this pattern for the added trip id and service date
      FeedScopedId tripId = updatedTripTimes.getTrip().getId();
      TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(tripId, serviceDate);
      realtimeAddedTripPattern.put(tripIdAndServiceDate, pattern);
    }

    // To make these trip patterns visible for departureRow searches.
    addPatternToIndex(pattern);

    // The time tables are finished during the commit

    return Result.success(UpdateSuccess.noWarnings());
  }

  /**
   * This produces a small delay of typically around 50ms, which is almost entirely due to the
   * indexing step. Cloning the map is much faster (2ms). It is perhaps better to index timetables
   * as they are changed to avoid experiencing all this lag at once, but we want to avoid
   * re-indexing when receiving multiple updates for the same timetable in rapid succession. This
   * compromise is expressed by the maxSnapshotFrequency property of StoptimeUpdater. The indexing
   * could be made much more efficient as well.
   *
   * @return an immutable copy of this TimetableSnapshot with all updates applied
   */
  public TimetableSnapshot commit() {
    return commit(null, false);
  }

  @SuppressWarnings("unchecked")
  public TimetableSnapshot commit(TransitLayerUpdater transitLayerUpdater, boolean force) {
    if (readOnly) {
      throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
    }

    TimetableSnapshot ret = new TimetableSnapshot();
    if (!force && !this.isDirty()) {
      return null;
    }
    ret.timetables = (HashMap<TripPattern, SortedSet<Timetable>>) this.timetables.clone();
    ret.realtimeAddedTripPattern =
      (HashMap<TripIdAndServiceDate, TripPattern>) this.realtimeAddedTripPattern.clone();

    if (transitLayerUpdater != null) {
      transitLayerUpdater.update(dirtyTimetables, timetables);
    }

    this.dirtyTimetables.clear();
    this.dirty = false;

    ret.setPatternsForStop(HashMultimap.create(this.patternsForStop));

    ret.readOnly = true; // mark the snapshot as henceforth immutable
    return ret;
  }

  /**
   * Clear all data of snapshot for the provided feed id
   *
   * @param feedId feed id to clear the snapshot for
   */
  public void clear(String feedId) {
    if (readOnly) {
      throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
    }
    // Clear all data from snapshot.
    boolean timetableWasModified = clearTimetable(feedId);
    boolean realtimeAddedWasModified = clearRealtimeAddedTripPattern(feedId);

    // If this snapshot was modified, it will be dirty after the clear actions.
    if (timetableWasModified || realtimeAddedWasModified) {
      dirty = true;
    }
  }

  /**
   * Removes the latest added trip pattern from the cache. This should be done when removing the
   * trip times from the timetable the trip has been added to.
   */
  public void removeLastAddedTripPattern(FeedScopedId feedScopedTripId, LocalDate serviceDate) {
    realtimeAddedTripPattern.remove(new TripIdAndServiceDate(feedScopedTripId, serviceDate));
  }

  /**
   * Removes all Timetables which are valid for a ServiceDate on-or-before the one supplied.
   */
  public boolean purgeExpiredData(LocalDate serviceDate) {
    if (readOnly) {
      throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
    }

    boolean modified = false;
    for (Iterator<TripPattern> it = timetables.keySet().iterator(); it.hasNext();) {
      TripPattern pattern = it.next();
      SortedSet<Timetable> sortedTimetables = timetables.get(pattern);
      SortedSet<Timetable> toKeepTimetables = new TreeSet<>(new SortedTimetableComparator());
      for (Timetable timetable : sortedTimetables) {
        if (serviceDate.compareTo(timetable.getServiceDate()) < 0) {
          toKeepTimetables.add(timetable);
        } else {
          modified = true;
        }
      }

      if (toKeepTimetables.isEmpty()) {
        it.remove();
      } else {
        timetables.put(pattern, toKeepTimetables);
      }
    }

    // Also remove last added trip pattern for days that are purged
    for (
      Iterator<Entry<TripIdAndServiceDate, TripPattern>> iterator = realtimeAddedTripPattern
        .entrySet()
        .iterator();
      iterator.hasNext();
    ) {
      TripIdAndServiceDate tripIdAndServiceDate = iterator.next().getKey();
      if (serviceDate.compareTo(tripIdAndServiceDate.serviceDate()) >= 0) {
        iterator.remove();
        modified = true;
      }
    }

    return modified;
  }

  public boolean isDirty() {
    if (readOnly) {
      return false;
    }
    return dirty;
  }

  public String toString() {
    String d = readOnly ? "committed" : String.format("%d dirty", dirtyTimetables.size());
    return String.format("Timetable snapshot: %d timetables (%s)", timetables.size(), d);
  }

  public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    return patternsForStop.get(stop);
  }

  public void setPatternsForStop(SetMultimap<StopLocation, TripPattern> patternsForStop) {
    this.patternsForStop = patternsForStop;
  }

  /**
   * Clear timetable for all patterns matching the provided feed id.
   *
   * @param feedId feed id to clear out
   * @return true if the timetable changed as a result of the call
   */
  protected boolean clearTimetable(String feedId) {
    return timetables.keySet().removeIf(tripPattern -> feedId.equals(tripPattern.getFeedId()));
  }

  /**
   * Clear all realtime added trip patterns matching the provided feed id.
   *
   * @param feedId feed id to clear out
   * @return true if the realtimeAddedTripPattern changed as a result of the call
   */
  protected boolean clearRealtimeAddedTripPattern(String feedId) {
    return realtimeAddedTripPattern
      .keySet()
      .removeIf(realtimeAddedTripPattern ->
        feedId.equals(realtimeAddedTripPattern.tripId().getFeedId())
      );
  }

  /**
   * Add the patterns to the stop index, only if they come from a modified pattern
   */
  private void addPatternToIndex(TripPattern tripPattern) {
    if (tripPattern.isCreatedByRealtimeUpdater()) {
      //TODO - SIRI: Add pattern to index?

      for (var stop : tripPattern.getStops()) {
        patternsForStop.put(stop, tripPattern);
      }
    }
  }

  protected static class SortedTimetableComparator implements Comparator<Timetable> {

    @Override
    public int compare(Timetable t1, Timetable t2) {
      return t1.getServiceDate().compareTo(t2.getServiceDate());
    }
  }
}
