package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.transit.raptor.speed_test.api.model.Itinerary;

import java.util.Collection;
import java.util.List;

import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrShort;


/**
 * Hold all information about a test case and its results.
 */
public class TestCase {
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
        return String.format(
                "#%s %s - %s, (%.3f, %.3f) - (%.3f, %.3f), %s-%s(%s)",
                id, fromPlace.getDescription(), toPlace.getDescription(),
                fromPlace.getLat(), fromPlace.getLon(),
                toPlace.getLat(), toPlace.getLon(),
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
    List<org.opentripplanner.transit.raptor.speed_test.testcase.Result> actualResults() {
        return results.actualResults();
    }
}
