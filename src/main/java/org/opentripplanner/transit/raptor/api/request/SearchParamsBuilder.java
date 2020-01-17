package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransferLeg;

import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

/**
 * Mutable version of {@link SearchParams}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class SearchParamsBuilder<T extends RaptorTripSchedule> {

    private final RaptorRequestBuilder<T> parent;
    // Search
    private int earliestDepartureTime;
    private int latestArrivalTime;
    private int searchWindowInSeconds;
    private int boardSlackInSeconds;
    private int numberOfAdditionalTransfers;
    private double relaxCostAtDestination;
    private boolean timetableEnabled;
    // TODO OTP2 - Rename, this allow the search window to be extended bejond the
    //           - "latest departure time".
    private boolean allowWaitingBetweenAccessAndTransit;
    private BitSet stopFilter;
    private final Collection<TransferLeg> accessLegs = new ArrayList<>();
    private final Collection<TransferLeg> egressLegs = new ArrayList<>();

    public SearchParamsBuilder(RaptorRequestBuilder<T> parent, SearchParams defaults) {
        this.parent = parent;
        this.earliestDepartureTime = defaults.earliestDepartureTime();
        this.latestArrivalTime = defaults.latestArrivalTime();
        this.searchWindowInSeconds = defaults.searchWindowInSeconds();
        this.boardSlackInSeconds = defaults.boardSlackInSeconds();
        this.numberOfAdditionalTransfers = defaults.numberOfAdditionalTransfers();
        this.relaxCostAtDestination = defaults.relaxCostAtDestination();
        this.timetableEnabled = defaults.timetableEnabled();
        this.allowWaitingBetweenAccessAndTransit = defaults.allowWaitingBetweenAccessAndTransit();
        this.stopFilter = defaults.stopFilter();
        this.accessLegs.addAll(defaults.accessLegs());
        this.egressLegs.addAll(defaults.egressLegs());
    }

    public int earliestDepartureTime() {
        return earliestDepartureTime;
    }

    public SearchParamsBuilder<T> earliestDepartureTime(int earliestDepartureTime) {
        this.earliestDepartureTime = earliestDepartureTime;
        return this;
    }

    public int latestArrivalTime() {
        return latestArrivalTime;
    }

    public SearchParamsBuilder<T> latestArrivalTime(int latestArrivalTime) {
        this.latestArrivalTime = latestArrivalTime;
        return this;
    }

    public int searchWindowInSeconds() {
        return searchWindowInSeconds;
    }

    public SearchParamsBuilder<T> searchOneIterationOnly() {
        return searchWindowInSeconds(0);
    }

    public SearchParamsBuilder<T> searchWindowInSeconds(int searchWindowInSeconds) {
        this.searchWindowInSeconds = searchWindowInSeconds;
        return this;
    }

    public SearchParamsBuilder<T> searchWindow(Duration searchWindow) {
        this.searchWindowInSeconds = searchWindow == null
                ? SearchParams.NOT_SET
                : (int)searchWindow.toSeconds();
        return this;
    }

    public int boardSlackInSeconds() {
        return boardSlackInSeconds;
    }

    public SearchParamsBuilder<T> boardSlackInSeconds(int boardSlackInSeconds) {
        this.boardSlackInSeconds = boardSlackInSeconds;
        return this;
    }


    public int numberOfAdditionalTransfers() {
        return numberOfAdditionalTransfers;
    }

    public SearchParamsBuilder<T> numberOfAdditionalTransfers(int numberOfAdditionalTransfers) {
        this.numberOfAdditionalTransfers = numberOfAdditionalTransfers;
        return this;
    }

    public double relaxCostAtDestination() {
        return relaxCostAtDestination;
    }

    public SearchParamsBuilder<T> relaxCostAtDestination(double relaxCostAtDestination) {
        this.relaxCostAtDestination = relaxCostAtDestination;
        return this;
    }

    public boolean timetableEnabled() {
        return timetableEnabled;
    }

    public SearchParamsBuilder<T> timetableEnabled(boolean enable) {
        this.timetableEnabled = enable;
        return this;
    }

    public boolean allowWaitingBetweenAccessAndTransit() {
        return allowWaitingBetweenAccessAndTransit;
    }

    public SearchParamsBuilder<T> allowWaitingBetweenAccessAndTransit(boolean enable) {
        this.allowWaitingBetweenAccessAndTransit = enable;
        return this;
    }

    public BitSet stopFilter() {
        return stopFilter;
    }

    public SearchParamsBuilder<T> stopFilter(BitSet stopFilter) {
        this.stopFilter = stopFilter;
        return this;
    }

    public Collection<TransferLeg> accessLegs() {
        return accessLegs;
    }

    public SearchParamsBuilder<T> addAccessStop(TransferLeg accessLeg) {
        this.accessLegs.add(accessLeg);
        return this;
    }

    public SearchParamsBuilder<T> addAccessStops(Iterable<TransferLeg> accessLegs) {
        for (TransferLeg it : accessLegs) {
            addAccessStop(it);
        }
        return this;
    }

    public Collection<TransferLeg> egressLegs() {
        return egressLegs;
    }

    public SearchParamsBuilder<T> addEgressStop(TransferLeg egressLeg) {
        this.egressLegs.add(egressLeg);
        return this;
    }

    public SearchParamsBuilder<T> addEgressStops(Iterable<TransferLeg> egressLegs) {
        for (TransferLeg it : egressLegs) {
            addEgressStop(it);
        }
        return this;
    }

    public RaptorRequest<T> build() {
        return parent.build();
    }

    /** This is public to allow tests to build just search params */
    public SearchParams buildSearchParam() {
        return new SearchParams(this);
    }
}
