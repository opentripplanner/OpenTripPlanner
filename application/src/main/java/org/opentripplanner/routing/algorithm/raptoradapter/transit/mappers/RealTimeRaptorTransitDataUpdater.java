package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferIndexGenerator;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TimetableSnapshotUpdateListener;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update the RaptorTransitData from a set of TimeTables. A shallow copy is made of the RaptorTransitData
 * (this also includes a shallow copy of the TripPatternsForDate map). TripPatterns are matched on
 * id and replaced by their updated versions. The realtime RaptorTransitData is then switched out with
 * the updated copy in an atomic operation. This ensures that any RaptorTransitData that is referenced
 * from the Graph is never changed.
 *
 * This is a way of keeping the RaptorTransitData up to date (in sync with the TimetableRepository plus its most
 * recent TimetableSnapshot) without repeatedly deriving it from scratch every few seconds. The same
 * incremental changes are applied to both the TimetableSnapshot and the RaptorTransitData and they are
 * published together.
 */
public class RealTimeRaptorTransitDataUpdater implements TimetableSnapshotUpdateListener {

  private static final Logger LOG = LoggerFactory.getLogger(RealTimeRaptorTransitDataUpdater.class);

  private final TimetableRepository timetableRepository;

  /**
   * Cache the TripPatternForDates indexed on the original TripPatterns in order to avoid this
   * expensive operation being done each time the update method is called.
   */
  private final Map<
    LocalDate,
    Map<TripPattern, TripPatternForDate>
  > tripPatternsStartingOnDateMapCache = new HashMap<>();

  /**
   * Cache the TripPatternForDate currently in use for a trip and service date. Only one TripPatternForDate is allowed
   * for a trip id and service date. This cache is used to clean up extra tripPatternsForDate.
   */
  private final Map<
    TripIdAndServiceDate,
    TripPatternForDate
  > tripPatternsForTripIdAndServiceDateCache = new HashMap<>();

  private final Map<LocalDate, Set<TripPatternForDate>> tripPatternsRunningOnDateMapCache =
    new HashMap<>();

  public RealTimeRaptorTransitDataUpdater(TimetableRepository timetableRepository) {
    this.timetableRepository = timetableRepository;
  }

