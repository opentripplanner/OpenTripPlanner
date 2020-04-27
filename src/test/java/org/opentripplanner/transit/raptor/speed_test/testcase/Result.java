package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is responsible for holding information about a test result - a single
 * itinerary. The result can be expected or actual, both represented by this class.
 */
class Result implements Comparable<Result> {
    private static final Pattern STOPS_PATTERN = Pattern.compile(" ~ (\\d+) ~ ");
    /**
     * The status is not final; This allows to update the status when matching expected and actual results.
     */
    TestStatus status = TestStatus.NA;
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

    void setStatus(TestStatus status) {
        this.status = status;
    }

    @Override
    public int compareTo(Result o) {
        // Sort first arrival first
        int res = endTime.compareTo(o.endTime);
        if(res != 0) { return res; }

        // Sort latest departure first
        res = -startTime.compareTo(o.startTime);
        if(res != 0) { return res; }

        // Sort lowest cost first
        res = -cost.compareTo(o.cost);
        if(res != 0) { return res; }

        // Sort based on route
        res = compare(routes, o.routes, String::compareTo);
        if(res != 0) { return res; }

        // Sort based on stops
        res = compare(stops, o.stops, Integer::compareTo);
        return res;
    }

    /**
     * Compare if two results are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result result = (Result) o;
        return Objects.equals(endTime, result.endTime) &&
                Objects.equals(endTime, result.endTime) &&
                compareCost(cost, result.cost) &&
                compare(routes, result.routes, String::compareTo) == 0 &&
                compare(stops, result.stops, Integer::compareTo) == 0;
    }

    private boolean compareCost(Integer c1, Integer c2) {
        // Ignore cost from comparison if not computed
        if(c1 == null || c1 == 0) return true;
        if(c2 == null || c2 == 0) return true;
        return c1.equals(c2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, cost, details);
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
                startTimeAsStr(),
                endTimeAsStr(),
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
        return TimeUtils.durationToStr(duration);
    }

    public String startTimeAsStr() {
        return TimeUtils.timeToStrCompact(startTime, -1);
    }

    public String endTimeAsStr() {
        return TimeUtils.timeToStrCompact(endTime, -1);
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
