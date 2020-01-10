package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.transit.TransferLeg;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

/**
 * Mutable version of {@link SearchParams}.
 */
public class SearchParamsBuilder {
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
    private boolean waitAtBeginningEnabled;
    private BitSet stopFilter;
    private final Collection<TransferLeg> accessLegs = new ArrayList<>();
    private final Collection<TransferLeg> egressLegs = new ArrayList<>();



    public SearchParamsBuilder() {
        this(SearchParams.defaults());
    }

    public SearchParamsBuilder(SearchParams defaults) {
        this.earliestDepartureTime = defaults.earliestDepartureTime();
        this.latestArrivalTime = defaults.latestArrivalTime();
        this.searchWindowInSeconds = defaults.searchWindowInSeconds();
        this.boardSlackInSeconds = defaults.boardSlackInSeconds();
        this.numberOfAdditionalTransfers = defaults.numberOfAdditionalTransfers();
        this.relaxCostAtDestination = defaults.relaxCostAtDestination();
        this.timetableEnabled = defaults.timetableEnabled();
        this.waitAtBeginningEnabled = defaults.waitAtBeginningEnabled();
        this.stopFilter = defaults.stopFilter();
        this.accessLegs.addAll(defaults.accessLegs());
        this.egressLegs.addAll(defaults.egressLegs());

    }

    public int earliestDepartureTime() {
        return earliestDepartureTime;
    }

    public SearchParamsBuilder earliestDepartureTime(int earliestDepartureTime) {
        this.earliestDepartureTime = earliestDepartureTime;
        return this;
    }

    public int latestArrivalTime() {
        return latestArrivalTime;
    }

    public SearchParamsBuilder latestArrivalTime(int latestArrivalTime) {
        this.latestArrivalTime = latestArrivalTime;
        return this;
    }

    public int searchWindowInSeconds() {
        return searchWindowInSeconds;
    }

    public SearchParamsBuilder searchOneIterationOnly() {
        return searchWindowInSeconds(0);
    }

    public SearchParamsBuilder searchWindowInSeconds(int searchWindowInSeconds) {
        this.searchWindowInSeconds = searchWindowInSeconds;
        return this;
    }

    public int boardSlackInSeconds() {
        return boardSlackInSeconds;
    }

    public SearchParamsBuilder boardSlackInSeconds(int boardSlackInSeconds) {
        this.boardSlackInSeconds = boardSlackInSeconds;
        return this;
    }


    public int numberOfAdditionalTransfers() {
        return numberOfAdditionalTransfers;
    }

    public SearchParamsBuilder numberOfAdditionalTransfers(int numberOfAdditionalTransfers) {
        this.numberOfAdditionalTransfers = numberOfAdditionalTransfers;
        return this;
    }

    public double relaxCostAtDestination() {
        return relaxCostAtDestination;
    }

    public SearchParamsBuilder relaxCostAtDestination(double relaxCostAtDestination) {
        this.relaxCostAtDestination = relaxCostAtDestination;
        return this;
    }

    public boolean timetableEnabled() {
        return timetableEnabled;
    }

    public SearchParamsBuilder timetableEnabled(boolean enable) {
        this.timetableEnabled = enable;
        return this;
    }

    public boolean waitAtBeginningEnabled() {
        return waitAtBeginningEnabled;
    }

    public SearchParamsBuilder waitAtBeginningEnabled(boolean enable) {
        this.waitAtBeginningEnabled = enable;
        return this;
    }

    public BitSet stopFilter() {
        return stopFilter;
    }

    public SearchParamsBuilder stopFilter(BitSet stopFilter) {
        this.stopFilter = stopFilter;
        return this;
    }

    public Collection<TransferLeg> accessLegs() {
        return accessLegs;
    }

    public SearchParamsBuilder addAccessStop(TransferLeg accessLeg) {
        this.accessLegs.add(accessLeg);
        return this;
    }

    public SearchParamsBuilder addAccessStops(Iterable<TransferLeg> accessLegs) {
        for (TransferLeg it : accessLegs) {
            addAccessStop(it);
        }
        return this;
    }

    public Collection<TransferLeg> egressLegs() {
        return egressLegs;
    }

    public SearchParamsBuilder addEgressStop(TransferLeg egressLeg) {
        this.egressLegs.add(egressLeg);
        return this;
    }

    public SearchParamsBuilder addEgressStops(Iterable<TransferLeg> egressLegs) {
        for (TransferLeg it : egressLegs) {
            addEgressStop(it);
        }
        return this;
    }


    public SearchParams build() {
        return new SearchParams(this);
    }
}