  /// Updates the real-time [RaptorTransitData] to use the modified timetables.
  ///
  /// This method bridges the different update approaches:
  /// 1. `updatedTimetables` and `timetables` only contains [Timetable]s with real-time
  ///    updates. This means that removed items are not present.
  /// 2. [RaptorTransitData] requires applying the changes to a previous snapshot: adding,
  ///    updating and removing timetables.
  ///
  /// To support this the method has three tasks:
  /// 1. Collect [TripPatternForDate]s which have invalidated data (`oldTripPatternsForDate`).
  ///    Trips may change in multiple ways and because of that may move between [TripPattern]s. To
  ///    track a [TripIdAndServiceDate] it's previous state needs to be stored so that all relevant
  ///    places may be updated.
  ///      * a trip may have a new (real-time) Timetable, which results in two updated [Timetable]s
  ///      * a trip may move between scheduled [StopPattern]s and/or real-time [StopPattern]s
  /// 2. Collect [TripPatternForDate]s which have valid data (`newTripPatternsForDate`).
  ///    There are two options:
  ///    1. an update was received
  ///    2. no update was received, and so the previous updated should be removed. If the update
  ///       was for a scheduled trip, then the schedule should be restored.
  /// 3. Remove the `oldTripPatternsForDate` and add the `newTripPatternsForDate` to the
  ///    [RaptorTransitData].
  @Override
  public void update(
    Collection<Timetable> updatedTimetables,
    Function<FeedScopedId, SortedSet<Timetable>> timetableProvider
  ) {
    if (!timetableRepository.hasRealtimeRaptorTransitData()) {
      return;
    }

    long startTime = System.currentTimeMillis();

    // Make a shallow copy of the realtime transit layer. Only the objects that are copied will be
    // changed during this update process.
    RaptorTransitData realtimeRaptorTransitData = new RaptorTransitData(
      timetableRepository.getRealtimeRaptorTransitData()
    );

    // Instantiate a TripPatternForDateMapper with the new TripPattern mappings
    TripPatternForDateMapper tripPatternForDateMapper = new TripPatternForDateMapper(
      timetableRepository.getServiceCodesRunningForDate()
    );

    Set<LocalDate> datesToBeUpdated = new HashSet<>();
    SetMultimap<TripPattern, TripPatternForDate> newTripPatternsForDate = HashMultimap.create();
    SetMultimap<TripPattern, TripPatternForDate> oldTripPatternsForDate = HashMultimap.create();

    TransferIndexGenerator transferIndexGenerator = null;
    if (OTPFeature.TransferConstraints.isOn()) {
      transferIndexGenerator = realtimeRaptorTransitData.getTransferIndexGenerator();
    }
    Set<TripPatternForDate> previouslyUsedPatterns = new HashSet<>();
    // Map new TriPatternForDate and index for old and new TripPatternsForDate on service date
    for (Timetable timetable : updatedTimetables) {
      LocalDate date = timetable.getServiceDate();
      TripPattern tripPattern = timetable.getPattern();

      if (!tripPatternsStartingOnDateMapCache.containsKey(date)) {
        Map<TripPattern, TripPatternForDate> map = realtimeRaptorTransitData
          .getTripPatternsOnServiceDateCopy(date)
          .stream()
          .collect(Collectors.toMap(t -> t.getTripPattern().getPattern(), t -> t));
        tripPatternsStartingOnDateMapCache.put(date, map);
      }

      TripPatternForDate oldTripPatternForDate = tripPatternsStartingOnDateMapCache
        .get(date)
        .get(tripPattern);

      if (oldTripPatternForDate != null) {
        tripPatternsStartingOnDateMapCache.get(date).remove(tripPattern, oldTripPatternForDate);
        oldTripPatternsForDate.put(tripPattern, oldTripPatternForDate);
        datesToBeUpdated.addAll(oldTripPatternForDate.getRunningPeriodDates());
      }

      TripPatternForDate newTripPatternForDate;

      try {
        newTripPatternForDate = tripPatternForDateMapper.mapAndValidate(
          timetable,
          timetable.getServiceDate()
        );
      } catch (IllegalArgumentException exception) {
        // There is some issue with finding the correct running period, using old pattern instead
        newTripPatternForDate = oldTripPatternForDate;
      }

      if (newTripPatternForDate != null) {
        tripPatternsStartingOnDateMapCache.get(date).put(tripPattern, newTripPatternForDate);
        newTripPatternsForDate.put(tripPattern, newTripPatternForDate);
        datesToBeUpdated.addAll(newTripPatternForDate.getRunningPeriodDates());
        if (transferIndexGenerator != null && tripPattern.isRealTimeTripPattern()) {
          transferIndexGenerator.addRealtimeTrip(
            tripPattern,
            timetable.getTripTimes().stream().map(TripTimes::getTrip).collect(Collectors.toList())
          );
        }

        for (TripTimes triptimes : timetable.getTripTimes()) {
          var id = new TripIdAndServiceDate(
            triptimes.getTrip().getId(),
            timetable.getServiceDate()
          );
          TripPatternForDate previousTripPatternForDate =
            tripPatternsForTripIdAndServiceDateCache.put(id, newTripPatternForDate);
          if (previousTripPatternForDate != null) {
            previouslyUsedPatterns.add(previousTripPatternForDate);
          } else {
            LOG.debug(
              "NEW TripPatternForDate: {} - {}",
              newTripPatternForDate.getServiceDate(),
              newTripPatternForDate.getTripPattern().debugInfo()
            );
          }
        }
      }
    }

    // Now loop through all running period dates of old and new TripPatternsForDate and update
    // the tripPatternsByRunningPeriodDate accordingly
    for (LocalDate date : datesToBeUpdated) {
      tripPatternsRunningOnDateMapCache.computeIfAbsent(date, p ->
        new HashSet<>(realtimeRaptorTransitData.getTripPatternsRunningOnDateCopy(date))
      );

      // Remove old cached tripPatterns where tripTimes are no longer running
      Set<TripPatternForDate> patternsForDate = tripPatternsRunningOnDateMapCache.get(date);

      for (Map.Entry<TripPattern, Collection<TripPatternForDate>> entry : oldTripPatternsForDate
        .asMap()
        .entrySet()) {
        for (TripPatternForDate oldTripPatternForDate : entry.getValue()) {
          // Remove old TripPatternForDate for this date if it was valid on this date
          if (oldTripPatternForDate != null) {
            if (oldTripPatternForDate.getRunningPeriodDates().contains(date)) {
              patternsForDate.remove(oldTripPatternForDate);
            }
          }
        }
      }

      for (TripPatternForDate tripPatternForDate : previouslyUsedPatterns) {
        if (tripPatternForDate.getServiceDate().equals(date)) {
          TripPattern pattern = tripPatternForDate.getTripPattern().getPattern();
          if (!pattern.isRealTimeTripPattern()) {
            continue;
          }
          var oldTimeTable = timetableProvider.apply(pattern.getId());
          if (oldTimeTable != null) {
            var toRemove = oldTimeTable
              .stream()
              .filter(tt -> tt.getServiceDate().equals(date))
              .findFirst()
              .map(tt -> tt.getTripTimes().isEmpty())
              .orElse(false);

            if (toRemove) {
              patternsForDate.remove(tripPatternForDate);
            }
          } else {
            LOG.warn("Could not fetch timetable for {}, removing.", pattern);
            patternsForDate.remove(tripPatternForDate);
          }
        }
      }

      for (Map.Entry<TripPattern, Collection<TripPatternForDate>> entry : newTripPatternsForDate
        .asMap()
        .entrySet()) {
        for (TripPatternForDate newTripPatternForDate : entry.getValue()) {
          // Add new TripPatternForDate for this date if it mapped correctly and is valid on this date
          if (newTripPatternForDate != null) {
            if (newTripPatternForDate.getRunningPeriodDates().contains(date)) {
              patternsForDate.add(newTripPatternForDate);
            }
          }
        }
      }

      realtimeRaptorTransitData.replaceTripPatternsForDate(date, new ArrayList<>(patternsForDate));
    }

    if (transferIndexGenerator != null) {
      realtimeRaptorTransitData.setConstrainedTransfers(transferIndexGenerator.generateTransfers());
    }

    // Switch out the reference with the updated realtimeRaptorTransitData. This is synchronized to
    // guarantee that the reference is set after all the fields have been updated.
    timetableRepository.setRealtimeRaptorTransitData(realtimeRaptorTransitData);

    LOG.debug(
      "UPDATING {} tripPatterns took {} ms",
      updatedTimetables.size(),
      System.currentTimeMillis() - startTime
    );
  }
}
