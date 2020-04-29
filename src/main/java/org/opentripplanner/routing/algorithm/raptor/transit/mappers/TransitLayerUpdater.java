package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import gnu.trove.set.TIntSet;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.graph.Graph;
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
 * Update the TransitLayer from a set of TimeTables. A shallow copy is made of the TransitLayer
 * (this also includes a shallow copy of the TripPatternsForDate map). TripPatterns are matched on
 * id and replaced by their updated versions. The realtime TransitLayer is then switched out
 * with the updated copy in an atomic operation. This ensures that any TransitLayer that is
 * referenced from the Graph is never changed.
 */
public class TransitLayerUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(TransitLayerUpdater.class);

  private final Graph graph;

  private final Map<ServiceDate, TIntSet> serviceCodesRunningForDate;

  /**
   * Cache the TripPatternForDates indexed on the original TripPatterns in order to avoid
   * this expensive operation being done each time the update method is called.
   */
  private final Map<LocalDate, Map<org.opentripplanner.model.TripPattern, TripPatternForDate>>
                tripPatternForDateMapCache = new HashMap<>();

  public TransitLayerUpdater(
      Graph graph,
      Map<ServiceDate, TIntSet> serviceCodesRunningForDate
  ) {
    this.graph = graph;
    this.serviceCodesRunningForDate = serviceCodesRunningForDate;
  }

  public void update(Set<Timetable> updatedTimetables) {
    if (!graph.hasRealtimeTransitLayer()) { return; }

    // Make a shallow copy of the realtime transit layer. Only the objects that are copied will be
    // changed during this update process.
    TransitLayer realtimeTransitLayer = new TransitLayer(graph.getRealtimeTransitLayer());

    double startTime = System.currentTimeMillis();

    // Map TripPatterns for this update to Raptor TripPatterns
    final Map<org.opentripplanner.model.TripPattern, TripPatternWithRaptorStopIndexes>
        newTripPatternForOld = mapOldTripPatternToRaptorTripPattern(
        realtimeTransitLayer.getStopIndex(),
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

      List<TripPatternForDate> patternsForDate =
          realtimeTransitLayer.getTripPatternsForDateCopy(date);

      if (patternsForDate == null) {
        continue;
      }

      Map<org.opentripplanner.model.TripPattern, TripPatternForDate> patternsForDateMap =
          tripPatternForDateMapCache.computeIfAbsent(date, p -> patternsForDate
          .stream()
          .collect(Collectors.toMap(t -> t.getTripPattern().getPattern(), t -> t)));

      for (Timetable timetable : timetablesForDate) {
        TripPatternForDate tripPatternForDate = tripPatternForDateMapper.map(
            timetable,
            timetable.serviceDate
        );
        if (tripPatternForDate != null) {
          patternsForDateMap.put(timetable.pattern, tripPatternForDate);
        }
      }

      realtimeTransitLayer.replaceTripPatternsForDate(
          date,
          new ArrayList<>(patternsForDateMap.values())
      );

      // Switch out the reference with the updated realtimeTransitLayer. This is synchronized to
      // guarantee that the reference is set after all the fields have been updated.
      graph.setRealtimeTransitLayer(realtimeTransitLayer);

      LOG.debug(
          "UPDATING {} tripPatterns took {} ms",
          updatedTimetables.size(),
          System.currentTimeMillis() - startTime
      );
    }
  }
}
