package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.StopIndexMapper.listStopIndexesForTripPattern;

class TripPatternMapper {
    static Multimap<Integer, TripPattern> mapPatternsByServiceCode(
            Map<Stop, Integer> indexByStop,
            Collection<org.opentripplanner.routing.edgetype.TripPattern> originalTripPatterns
    ) {
        Multimap<Integer, TripPattern> patternsByServiceCode = HashMultimap.create();

        int patternId = 0;
        for (org.opentripplanner.routing.edgetype.TripPattern tripPattern : originalTripPatterns) {
            List<TripSchedule> tripSchedules = new ArrayList<>();

            List<TripTimes> sortedTripTimes = tripPattern.scheduledTimetable.tripTimes.stream()
                    .sorted(Comparator.comparing(t -> t.getArrivalTime(0)))
                    .collect(Collectors.toList());

            TripPattern newTripPattern = new TripPattern(
                    patternId++,
                    tripSchedules,
                    tripPattern.mode,
                    listStopIndexesForTripPattern(tripPattern, indexByStop)
            );

            for (TripTimes tripTimes : sortedTripTimes) {
                patternsByServiceCode.put(tripTimes.serviceCode, newTripPattern);
                tripSchedules.add(TripScheduleMapper.map(tripPattern, tripTimes));
            }

        }
        return patternsByServiceCode;
    }


    static HashMap<LocalDate, List<TripPatternForDate>> mapTripPatternsByStopDate(
            Multimap<Integer, TripPattern> patternsByServiceCode,
            Multimap<LocalDate, Integer> serviceCodesByLocalDates
    ) {
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate = new HashMap<>();

        for (LocalDate localDate : serviceCodesByLocalDates.keySet()) {
            Set<Integer> services = new HashSet<>(serviceCodesByLocalDates.get(localDate));

            List<TripPattern> filteredPatterns = services
                    .stream()
                    .map(patternsByServiceCode::get)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            List<TripPatternForDate> tripPatternsForDate = new ArrayList<>();

            for (TripPattern tripPattern : filteredPatterns) {
                List<TripSchedule> tripSchedules = new ArrayList<>(tripPattern.getTripSchedules());

                tripSchedules = tripSchedules.stream()
                        .filter(t -> services.contains(t.getServiceCode()))
                        .collect(Collectors.toList());

                TripPatternForDate tripPatternForDate = new TripPatternForDate(
                        tripPattern, tripSchedules
                );

                tripPatternsForDate.add(tripPatternForDate);
            }

            tripPatternsByStopByDate.put(localDate, tripPatternsForDate);
        }

        // Sort by TripPattern for easier merging in RaptorRoutingRequestTransitData
        for (List<TripPatternForDate> list : tripPatternsByStopByDate.values()) {
            list.sort(Comparator.comparingInt(p -> p.getTripPattern().getId()));
        }
        return tripPatternsByStopByDate;
    }

}
