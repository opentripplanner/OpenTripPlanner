package org.opentripplanner.transit.raptor.speed_test.test;

import org.opentripplanner.transit.raptor.speed_test.api.model.Itinerary;

import java.util.Collection;
import java.util.List;

import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrShort;


/**
 * Hold all information about a test case and its results.
 */
public class TestCase {
    public final String id;
    public final String description;
    public final int departureTime;
    public final int arrivalTime;
    public final int window;
    public final String origin;
    public final String fromPlace;
    public final double fromLat;
    public final double fromLon;
    public final String toPlace;
    public final double toLat;
    public final double toLon;
    public final String destination;
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
        this.origin = origin;
        this.fromPlace = fromPlace;
        this.fromLat = fromLat;
        this.fromLon = fromLon;
        this.destination = destination;
        this.toPlace = toPlace;
        this.toLat = toLat;
        this.toLon = toLon;
        this.results = testCaseResults == null ? new TestCaseResults(id) : testCaseResults;
    }

    @Override
    public String toString() {
        return String.format(
                "#%s %s - %s, (%.3f, %.3f) - (%.3f, %.3f), %s-%s(%s)",
                id, origin, destination,
                fromLat, fromLon,
                toLat, toLon,
                timeToStrShort(departureTime), timeToStrShort(arrivalTime), timeToStrShort(window)
        );
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
    List<org.opentripplanner.transit.raptor.speed_test.test.Result> actualResults() {
        return results.actualResults();
    }
}
