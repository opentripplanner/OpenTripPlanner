package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.TripScheduleImpl;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
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

    /**
     * Convert pre-Raptor TripPatterns to Raptor TripPatterns, adding the new TripPatterns to a Map keyed on the service
     * codes for which the TripPattern has some information. Each new TripPattern may appear in more than one key.
     * Each TripPattern has N TripTimes (from the N trips within the pattern). Each of those trips can be running on a
     * different service code, so each TripPattern will appear in 1 to N services.
     */
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
                tripSchedules.add(new TripScheduleImpl(tripTimes, tripPattern));
            }

        }
        return patternsByServiceCode;
    }


    /**
     * Groups each pattern by its active dates for quick lookup when making a request at a specific date (or dates).
     * We create a new object called TripPatternForDate that contains only the TripSchedules for that date.
     */
    static HashMap<LocalDate, List<TripPatternForDate>> groupTripPatternsPerDateAndFilterSchedules(
            Multimap<Integer, TripPattern> patternsByServiceCode,
            Multimap<LocalDate, Integer> serviceCodesByLocalDates
    ) {
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate = new HashMap<>();

        // For each date:
        // Get service codes active on that date
        // Get all trip patterns active on those service codes
        // Filter the TripSchedules on each of those TripPatterns to only those trips active on the given service code.
        // Group those resulting TripPatternForDate by their dates.
        for (LocalDate localDate : serviceCodesByLocalDates.keySet()) {
            Set<Integer> services = new HashSet<>(serviceCodesByLocalDates.get(localDate));

            List<TripPattern> filteredPatterns = services
                    .stream()
                    .map(patternsByServiceCode::get)
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());

            List<TripPatternForDate> tripPatternsForDate = new ArrayList<>();

            for (TripPattern tripPattern : filteredPatterns) {
                List<TripSchedule> tripSchedules = new ArrayList<>(tripPattern.getTripSchedules());

                tripSchedules = tripSchedules.stream()
                        .filter(t -> services.contains(t.getServiceCode()))
                        .collect(Collectors.toList());

                TripPatternForDate tripPatternForDate = new TripPatternForDate(
                        tripPattern, tripSchedules, localDate
                );

                tripPatternsForDate.add(tripPatternForDate);
            }

            tripPatternsByStopByDate.put(localDate, tripPatternsForDate);
        }

        return tripPatternsByStopByDate;
    }

}
