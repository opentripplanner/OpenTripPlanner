package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.TripScheduleWrapperImpl;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO extract common logic between this and TransitLayerMapper
public class TransitLayerUpdater {
  public static void update(Set<Timetable> updatedTimetables, TransitLayer transitLayer ) {

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

      // TODO Keep this in the realtimeTransitLayer to increase performance
      Map<org.opentripplanner.model.TripPattern, TripPatternForDate> patternsForDateMap =
          patternsForDate.stream()
              .collect(Collectors.toMap(t -> t.getTripPattern().getOriginalTripPattern(), t -> t));

      for (Timetable timetable : timetablesForDate) {
        TripPattern newTripPattern = new TripPattern(
            transitLayer.incrementAndGetPatternId(),
            null,
            timetable.pattern.mode,
            transitLayer.getStopIndex().listStopIndexesForStops(timetable.pattern.stopPattern.stops),
            timetable.pattern
        );

        // TODO Sort these using an algorithm suited for sorting nearly sorted lists
        List<TripSchedule> newTripSchedules = new ArrayList<>();
        List<TripTimes> sortedTripTimes = TransitLayerMapper.getSortedTripTimes(timetable);
        for (TripTimes tripTimes : sortedTripTimes) {
          if (tripTimes.getRealTimeState() == RealTimeState.CANCELED) {
            continue;
          }
          TripSchedule tripSchedule = new TripScheduleWrapperImpl(tripTimes,
              timetable.pattern
          );

          newTripSchedules.add(tripSchedule);
        }

        TripPatternForDate tripPatternForDate = new TripPatternForDate(newTripPattern,
            newTripSchedules,
            date
        );

        patternsForDateMap.put(timetable.pattern, tripPatternForDate);
      }

      transitLayer.replaceTripPatternsForDate(
          date,
          new ArrayList<>(patternsForDateMap.values())
      );
    }
  }
}
