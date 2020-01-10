package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.transit.TransferLeg;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;


/**
 * The responsibility of this class is to encapsulate a Range Raptor travel request
 * search parameters.
 */
public class SearchParams {
    private static final int NOT_SET = -1;
    private static final int DEFAULT_SEARCH_WINDOW_IN_SECONDS = 60 * 60; // 1 hour

    private final int earliestDepartureTime;
    private final int latestArrivalTime;
    private final int searchWindowInSeconds;
    private final int boardSlackInSeconds;
    private final int numberOfAdditionalTransfers;
    private final double relaxCostAtDestination;
    private final boolean timetableEnabled;
    private final boolean waitAtBeginningEnabled;
    private final BitSet stopFilter;
    private final Collection<TransferLeg> accessLegs;
    private final Collection<TransferLeg> egressLegs;

    /**
     * Default values is defined in the default constructor.
     */
    private SearchParams() {
        earliestDepartureTime = NOT_SET;
        latestArrivalTime = NOT_SET;
        searchWindowInSeconds = DEFAULT_SEARCH_WINDOW_IN_SECONDS;
        boardSlackInSeconds = 60;
        numberOfAdditionalTransfers = 5;
        relaxCostAtDestination = NOT_SET;
        timetableEnabled = false;
        waitAtBeginningEnabled = true;
        stopFilter = null;
        accessLegs = Collections.emptyList();
        egressLegs = Collections.emptyList();
    }

    SearchParams(SearchParamsBuilder builder) {
        this.earliestDepartureTime = builder.earliestDepartureTime();
        this.latestArrivalTime = builder.latestArrivalTime();
        this.searchWindowInSeconds = builder.searchWindowInSeconds();
        this.boardSlackInSeconds = builder.boardSlackInSeconds();
        this.numberOfAdditionalTransfers = builder.numberOfAdditionalTransfers();
        this.relaxCostAtDestination = builder.relaxCostAtDestination();
        this.timetableEnabled = builder.timetableEnabled();
        this.waitAtBeginningEnabled = builder.waitAtBeginningEnabled();
        this.stopFilter = builder.stopFilter();
        this.accessLegs = java.util.List.copyOf(builder.accessLegs());
        this.egressLegs = java.util.List.copyOf(builder.egressLegs());
    }

    static SearchParams defaults() {
        return new SearchParams();
    }

    /**
     * TODO OTP2 Cleanup doc:
     * The beginning of the departure window, in seconds since midnight. Inclusive.
     * The earliest a journey may start in seconds since midnight. In the case of a 'depart after'
     * search this is a required. In the case of a 'arrive by' search this is optional.
     * <p/>
     * In Raptor terms this maps to the beginning of the departure window. The {@link #searchWindowInSeconds()}
     * is used to find the end of the time window.
     * <p/>
     * Required. Must be a positive integer, seconds since midnight(transit service time).
     * Required for 'depart after'. Must be a positive integer, seconds since midnight(transit service time).
     *
     */
    public int earliestDepartureTime() {
        return earliestDepartureTime;
    }

    /**
     * TODO OTP2 Cleanup doc:
     * The end of the departure window, in seconds since midnight. Exclusive.
     * The latest a journey may arrive in seconds since midnight. In the case of a 'arrive by'
     * search this is a required. In the case of a 'depart after' search this is optional.
     * <p/>
     * Required. Must be a positive integer, seconds since midnight(transit service time).
     * In Raptor terms this maps to the beginning of the departure window of a reverse search. The
     * {@link #searchWindowInSeconds()} is used to find the end of the time window.
     * <p/>
     * Required for 'arrive by'. Must be a positive integer, seconds since midnight(transit service time).
     */
    public int latestArrivalTime() {
        return latestArrivalTime;
    }

    /**
     * TODO OTP2 Cleanup doc:
     * The time window used to search. The unit is seconds. For a *depart by search*, this is
     * added to the 'earliestDepartureTime' to find the 'latestDepartureTime'. For a *arrive
     * by search* this is used to calculate the 'earliestArrivalTime'. The algorithm will find
     * all optimal travels within the given time window.
     * <p/>
     * Set the search window to 0 (zero) to run 1 iteration.
     * <p/>
     * Required. Must be a positive integer or 0(zero).
     */
    public int searchWindowInSeconds() {
        return searchWindowInSeconds;
    }


    /**
     * TODO OTP2 - Describe this
     * <p/>
     * Optional. Default value is 'TIME_TABLE'.
     */
    public ArrivalAndDeparturePreference arrivalAndDeparturePreference() {
        return ArrivalAndDeparturePreference.TIME_TABLE;
    }

    /**
     * The minimum wait time for transit boarding to account for schedule variation.
     * This is added between transits, between transfer and transit, and between access "walk" and transit.
     * <p/>
     * The default value is 60.
     */
    public int boardSlackInSeconds() {
        return boardSlackInSeconds;
    }

    /**
     * RangeRaptor is designed to search until the destination is reached and then
     * {@code numberOfAdditionalTransfers} more rounds.
     * <p/>
     * The default value is 5.
     */
    public int numberOfAdditionalTransfers() {
        return numberOfAdditionalTransfers;
    }

