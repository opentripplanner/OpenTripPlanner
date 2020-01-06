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
 *     <li>{@link RangeRaptorRequest}
 *     <li>{@link McCostParams}
 *     <li>{@link DebugRequest}
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public class RequestBuilder<T extends TripScheduleInfo> {
    // Search
    private final SearchParamsBuilder searchParams;
    private boolean searchForward;

    // Algorithm
    private RangeRaptorProfile profile;
    private final McCostParamsBuilder mcCost;
    private final Set<Optimization> optimizations = EnumSet.noneOf(Optimization.class);


    // Debug
    private final DebugRequestBuilder<T> debug;

    public RequestBuilder() {
        this(RangeRaptorRequest.defaults());
    }

    RequestBuilder(RangeRaptorRequest<T> defaults) {
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

    public RangeRaptorProfile profile() {
        return profile;
    }

    public RequestBuilder<T> profile(RangeRaptorProfile profile) {
        this.profile = profile;
        return this;
    }

    public boolean searchForward() {
        return searchForward;
    }

    public RequestBuilder<T> searchDirection(boolean searchForward) {
        this.searchForward = searchForward;
        return this;
    }

    public Collection<Optimization> optimizations() {
        return optimizations;
    }

    public RequestBuilder<T> enableOptimization(Optimization optimization) {
        this.optimizations.add(optimization);
        return this;
    }

    public RequestBuilder<T> disableOptimization(Optimization optimization) {
        this.optimizations.remove(optimization);
        return this;
    }

    public McCostParamsBuilder mcCostFactors() {
        return this.mcCost;
    }

    public DebugRequestBuilder<T> debug() {
        return this.debug;
    }

    public RangeRaptorRequest<T> build() {
        return new RangeRaptorRequest<>(this);
    }
}
