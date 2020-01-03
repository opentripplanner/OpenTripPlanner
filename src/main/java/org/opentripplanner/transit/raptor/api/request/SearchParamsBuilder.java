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
    private boolean arrivedBy;
    private int boardSlackInSeconds;
    private int numberOfAdditionalTransfers;
    private double relaxCostAtDestination;
    private boolean timetableEnabled;
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
        this.arrivedBy = defaults.arrivedBy();
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

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder earliestDepartureTime(int earliestDepartureTime) {
        this.earliestDepartureTime = earliestDepartureTime;
        return this;
    }

    public int latestArrivalTime() {
        return latestArrivalTime;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder latestArrivalTime(int latestArrivalTime) {
        this.latestArrivalTime = latestArrivalTime;
        return this;
    }

    public int searchWindowInSeconds() {
        return searchWindowInSeconds;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder searchOneIterationOnly() {
        return searchWindowInSeconds(0);
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder searchWindowInSeconds(int searchWindowInSeconds) {
        this.searchWindowInSeconds = searchWindowInSeconds;
        return this;
    }

    public boolean arrivedBy() {
        return arrivedBy;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder arrivedBy(boolean arrivedBy) {
        this.arrivedBy = arrivedBy;
        return this;
    }

    public int boardSlackInSeconds() {
        return boardSlackInSeconds;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder boardSlackInSeconds(int boardSlackInSeconds) {
        this.boardSlackInSeconds = boardSlackInSeconds;
        return this;
    }


    public int numberOfAdditionalTransfers() {
        return numberOfAdditionalTransfers;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder numberOfAdditionalTransfers(int numberOfAdditionalTransfers) {
        this.numberOfAdditionalTransfers = numberOfAdditionalTransfers;
        return this;
    }

    public double relaxCostAtDestination() {
        return relaxCostAtDestination;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder relaxCostAtDestination(double relaxCostAtDestination) {
        this.relaxCostAtDestination = relaxCostAtDestination;
        return this;
    }

    public boolean timetableEnabled() {
        return timetableEnabled;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder timetableEnabled(boolean enable) {
        this.timetableEnabled = enable;
        return this;
    }

    public boolean waitAtBeginningEnabled() {
        return waitAtBeginningEnabled;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder waitAtBeginningEnabled(boolean enable) {
        this.waitAtBeginningEnabled = enable;
        return this;
    }

    public BitSet stopFilter() {
        return stopFilter;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder stopFilter(BitSet stopFilter) {
        this.stopFilter = stopFilter;
        return this;
    }

    public Collection<TransferLeg> accessLegs() {
        return accessLegs;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder addAccessStop(TransferLeg accessLeg) {
        this.accessLegs.add(accessLeg);
        return this;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder addAccessStops(Iterable<TransferLeg> accessLegs) {
        for (TransferLeg it : accessLegs) {
            addAccessStop(it);
        }
        return this;
    }

    public Collection<TransferLeg> egressLegs() {
        return egressLegs;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder addEgressStop(TransferLeg egressLeg) {
        this.egressLegs.add(egressLeg);
        return this;
    }

    public org.opentripplanner.transit.raptor.api.request.SearchParamsBuilder addEgressStops(Iterable<TransferLeg> egressLegs) {
        for (TransferLeg it : egressLegs) {
            addEgressStop(it);
        }
        return this;
    }


    public SearchParams build() {
        return new SearchParams(this);
    }
}