    /**
     * This accept none optimal trips if they are close enough - if and only if they represent an optimal path
     * for their given iteration. I other words this slack only relax the pareto comparison at the destination.
     * <p/>
     * Let {@code c} be the existing minimum pareto optimal cost to to beat. Then a trip with cost {@code c'}
     * is accepted if the following is true:
     * <pre>
     * c' < Math.round(c * relaxCostAtDestination)
     * </pre>
     * If the values is less then 0.0 a normal '<' comparison is performed.
     * <p/>
     * TODO - When setting this above 1.0, we get some unwanted results. We should have a filter to remove those
     * TODO - results. See issue https://github.com/entur/r5/issues/28
     * <p/>
     * The default value is -1.0 (disabled)
     */
    public double relaxCostAtDestination() {
        return relaxCostAtDestination;
    }

    /**
     * Time table allow a Journey to be included in the result if it depart from the origin
     * AFTER another Journey, even if the first departure have lower cost, number of transfers,
     * and shorter travel time. For two Journeys that depart at the same time only the best one
     * will be included (both if they are mutually dominating each other).
     * <p/>
     * Setting this parameter to "TRUE" will increase the number of paths returned. The
     * performance impact is small since the check only affect the pareto check at the
     * destination.
     * <p/>
     * The default value is FALSE.
     */
    public boolean timetableEnabled() {
        return timetableEnabled;
    }

    /**
     * Allow a Journey to depart outside the search window. This parameter allow the first
     * Range Raptor iteration to "wait" at the first stop (access stop) to board the first
     * trip. The "access leg" is time-shifted, and the origin departure time will be outside
     * the search window.
     * <p/>
     * Setting this parameter to "FALSE" make it possible to concatenate the results from two
     * sequential searches without getting duplicate results. For example you can search form
     * 08:00 to 12:00, and from 12:00 to 16:00 in parallel and merge the result. The result will
     * not have any duplicates, but there is no guarantee that the set will be pareto optimal.
     * There can be journeys in one set that dominates journeys in the other set.
     * <p/>
     * The default value is TRUE.
     */
    public boolean waitAtBeginningEnabled() {
        return waitAtBeginningEnabled;
    }

    /**
     * Restrict the search to a limited set of stops. Range Raptor will check the
     * provided stop filter every time it arrive at a stop, dropping all arrivals
     * (and paths) during the search.
     * <p/>
     * Set bit n to TRUE to enable stop at index n.
     * </p>
     * The default is {@code null}
     */
    public BitSet stopFilter() {
        return stopFilter;
    }

    /**
     * Times to access each transit stop using the street network in seconds.
     * <p/>
     * Required, at least one access leg must exist.
     */
    public Collection<TransferLeg> accessLegs() {
        return accessLegs;
    }

    /**
     * List of all possible egress stops and time to reach destination in seconds.
     * <p>
     * NOTE! The {@link TransferLeg#stop()} is the stop where the egress leg
     * start, NOT the destination - think of it as a reversed leg.
     * <p/>
     * Required, at least one egress leg must exist.
     */
    public Collection<TransferLeg> egressLegs() {
        return egressLegs;
    }

    @Override
    public String toString() {
        return "SearchParams{" +
                "earliestDepartureTime=" + TimeUtils.timeToStrCompact(earliestDepartureTime, NOT_SET) +
                ", latestArrivalTime=" + TimeUtils.timeToStrCompact(latestArrivalTime, NOT_SET) +
                ", searchWindowInSeconds=" + TimeUtils.timeToStrCompact(searchWindowInSeconds) +
                ", accessLegs=" + accessLegs +
                ", egressLegs=" + egressLegs +
                ", boardSlackInSeconds=" + boardSlackInSeconds +
                ", numberOfAdditionalTransfers=" + numberOfAdditionalTransfers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchParams that = (SearchParams) o;
        return earliestDepartureTime == that.earliestDepartureTime &&
                latestArrivalTime == that.latestArrivalTime &&
                searchWindowInSeconds == that.searchWindowInSeconds &&
                boardSlackInSeconds == that.boardSlackInSeconds &&
                numberOfAdditionalTransfers == that.numberOfAdditionalTransfers &&
                accessLegs.equals(that.accessLegs) &&
                egressLegs.equals(that.egressLegs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                earliestDepartureTime, latestArrivalTime, searchWindowInSeconds, accessLegs,
                egressLegs, boardSlackInSeconds, numberOfAdditionalTransfers
        );
    }


    /* private methods */

    void verify() {
        assertProperty(!accessLegs.isEmpty(), "At least one 'accessLegs' is required.");
        assertProperty(!egressLegs.isEmpty(), "At least one 'egressLegs' is required.");
    }

    private void assertProperty(boolean predicate, String errorMessage) {
        if(!predicate) {
            throw new IllegalArgumentException(RangeRaptorRequest.class.getSimpleName()  + " error: " + errorMessage);
        }
    }
}
