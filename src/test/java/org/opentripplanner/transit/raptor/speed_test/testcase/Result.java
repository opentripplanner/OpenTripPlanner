package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.util.CompositeComparator;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is responsible for holding information about a test result - a single
 * itinerary. The result can be expected or actual, both represented by this class.
 */
class Result {
    private static final Pattern STOPS_PATTERN = Pattern.compile(" ~ (\\d+) ~ ");
    /**
     * The status is not final; This allows to update the status when matching expected and actual results.
     */
    final String testCaseId;
    final Integer transfers;
    final Integer duration;
    final Integer cost;
    final Integer walkDistance;
    final Integer startTime;
    final Integer endTime;
    final Set<String> agencies = new TreeSet<>();
    final Set<TraverseMode> modes = EnumSet.noneOf(TraverseMode.class);
    final List<String> routes = new ArrayList<>();
    final List<Integer> stops = new ArrayList<>();
    final String details;

    Result(String testCaseId, Integer transfers, Integer duration, Integer cost, Integer walkDistance, Integer startTime, Integer endTime, String details) {
        this.testCaseId = testCaseId;
        this.transfers = transfers;
        this.duration = duration;
        this.cost = cost;
        this.walkDistance = walkDistance;
        this.startTime = startTime;
        this.endTime = endTime;
        this.details = details;
        this.stops.addAll(parseStops(details));
    }

    public static Comparator<Result> comparator(boolean skipCost) {
        return new CompositeComparator<>(
            Comparator.comparing(r -> r.endTime),
            Comparator.comparing(r -> -r.startTime),
            compareCost(skipCost),
            (r1, r2) -> compare(r1.routes, r2.routes, String::compareTo),
            (r1, r2) -> compare(r1.stops, r2.stops, Integer::compareTo)
        );
    }

    private static Comparator<Result> compareCost(boolean skipCost) {
        return (r1, r2) -> {
            if(skipCost) { return 0; };
            if(r1.cost == null || r1.cost.equals(0)) { return 0; }
            if(r2.cost == null || r2.cost.equals(0)) { return 0; }
            return -(r2.cost - r1.cost);
        };
    }

    /** Create a compact String representation of an itinerary. */
    @Override
    public String toString() {
        return String.format(
                "%d %s %d %dm %s %s -- %s",
                transfers,
                durationAsStr(),
                cost,
                walkDistance,
                TimeUtils.timeToStrCompact(startTime),
                TimeUtils.timeToStrCompact(endTime),
                details
        );
    }

    private static List<Integer> parseStops(String details) {
        List<Integer> stops = new ArrayList<>();
        // WALK 0:44 ~ 87540 ~ BUS NX1 06:25 08:50 ~ 87244  WALK 0:20
        Matcher m = STOPS_PATTERN.matcher(details);

        while (m.find()) {
            stops.add(Integer.parseInt(m.group(1)));
        }
        return stops;
    }

    public String durationAsStr() {
      return DurationUtils.durationToStr(duration);
    }

    static <T> int compare(List<T> a, List<T> b, Comparator<T> comparator) {
        int size = Math.min(a.size(), b.size());
        for (int i = 0; i < size; i++) {
            int c = comparator.compare(a.get(i), b.get(i));
            if(c != 0) {
                return c;
            }
        }
        return a.size() - b.size();
    }
}
