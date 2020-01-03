package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import gnu.trove.set.TIntSet;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TripPatternMapper.mapOldTripPatternToRaptorTripPattern;

/**
 * Update the TransitLayer from a set of TimeTables. TripPatterns are matched on id and replaced
 * by their updated versions. A list of TripPatternsForDate is copied from the TransitLayer for
 * each relevant date, updated and then atomically replaced in the TransitLayer.
 */
public class TransitLayerUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(TransitLayerUpdater.class);

  private final TransitLayer transitLayer;

  private final Map<ServiceDate, TIntSet> serviceCodesRunningForDate;

  /**
   * Cache the TripPatternForDates indexed on the original TripPatterns in order to avoid
   * this expensive operation being done each time the update method is called.
   */
  private final Map<LocalDate, Map<org.opentripplanner.model.TripPattern, TripPatternForDate>>
                tripPatternForDateMapCache = new HashMap<>();

  public TransitLayerUpdater(
      TransitLayer transitLayer,
      Map<ServiceDate, TIntSet> serviceCodesRunningForDate
  ) {
    this.transitLayer = transitLayer;
    this.serviceCodesRunningForDate = serviceCodesRunningForDate;
  }

  public void update(Set<Timetable> updatedTimetables) {
    if (transitLayer == null) { return; }

    double startTime = System.currentTimeMillis();

    // Map TripPatterns for this update to Raptor TripPatterns
    final Map<org.opentripplanner.model.TripPattern, TripPattern>
        newTripPatternForOld = mapOldTripPatternToRaptorTripPattern(
            transitLayer.getStopIndex(),
            updatedTimetables.stream().map(t -> t.pattern).collect(Collectors.toSet()
        )
    );

    // Instantiate a TripPatternForDateMapper with the new TripPattern mappings
    TripPatternForDateMapper tripPatternForDateMapper = new TripPatternForDateMapper(
        serviceCodesRunningForDate,
        newTripPatternForOld
    );

    // Index updated timetables by date
    @SuppressWarnings("ConstantConditions")
    Multimap<LocalDate, Timetable> timetablesByDate = Multimaps.index(updatedTimetables,
        t -> ServiceCalendarMapper.localDateFromServiceDate(t.serviceDate)
    );

    for (LocalDate date : timetablesByDate.keySet()) {
      Collection<Timetable> timetablesForDate = timetablesByDate.get(date);

      List<TripPatternForDate> patternsForDate = transitLayer.getTripPatternsForDateCopy(date);

      if (patternsForDate == null) {
        continue;
      }

      Map<org.opentripplanner.model.TripPattern, TripPatternForDate> patternsForDateMap =
          tripPatternForDateMapCache.computeIfAbsent(date, p -> patternsForDate
          .stream()
          .collect(Collectors.toMap(t -> t.getTripPattern().getOriginalTripPattern(), t -> t)));

      for (Timetable timetable : timetablesForDate) {
        TripPatternForDate tripPatternForDate = tripPatternForDateMapper.map(
            timetable,
            timetable.serviceDate
        );
        if (tripPatternForDate != null) {
          patternsForDateMap.put(timetable.pattern, tripPatternForDate);
        }
      }

      transitLayer.replaceTripPatternsForDate(
          date,
          new ArrayList<>(patternsForDateMap.values())
      );

      LOG.debug(
          "UPDATING {} tripPatterns took {} ms",
          updatedTimetables.size(),
          System.currentTimeMillis() - startTime
      );
    }
  }
}
