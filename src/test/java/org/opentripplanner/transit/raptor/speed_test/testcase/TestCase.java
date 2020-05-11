package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.transit.raptor.speed_test.model.Itinerary;
import org.opentripplanner.transit.raptor.speed_test.model.Place;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import java.util.Collection;
import java.util.List;


/**
 * Hold all information about a test case and its results.
 */
public class TestCase {
    public static final int NOT_SET = -1;

    private static final String FEED_ID = "RB";
    public final String id;
    public final String description;
    public final int departureTime;
    public final int arrivalTime;
    public final int window;
    public final Place fromPlace;
    public final Place toPlace;
    final TestCaseResults results;

    TestCase(
            String id, int departureTime, int arrivalTime, int window, String description,
            String origin, String fromPlace, double fromLat, double fromLon,
            String destination, String toPlace, double toLat, double toLon,
            TestCaseResults testCaseResults
    ) {
        this.id = id;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.window = window;
        this.description = description;
        this.fromPlace = new Place(origin, FEED_ID, fromPlace, fromLat, fromLon);
        this.toPlace = new Place(destination, FEED_ID, toPlace, toLat, toLon);
        this.results = testCaseResults == null ? new TestCaseResults(id) : testCaseResults;
    }

    @Override
    public String toString() {
        return toString(NOT_SET, NOT_SET, NOT_SET);
    }

    public String toString(int departureTime, int arrivalTime, int window) {
        return String.format(
                "#%s %s - %s, %s - %s, %s-%s(%s)",
                id, fromPlace.name, toPlace.name,
                fromPlace.coordinate,
                toPlace.coordinate,
                timeToString(this.departureTime, departureTime),
                timeToString(this.arrivalTime, arrivalTime),
                durationToString(this.window, window)
        );
    }

    private String timeToString(int orgTime, int calcTime) {
        return orgTime == NOT_SET && calcTime > 0
                ? TimeUtils.timeToStrCompact(calcTime) + "*"
                : TimeUtils.timeToStrCompact(orgTime);
    }

    private String durationToString(int orgTime, int calcTime) {
        return orgTime == NOT_SET && calcTime > 0
            ? TimeUtils.durationToStr(calcTime) + "*"
            : TimeUtils.durationToStr(orgTime);
    }

    /**
     * Verify the result by matching it with the {@code expectedResult} from the csv file.
     */
    public void assertResult(Collection<Itinerary> itineraries) {
        results.matchItineraries(itineraries);

        if (results.failed()) {
            throw new TestCaseFailedException();
        }
    }

    /**
     * All test results are OK.
     */
    public boolean success() {
        return results.success();
    }

    public void printResults() {
        if(!results.actualResults().isEmpty()) {
            System.err.println(results.toString());
        }
    }

    /**
     * The test case is not run or no itineraries found.
     */
    boolean notRun() {
        return results.noResults();
    }

    /**
     * List all results found for this testcase.
     */
    List<Result> actualResults() {
        return results.actualResults();
    }
}
