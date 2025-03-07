package org.opentripplanner.model;

import static org.opentripplanner.utils.collection.CollectionUtils.getByNullableKey;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RealTimeRaptorTransitDataUpdater;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TimetableSnapshot holds a set of realtime-updated Timetables frozen at a moment in time. It
 * can return a Timetable for any TripPattern in the public transit network considering all
 * accumulated realtime updates, falling back on the scheduled Timetable if no updates have been
 * applied for a given TripPattern.
 * <p>
 * This is a central part of managing concurrency when many routing searches may be happening, but
 * realtime updates are also streaming in which change the vehicle arrival and departure times.
 * Any given request will only see one unchanging TimetableSnapshot over the course of its search.
 * <p>
 * An instance of TimetableSnapshot first serves as a buffer to accumulate a batch of incoming
 * updates on top of any already known updates to the base schedules. From time to time such a batch
 * of updates is committed (like a database transaction). At this point the TimetableSnapshot is
 * treated as immutable and becomes available for use by new incoming routing requests.
 * <p>
 * All updates to a snapshot must be completed before it is handed off to any searches. A single
 * snapshot should be used for an entire search, and should remain unchanged for that duration to
 * provide a consistent view not only of trips that have been boarded, but of relative arrival and
 * departure times of other trips that have not necessarily been boarded.
 * <p>
 * A TimetableSnapshot instance may only be modified by a single thread. This makes it easier to
 * reason about how the snapshot is built up and used. Write operations are applied one by one, in
 * order, with no concurrent access. Read operations are then allowed concurrently by many threads
 * after writing is forbidden.
 * <p>
 * The fact that TripPattern instances carry a reference only to their scheduled Timetable and not
 * to their realtime timetable is largely due to historical path-dependence in OTP development.
 * Streaming realtime support was added around 2013 as a sort of sandbox feature that was switched
 * off by default. Looking up realtime timetables during routing was a fringe feature that needed
 * to impose near-zero cost and avoid introducing complexity into the primary codebase. Now over
 * ten years later, the principles of how this system operates are rather stable, but the
 * implementation would benefit from some deduplication and cleanup. Once that is complete, looking
 * up timetables on this class could conceivably be replaced with snapshotting entire views of the
 * transit network. It would also be possible to make the realtime version of Timetables or
 * TripTimes the primary view, and include references back to their scheduled versions.
 * <p>
 * Implementation note: when a snapshot is committed, the mutable state of this class is stored
 * in final fields and completely initialized in the constructor. This provides an additional
 * guarantee of safe-publication without synchronization.
 * (see <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.5">final Field Semantics</a>)
 */
public class TimetableSnapshot {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshot.class);

  /**
   * During the construction phase of the TimetableSnapshot, before it is considered immutable and
   * used in routing, this Map holds all timetables that have been modified and are waiting to be
   * indexed.
   * A real-time timetable overrides the scheduled timetable of a TripPattern for only a single
   * service date. There can be only one overriding timetable per TripPattern and per service date.
   * This is enforced by indexing the map with a pair (TripPattern, service date).
   * This map is cleared when the TimetableSnapshot becomes read-only.
   */
  private final Map<TripPatternAndServiceDate, Timetable> dirtyTimetables = new HashMap<>();

  /**
   * For each TripPattern (sequence of stops on a particular Route) for which we have received a
   * realtime update, an ordered set of timetables on different days. The key TripPatterns may
   * include ones from the scheduled GTFS, as well as ones added by realtime messages and
   * tracked by the TripPatternCache. <p>
   * Note that the keys do not include all scheduled TripPatterns, only those for which we have at
   * least one update, and those for which we had updates before but just recently cleared.<p>
   * The members of the SortedSet (the Timetable for a particular day) are treated as copy-on-write
   * when we're updating them. If an update will modify the timetable for a particular day, that
   * timetable is replicated before any modifications are applied to avoid affecting any previous
   * TimetableSnapshots still in circulation which reference that same Timetable instance. <p>
   * Alternative implementations: A. This could be an array indexed using the integer pattern
   * indexes. B. It could be made into a flat hashtable with compound keys (TripPattern, LocalDate).
   * The compound key approach better reflects the fact that there should be only one Timetable per
   * TripPattern and date.
   */
  private final Map<TripPattern, SortedSet<Timetable>> timetables;
  private final Set<TripPatternAndServiceDate> patternAndServiceDatesToBeRestored = new HashSet<>();

  /**
   * For cases where the trip pattern (sequence of stops visited) has been changed by a realtime
   * update, a Map associating the updated trip pattern with a compound key of the feed-scoped
   * trip ID and the service date.
   * This index includes only modified trip patterns for existing trips.
   * It does not include trip patterns for new trips created by real-time updates (extra journeys).
   * .
   * TODO RT_AB: clarify if this is an index or the original source of truth.
   */
  private final Map<TripIdAndServiceDate, TripPattern> realTimeNewTripPatternsForModifiedTrips;

  /**
   * This is an index of TripPatterns, not the primary collection. It tracks which TripPatterns
   * that were updated or newly created by realtime messages contain which stops. This allows them
   * to be readily found and included in API responses containing stop times at a specific stop.
   * This is a SetMultimap, so that each pattern is only retained once per stop even if it's added
   * more than once.
   * TODO RT_AB: More general handling of all realtime indexes outside primary data structures.
   */
  private final SetMultimap<StopLocation, TripPattern> patternsForStop;

  /**
   * The realTimeAdded* maps are indexes on the trips created at runtime (extra-journey), and the
   * Route, TripPattern, TripOnServiceDate they refer to.
   * They are meant to override the corresponding indexes in TimetableRepositoryIndex.
   */
  private final Map<FeedScopedId, Route> realtimeAddedRoutes;
  private final Map<FeedScopedId, Trip> realTimeAddedTrips;
  private final Map<Trip, TripPattern> realTimeAddedPatternForTrip;
  private final Multimap<Route, TripPattern> realTimeAddedPatternsForRoute;
  private final Map<FeedScopedId, TripOnServiceDate> realTimeAddedTripOnServiceDateById;
  private final Map<
    TripIdAndServiceDate,
    TripOnServiceDate
  > realTimeAddedTripOnServiceDateForTripAndDay;

  /**
   * Boolean value indicating that timetable snapshot is read only if true. Once it is true, it
   * shouldn't be possible to change it to false anymore.
   */
  private final boolean readOnly;

  /**
   * Boolean value indicating that this timetable snapshot contains changes compared to the state of
   * the last commit if true.
   */
  private boolean dirty = false;

  public TimetableSnapshot() {
    this(
      new HashMap<>(),
      new HashMap<>(),
      new HashMap<>(),
      new HashMap<>(),
      new HashMap<>(),
      HashMultimap.create(),
      new HashMap<>(),
      new HashMap<>(),
      HashMultimap.create(),
      false
    );
  }

  private TimetableSnapshot(
    Map<TripPattern, SortedSet<Timetable>> timetables,
    Map<TripIdAndServiceDate, TripPattern> realTimeNewTripPatternsForModifiedTrips,
    Map<FeedScopedId, Route> realtimeAddedRoutes,
    Map<FeedScopedId, Trip> realtimeAddedTrips,
    Map<Trip, TripPattern> realTimeAddedPatternForTrip,
    Multimap<Route, TripPattern> realTimeAddedPatternsForRoute,
    Map<FeedScopedId, TripOnServiceDate> realTimeAddedTripOnServiceDateById,
    Map<TripIdAndServiceDate, TripOnServiceDate> realTimeAddedTripOnServiceDateForTripAndDay,
    SetMultimap<StopLocation, TripPattern> patternsForStop,
    boolean readOnly
  ) {
    this.timetables = timetables;
    this.realTimeNewTripPatternsForModifiedTrips = realTimeNewTripPatternsForModifiedTrips;
    this.realtimeAddedRoutes = realtimeAddedRoutes;
    this.realTimeAddedTrips = realtimeAddedTrips;
    this.realTimeAddedPatternForTrip = realTimeAddedPatternForTrip;
    this.realTimeAddedPatternsForRoute = realTimeAddedPatternsForRoute;
    this.realTimeAddedTripOnServiceDateById = realTimeAddedTripOnServiceDateById;
    this.realTimeAddedTripOnServiceDateForTripAndDay = realTimeAddedTripOnServiceDateForTripAndDay;
    this.patternsForStop = patternsForStop;
    this.readOnly = readOnly;
  }

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

  /**
   * Get the current trip pattern given a trip id and a service date, if it has been changed from
   * the scheduled pattern with an update, for which the stopPattern is different.
   *
   * @return trip pattern created by the updater; null if trip is on the original trip pattern
   */
  @Nullable
  public TripPattern getNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate) {
    TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(tripId, serviceDate);
    return realTimeNewTripPatternsForModifiedTrips.get(tripIdAndServiceDate);
  }

  /**
   * List trips which have been canceled.
   */
  public List<TripOnServiceDate> listCanceledTrips() {
    return findTripsOnServiceDates(TripTimes::isCanceled);
  }

  /**
   * @return if any trip patterns were modified
   */
  public boolean hasNewTripPatternsForModifiedTrips() {
    return !realTimeNewTripPatternsForModifiedTrips.isEmpty();
  }

  /**
   * Return the route created by the updater for the given id.
   */
  @Nullable
  public Route getRealtimeAddedRoute(FeedScopedId id) {
    return getByNullableKey(id, realtimeAddedRoutes);
  }

  public Collection<Route> listRealTimeAddedRoutes() {
    return Collections.unmodifiableCollection(realtimeAddedRoutes.values());
  }

  /**
   * Return the trip created by the updater for the given id.
   */
  @Nullable
  public Trip getRealTimeAddedTrip(FeedScopedId id) {
    return getByNullableKey(id, realTimeAddedTrips);
  }

  public Collection<Trip> listRealTimeAddedTrips() {
    return Collections.unmodifiableCollection(realTimeAddedTrips.values());
  }

  /**
   * Return the trip pattern created by the updater for the given trip.
   */
  @Nullable
  public TripPattern getRealTimeAddedPatternForTrip(Trip trip) {
    return getByNullableKey(trip, realTimeAddedPatternForTrip);
  }

  /**
   * Return the trip patterns created by the updater for the given route.
   */
  public Collection<TripPattern> getRealTimeAddedPatternForRoute(Route route) {
    return realTimeAddedPatternsForRoute.get(route);
  }

  /**
   * Return the trip on service date created by the updater for the given id.
   */
  @Nullable
  public TripOnServiceDate getRealTimeAddedTripOnServiceDateById(FeedScopedId id) {
    return getByNullableKey(id, realTimeAddedTripOnServiceDateById);
  }

  /**
   * Return the trip on service date created by the updater for the given trip and service date.
   */
  @Nullable
  public TripOnServiceDate getRealTimeAddedTripOnServiceDateForTripAndDay(
    TripIdAndServiceDate tripIdAndServiceDate
  ) {
    return getByNullableKey(tripIdAndServiceDate, realTimeAddedTripOnServiceDateForTripAndDay);
  }

  public Collection<? extends TripOnServiceDate> listRealTimeAddedTripOnServiceDate() {
    return Collections.unmodifiableCollection(realTimeAddedTripOnServiceDateForTripAndDay.values());
  }

  /**
   * Update the TripTimes of one Trip in a Timetable of a TripPattern. If the Trip of the TripTimes
   * does not exist yet in the Timetable, add it. This method will make a protective copy of the
   * Timetable if such a copy has not already been made while building up this snapshot, handling
   * both cases where patterns were pre-existing in static data or created by realtime data.
   *
   * @return whether the update was actually applied
   */
  public Result<UpdateSuccess, UpdateError> update(RealTimeTripUpdate realTimeTripUpdate) {
    validateNotReadOnly();

    TripPattern pattern = realTimeTripUpdate.pattern();
    LocalDate serviceDate = realTimeTripUpdate.serviceDate();
    TripTimes updatedTripTimes = realTimeTripUpdate.updatedTripTimes();

    Timetable tt = resolve(pattern, serviceDate);
    TimetableBuilder ttb = tt.copyOf().withServiceDate(serviceDate);

    // Assume all trips in a pattern are from the same feed, which should be the case.
    ttb.addOrUpdateTripTimes(updatedTripTimes);

    Timetable updated = ttb.build();
    swapTimetable(pattern, tt, updated);

    Trip trip = updatedTripTimes.getTrip();
    if (pattern.isCreatedByRealtimeUpdater()) {
      // Remember this pattern for the added trip id and service date
      FeedScopedId tripId = trip.getId();
      TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(tripId, serviceDate);
      realTimeNewTripPatternsForModifiedTrips.put(tripIdAndServiceDate, pattern);
    }

    // To make these trip patterns visible for departureRow searches.
    addPatternToIndex(pattern);

    Route route = trip.getRoute();

    if (realTimeTripUpdate.routeCreation()) {
      realtimeAddedRoutes.put(route.getId(), route);
    }
    if (realTimeTripUpdate.tripCreation()) {
      FeedScopedId tripId = trip.getId();
      realTimeAddedTrips.put(tripId, trip);
      realTimeAddedPatternForTrip.put(trip, pattern);
      realTimeAddedPatternsForRoute.put(route, pattern);
      TripOnServiceDate tripOnServiceDate = realTimeTripUpdate.addedTripOnServiceDate();

      if (tripOnServiceDate != null) {
        realTimeAddedTripOnServiceDateById.put(tripOnServiceDate.getId(), tripOnServiceDate);
        realTimeAddedTripOnServiceDateForTripAndDay.put(
          new TripIdAndServiceDate(tripId, serviceDate),
          tripOnServiceDate
        );
      }
    }

    // The time tables are finished during the commit
    return Result.success(UpdateSuccess.noWarnings(realTimeTripUpdate.producer()));
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

  public TimetableSnapshot commit(
    RealTimeRaptorTransitDataUpdater realtimeRaptorTransitDataUpdater,
    boolean force
  ) {
    validateNotReadOnly();

    if (!force && !this.isDirty()) {
      return null;
    }
    TimetableSnapshot ret = new TimetableSnapshot(
      Map.copyOf(timetables),
      Map.copyOf(realTimeNewTripPatternsForModifiedTrips),
      Map.copyOf(realtimeAddedRoutes),
      Map.copyOf(realTimeAddedTrips),
      Map.copyOf(realTimeAddedPatternForTrip),
      ImmutableSetMultimap.copyOf(realTimeAddedPatternsForRoute),
      Map.copyOf(realTimeAddedTripOnServiceDateById),
      Map.copyOf(realTimeAddedTripOnServiceDateForTripAndDay),
      ImmutableSetMultimap.copyOf(patternsForStop),
      true
    );

    if (realtimeRaptorTransitDataUpdater != null) {
      for (var patternAndServiceDate : patternAndServiceDatesToBeRestored) {
        if (!dirtyTimetables.containsKey(patternAndServiceDate)) {
          var pattern = patternAndServiceDate.tripPattern();
          var scheduledTimetable = pattern.getScheduledTimetable();
          dirtyTimetables.put(
            patternAndServiceDate,
            scheduledTimetable.copyForServiceDate(patternAndServiceDate.serviceDate)
          );
        }
      }

      realtimeRaptorTransitDataUpdater.update(dirtyTimetables.values(), timetables);
    }

    patternAndServiceDatesToBeRestored.clear();
    this.dirtyTimetables.clear();
    this.dirty = false;

    return ret;
  }

  /**
   * Clear all data of snapshot for the provided feed id
   *
   * @param feedId feed id to clear the snapshot for
   */
  public void clear(String feedId) {
    validateNotReadOnly();
    // Clear all data from snapshot.
    boolean timetablesWereCleared = clearTimetables(feedId);
    boolean newTripPatternsForModifiedTripsWereCleared = clearNewTripPatternsForModifiedTrips(
      feedId
    );
    boolean addedTripPatternsWereCleared = clearEntriesForRealtimeAddedTrips(feedId);
    // If this snapshot was modified, it will be dirty after the clear actions.
    if (
      timetablesWereCleared ||
      newTripPatternsForModifiedTripsWereCleared ||
      addedTripPatternsWereCleared
    ) {
      dirty = true;
    }
  }

  /**
   * If a previous realtime update has changed which trip pattern is associated with the given trip
   * on the given service date, this method will dissociate the trip from that pattern and remove
   * the trip's timetables from that pattern on that particular service date.
   *
   * For this service date, the trip will revert to its original trip pattern from the scheduled
   * data, remaining on that pattern unless it's changed again by a future realtime update.
   *
   * @return true if the trip was found to be shifted to a different trip pattern by a realtime
   * message and an attempt was made to re-associate it with its originally scheduled trip pattern.
   */
  public boolean revertTripToScheduledTripPattern(FeedScopedId tripId, LocalDate serviceDate) {
    validateNotReadOnly();

    boolean success = false;

    final TripPattern pattern = getNewTripPatternForModifiedTrip(tripId, serviceDate);
    if (pattern != null) {
      // Dissociate the given trip from any realtime-added pattern.
      // The trip will then fall back to its original scheduled pattern.
      realTimeNewTripPatternsForModifiedTrips.remove(new TripIdAndServiceDate(tripId, serviceDate));
      // Remove times for the trip from any timetables
      // under that now-obsolete realtime-added pattern.
      SortedSet<Timetable> sortedTimetables = this.timetables.get(pattern);
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
          for (Timetable originalTimetable : sortedTimetables) {
            if (originalTimetable.getTripTimes().contains(tripTimesToRemove)) {
              Timetable updatedTimetable = originalTimetable
                .copyOf()
                .removeTripTimes(tripTimesToRemove)
                .build();
              swapTimetable(pattern, originalTimetable, updatedTimetable);
            }
          }
        }
      }
      success = true;
    }

    return success;
  }

  /**
   * Removes all Timetables which are valid for a ServiceDate on-or-before the one supplied.
   *
   * @return true if any data has been modified and false if no purging has happened.
   */
  public boolean purgeExpiredData(LocalDate serviceDate) {
    validateNotReadOnly();

    boolean modified = false;
    for (Iterator<TripPattern> it = timetables.keySet().iterator(); it.hasNext();) {
      TripPattern pattern = it.next();
      SortedSet<Timetable> sortedTimetables = timetables.get(pattern);
      SortedSet<Timetable> toKeepTimetables = new TreeSet<>(new SortedTimetableComparator());
      for (Timetable timetable : sortedTimetables) {
        if (serviceDate.isBefore(timetable.getServiceDate())) {
          toKeepTimetables.add(timetable);
        } else {
          modified = true;
        }
      }

      if (toKeepTimetables.isEmpty()) {
        it.remove();
      } else {
        timetables.put(pattern, ImmutableSortedSet.copyOfSorted(toKeepTimetables));
      }
    }

    // Also remove last added trip pattern for days that are purged
    for (
      Iterator<Entry<TripIdAndServiceDate, TripPattern>> iterator =
        realTimeNewTripPatternsForModifiedTrips.entrySet().iterator();
      iterator.hasNext();
    ) {
      TripIdAndServiceDate tripIdAndServiceDate = iterator.next().getKey();
      if (!serviceDate.isBefore(tripIdAndServiceDate.serviceDate())) {
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

  /**
   * Does this snapshot contain any realtime data or is it completely empty?
   */
  public boolean isEmpty() {
    return (
      dirtyTimetables.isEmpty() &&
      timetables.isEmpty() &&
      realTimeNewTripPatternsForModifiedTrips.isEmpty()
    );
  }

  /**
   * Clear timetable for all patterns matching the provided feed id.
   *
   * @param feedId feed id to clear out
   * @return true if the timetable changed as a result of the call
   */
  private boolean clearTimetables(String feedId) {
    var entriesToBeRemoved = timetables
      .entrySet()
      .stream()
      .filter(entry -> feedId.equals(entry.getKey().getFeedId()))
      .collect(Collectors.toSet());
    patternAndServiceDatesToBeRestored.addAll(
      entriesToBeRemoved
        .stream()
        .flatMap(entry ->
          entry
            .getValue()
            .stream()
            .map(timetable ->
              new TripPatternAndServiceDate(entry.getKey(), timetable.getServiceDate())
            )
        )
        .toList()
    );
    return timetables.entrySet().removeAll(entriesToBeRemoved);
  }

  /**
   * Clear new trip patterns for modified trips matching the provided feed id.
   *
   * @param feedId feed id to clear out
   * @return true if the newTripPatternForModifiedTrip changed as a result of the call
   */
  private boolean clearNewTripPatternsForModifiedTrips(String feedId) {
    return realTimeNewTripPatternsForModifiedTrips
      .keySet()
      .removeIf(tripIdAndServiceDate -> feedId.equals(tripIdAndServiceDate.tripId().getFeedId()));
  }

  /**
   * Clear all realtime added routes, trip patterns and trips matching the provided feed id.
   *
   * */
  private boolean clearEntriesForRealtimeAddedTrips(String feedId) {
    // it is sufficient to test for the removal of added trips, since other indexed entities are
    // added only if a new trip is added.
    boolean removedEntry = realTimeAddedTrips
      .keySet()
      .removeIf(id -> feedId.equals(id.getFeedId()));
    realTimeAddedPatternForTrip.keySet().removeIf(trip -> feedId.equals(trip.getId().getFeedId()));
    realTimeAddedTripOnServiceDateForTripAndDay
      .keySet()
      .removeIf(tripOnServiceDate -> feedId.equals(tripOnServiceDate.tripId().getFeedId()));
    realTimeAddedTripOnServiceDateById.keySet().removeIf(id -> feedId.equals(id.getFeedId()));
    realTimeAddedPatternsForRoute
      .keySet()
      .removeIf(route -> feedId.equals(route.getId().getFeedId()));
    realtimeAddedRoutes.keySet().removeIf(id -> feedId.equals(id.getFeedId()));
    return removedEntry;
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

  /**
   * Replace the original Timetable by the updated one in the timetable index.
   * The SortedSet that holds the collection of Timetables for that pattern
   * (sorted by service date) is shared between multiple snapshots and must be copied as well.<br/>
   * Note on performance: if  multiple Timetables are modified in a SortedSet, the SortedSet will be
   * copied multiple times. The impact on memory/garbage collection is assumed to be minimal
   * since the collection is small.
   * The SortedSet is made immutable to prevent change after snapshot publication.
   */
  private void swapTimetable(TripPattern pattern, Timetable original, Timetable updated) {
    SortedSet<Timetable> sortedTimetables = timetables.get(pattern);
    if (sortedTimetables == null) {
      sortedTimetables = new TreeSet<>(new SortedTimetableComparator());
    } else {
      SortedSet<Timetable> temp = new TreeSet<>(new SortedTimetableComparator());
      temp.addAll(sortedTimetables);
      sortedTimetables = temp;
    }
    // This is a minor optimization:
    // Since sortedTimetables contains only timetables created in real-time, no need to try to
    // remove the original if it was not created by real-time.
    if (original.isCreatedByRealTimeUpdater()) {
      sortedTimetables.remove(original);
    }
    sortedTimetables.add(updated);
    timetables.put(pattern, ImmutableSortedSet.copyOfSorted(sortedTimetables));

    // if the timetable was already modified by a previous real-time update in the same snapshot
    // and for the same service date,
    // then the previously updated timetable is superseded by the new one
    dirtyTimetables.put(new TripPatternAndServiceDate(pattern, updated.getServiceDate()), updated);

    dirty = true;
  }

  private void validateNotReadOnly() {
    if (readOnly) {
      throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
    }
  }

  private TripOnServiceDate mapToTripOnServiceDate(TripTimes tripTimes, Timetable timetable) {
    return TripOnServiceDate.of(tripTimes.getTrip().getId())
      .withServiceDate(timetable.getServiceDate())
      .withTrip(tripTimes.getTrip())
      .build();
  }

  /**
   * Find trips from timetables based on filter criteria.
   *
   * @param filter used to filter {@link TripTimes}.
   */
  private List<TripOnServiceDate> findTripsOnServiceDates(Predicate<TripTimes> filter) {
    return timetables
      .values()
      .stream()
      .flatMap(timetables ->
        timetables
          .stream()
          .flatMap(timetable ->
            timetable
              .getTripTimes()
              .stream()
              .filter(filter)
              .map(tripTimes -> mapToTripOnServiceDate(tripTimes, timetable))
          )
      )
      .collect(Collectors.toCollection(ArrayList::new));
  }

  protected static class SortedTimetableComparator implements Comparator<Timetable> {

    @Override
    public int compare(Timetable t1, Timetable t2) {
      return t1.getServiceDate().compareTo(t2.getServiceDate());
    }
  }

  /**
   * A pair made of a TripPattern and one of the service dates it is running on.
   */
  private record TripPatternAndServiceDate(TripPattern tripPattern, LocalDate serviceDate) {}
}
