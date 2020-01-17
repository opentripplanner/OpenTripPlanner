package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * This is a Request builder to help construct valid requests. Se the
 * request classes for documentation on each parameter.
 * <p/>
 * <ul>
 *     <li>{@link RaptorRequest}
 *     <li>{@link McCostParams}
 *     <li>{@link DebugRequest}
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public class RaptorRequestBuilder<T extends TripScheduleInfo> {
    // Search
    private final SearchParamsBuilder searchParams;
    private boolean searchForward;

    // Algorithm
    private RaptorProfile profile;
    private final McCostParamsBuilder mcCost;
    private final Set<Optimization> optimizations = EnumSet.noneOf(Optimization.class);


    // Debug
    private final DebugRequestBuilder<T> debug;

    public RaptorRequestBuilder() {
        this(RaptorRequest.defaults());
    }

    RaptorRequestBuilder(RaptorRequest<T> defaults) {
        this.searchParams = new SearchParamsBuilder(defaults.searchParams());
        this.searchForward = defaults.searchForward();

        // Algorithm
        this.profile = defaults.profile();
        this.mcCost = new McCostParamsBuilder(defaults.multiCriteriaCostFactors());
        this.optimizations.addAll(defaults.optimizations());

        // Debug
        this.debug = new DebugRequestBuilder<>(defaults.debug());
    }

    public SearchParamsBuilder searchParams() {
        return searchParams;
    }

    public RaptorProfile profile() {
        return profile;
    }

    public RaptorRequestBuilder<T> profile(RaptorProfile profile) {
        this.profile = profile;
        return this;
    }

    public boolean searchForward() {
        return searchForward;
    }

    public RaptorRequestBuilder<T> searchDirection(boolean searchForward) {
        this.searchForward = searchForward;
        return this;
    }

    public Collection<Optimization> optimizations() {
        return optimizations;
    }

    public RaptorRequestBuilder<T> enableOptimization(Optimization optimization) {
        this.optimizations.add(optimization);
        return this;
    }

    public RaptorRequestBuilder<T> disableOptimization(Optimization optimization) {
        this.optimizations.remove(optimization);
        return this;
    }

    public McCostParamsBuilder mcCostFactors() {
        return this.mcCost;
    }

    public DebugRequestBuilder<T> debug() {
        return this.debug;
    }

    public RaptorRequest<T> build() {
        return new RaptorRequest<>(this);
    }
}
