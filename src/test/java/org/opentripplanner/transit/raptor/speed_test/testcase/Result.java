package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.transit.raptor.util.TimeUtils;

import java.util.ArrayList;
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
    private static final Pattern STOPS_PATTERN = Pattern.compile(" - (\\d+) - ");
    /**
     * The status is not final; This allows to update the status when matching expected and actual results.
     */
    TestStatus status = TestStatus.NA;
    final String testCaseId;
    final Integer transfers;
    final Integer duration;
    final Integer cost;
    final Integer walkDistance;
    final String startTime;
    final String endTime;
    final Set<String> agencies = new TreeSet<>();
    final Set<String> modes = new TreeSet<>();
    final List<String> routes = new ArrayList<>();
    final List<Integer> stops = new ArrayList<>();
    final String details;

    Result(String testCaseId, Integer transfers, Integer duration, Integer cost, Integer walkDistance, String startTime, String endTime, String details) {
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
        if(res == 0) {
            // Sort latest departure first
            res = -startTime.compareTo(o.startTime);
        }
        if(res == 0) {
            // Sort trips with the same patterns in the beginning together
            res = details.compareTo(o.details);
        }
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
        return Objects.equals(startTime, result.startTime) &&
                Objects.equals(endTime, result.endTime) &&
                compareCost(cost, result.cost) &&
                Objects.equals(details, result.details);
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

    /**
     * Create a compact representation of an itinerary.
     * Example:
     * <pre>
     * 2 1:21:00 953m 09:22:00 10:43:10 -- WALK 7:12 - 37358 - NW180 09:30 10:20 - 3551 - WALK 3:10
     * </pre>
     * Format:
     * <pre>
     * [Transfers] [Duration] [cost] [walk distance meters] [start time] [end time] -- [details]
     * </pre>
     */
    @Override
    public String toString() {
        return String.format(
                "%d %s %d %dm %s %s -- %s",
                transfers,
                TimeUtils.timeToStrCompact(duration),
                cost,
                walkDistance,
                startTime,
                endTime,
                details
        );
    }

    private static List<Integer> parseStops(String details) {
        List<Integer> stops = new ArrayList<>();
        // WALK 0:44 - 87540 - BUS NX1 06:25 08:50 - 87244 - WALK 0:20
        Matcher m = STOPS_PATTERN.matcher(details);

        while (m.find()) {
            stops.add(Integer.parseInt(m.group(1)));
        }
        return stops;
    }
}
