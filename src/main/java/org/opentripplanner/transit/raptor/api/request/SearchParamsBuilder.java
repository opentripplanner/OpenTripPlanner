package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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
    private boolean preferLateArrival;
    private int numberOfAdditionalTransfers;
    private int maxNumberOfTransfers;
    private double relaxCostAtDestination;
    private boolean timetableEnabled;
    private final Collection<RaptorTransfer> accessPaths = new ArrayList<>();
    private final Collection<RaptorTransfer> egressPaths = new ArrayList<>();

    public SearchParamsBuilder(RaptorRequestBuilder<T> parent, SearchParams defaults) {
        this.parent = parent;
        this.earliestDepartureTime = defaults.earliestDepartureTime();
        this.latestArrivalTime = defaults.latestArrivalTime();
        this.searchWindowInSeconds = defaults.searchWindowInSeconds();
        this.preferLateArrival = defaults.preferLateArrival();
        this.numberOfAdditionalTransfers = defaults.numberOfAdditionalTransfers();
        this.maxNumberOfTransfers = defaults.maxNumberOfTransfers();
        this.relaxCostAtDestination = defaults.relaxCostAtDestination();
        this.timetableEnabled = defaults.timetableEnabled();
        this.accessPaths.addAll(defaults.accessPaths());
        this.egressPaths.addAll(defaults.egressPaths());
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

    public boolean preferLateArrival() {
        return preferLateArrival;
    }

    public SearchParamsBuilder<T> preferLateArrival(boolean enable) {
        this.preferLateArrival = enable;
        return this;
    }

    public int numberOfAdditionalTransfers() {
        return numberOfAdditionalTransfers;
    }

    public SearchParamsBuilder<T> numberOfAdditionalTransfers(int numberOfAdditionalTransfers) {
        this.numberOfAdditionalTransfers = numberOfAdditionalTransfers;
        return this;
    }

    public int maxNumberOfTransfers() {
        return maxNumberOfTransfers;
    }

    public SearchParamsBuilder<T> maxNumberOfTransfers(int maxNumberOfTransfers) {
        this.maxNumberOfTransfers = maxNumberOfTransfers;
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

    public Collection<RaptorTransfer> accessPaths() {
        return accessPaths;
    }

    public SearchParamsBuilder<T> addAccessPaths(Collection<? extends RaptorTransfer> accessPaths) {
        this.accessPaths.addAll(accessPaths);
        return this;
    }

    public SearchParamsBuilder<T> addAccessPaths(RaptorTransfer ... accessPaths) {
        return addAccessPaths(Arrays.asList(accessPaths));
    }

    public Collection<RaptorTransfer> egressPaths() {
        return egressPaths;
    }

    public SearchParamsBuilder<T> addEgressPaths(Collection<? extends RaptorTransfer> egressPaths) {
        this.egressPaths.addAll(egressPaths);
        return this;
    }

    public SearchParamsBuilder<T> addEgressPaths(RaptorTransfer ... egressPaths) {
        return addEgressPaths(Arrays.asList(egressPaths));
    }

    public RaptorRequest<T> build() {
        return parent.build();
    }

    /** This is public to allow tests to build just search params */
    public SearchParams buildSearchParam() {
        return new SearchParams(this);
    }

}
