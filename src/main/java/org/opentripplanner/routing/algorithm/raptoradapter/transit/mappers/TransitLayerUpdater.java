package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import gnu.trove.set.TIntSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferIndexGenerator;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update the TransitLayer from a set of TimeTables. A shallow copy is made of the TransitLayer
 * (this also includes a shallow copy of the TripPatternsForDate map). TripPatterns are matched on
 * id and replaced by their updated versions. The realtime TransitLayer is then switched out with
 * the updated copy in an atomic operation. This ensures that any TransitLayer that is referenced
 * from the Graph is never changed.
 */
public class TransitLayerUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(TransitLayerUpdater.class);

  private final TransitModel transitModel;

  private final Map<LocalDate, TIntSet> serviceCodesRunningForDate;

  /**
   * Cache the TripPatternForDates indexed on the original TripPatterns in order to avoid this
   * expensive operation being done each time the update method is called.
   */
  private final Map<LocalDate, Map<TripPattern, TripPatternForDate>> tripPatternsStartingOnDateMapCache = new HashMap<>();

  /**
   * Cache the TripPatternForDate currently in use for a trip and service date. Only one TripPatternForDate is allowed
   * for a trip id and service date. This cache is used to clean up extra tripPatternsForDate.
   */
  private final Map<TripIdAndServiceDate, TripPatternForDate> tripPatternsForTripIdAndServiceDateCache = new HashMap<>();

  private final Map<LocalDate, Set<TripPatternForDate>> tripPatternsRunningOnDateMapCache = new HashMap<>();

  public TransitLayerUpdater(
    TransitModel transitModel,
    Map<LocalDate, TIntSet> serviceCodesRunningForDate
  ) {
    this.transitModel = transitModel;
    this.serviceCodesRunningForDate = serviceCodesRunningForDate;
  }

  public void update(
    Set<Timetable> updatedTimetables,
    Map<TripPattern, SortedSet<Timetable>> timetables
  ) {
    if (!transitModel.hasRealtimeTransitLayer()) {
      return;
    }

    long startTime = System.currentTimeMillis();

    // Make a shallow copy of the realtime transit layer. Only the objects that are copied will be
    // changed during this update process.
    TransitLayer realtimeTransitLayer = new TransitLayer(transitModel.getRealtimeTransitLayer());

    // Instantiate a TripPatternForDateMapper with the new TripPattern mappings
    TripPatternForDateMapper tripPatternForDateMapper = new TripPatternForDateMapper(
      serviceCodesRunningForDate
    );

    Set<LocalDate> datesToBeUpdated = new HashSet<>();
    SetMultimap<TripPattern, TripPatternForDate> newTripPatternsForDate = HashMultimap.create();
    SetMultimap<TripPattern, TripPatternForDate> oldTripPatternsForDate = HashMultimap.create();

    TransferIndexGenerator transferIndexGenerator = null;
    if (OTPFeature.TransferConstraints.isOn()) {
      transferIndexGenerator = realtimeTransitLayer.getTransferIndexGenerator();
    }
    Set<TripPatternForDate> previouslyUsedPatterns = new HashSet<>();
    // Map new TriPatternForDate and index for old and new TripPatternsForDate on service date
    for (Timetable timetable : updatedTimetables) {
      LocalDate date = timetable.getServiceDate();
      TripPattern tripPattern = timetable.getPattern();

      if (!tripPatternsStartingOnDateMapCache.containsKey(date)) {
        Map<TripPattern, TripPatternForDate> map = realtimeTransitLayer
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
        newTripPatternForDate = tripPatternForDateMapper.map(timetable, timetable.getServiceDate());
      } catch (IllegalArgumentException exception) {
        // There is some issue with finding the correct running period, using old pattern instead
        newTripPatternForDate = oldTripPatternForDate;
      }

      if (newTripPatternForDate != null) {
        tripPatternsStartingOnDateMapCache.get(date).put(tripPattern, newTripPatternForDate);
        newTripPatternsForDate.put(tripPattern, newTripPatternForDate);
        datesToBeUpdated.addAll(newTripPatternForDate.getRunningPeriodDates());
        if (transferIndexGenerator != null && tripPattern.isCreatedByRealtimeUpdater()) {
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
          TripPatternForDate previousTripPatternForDate = tripPatternsForTripIdAndServiceDateCache.put(
            id,
            newTripPatternForDate
          );
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
      tripPatternsRunningOnDateMapCache.computeIfAbsent(
        date,
        p -> new HashSet<>(realtimeTransitLayer.getTripPatternsRunningOnDateCopy(date))
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
          if (!pattern.isCreatedByRealtimeUpdater()) {
            continue;
          }
          var oldTimeTable = timetables.get(pattern);
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
            LOG.warn("Could not fetch timetable for {}", pattern);
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

      realtimeTransitLayer.replaceTripPatternsForDate(date, new ArrayList<>(patternsForDate));
    }

    if (transferIndexGenerator != null) {
      realtimeTransitLayer.setConstrainedTransfers(transferIndexGenerator.generateTransfers());
    }

    // Switch out the reference with the updated realtimeTransitLayer. This is synchronized to
    // guarantee that the reference is set after all the fields have been updated.
    transitModel.setRealtimeTransitLayer(realtimeTransitLayer);

    LOG.debug(
      "UPDATING {} tripPatterns took {} ms",
      updatedTimetables.size(),
      System.currentTimeMillis() - startTime
    );
  }
}
