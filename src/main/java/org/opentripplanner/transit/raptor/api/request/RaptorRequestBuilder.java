package org.opentripplanner.transit.raptor.api.request;

import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import javax.validation.constraints.NotNull;
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
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorRequestBuilder<T extends RaptorTripSchedule> {
    // Search
    private final SearchParamsBuilder<T> searchParams;
    private SearchDirection searchDirection;
    private RaptorSlackProvider slackProvider;

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
        this.searchParams = new SearchParamsBuilder<>(this, defaults.searchParams());
        this.searchDirection = defaults.searchDirection();
        this.slackProvider = defaults.slackProvider();

        // Algorithm
        this.profile = defaults.profile();
        this.mcCost = new McCostParamsBuilder(defaults.multiCriteriaCostFactors());
        this.optimizations.addAll(defaults.optimizations());

        // Debug
        this.debug = new DebugRequestBuilder<>(defaults.debug());
    }

    public SearchParamsBuilder<T> searchParams() {
        return searchParams;
    }

    public RaptorProfile profile() {
        return profile;
    }

    public RaptorRequestBuilder<T> profile(RaptorProfile profile) {
        this.profile = profile;
        return this;
    }

    public SearchDirection searchDirection() {
        return searchDirection;
    }

    public RaptorRequestBuilder<T> searchDirection(SearchDirection searchDirection) {
        this.searchDirection = searchDirection;
        return this;
    }

    public RaptorSlackProvider slackProvider() {
        return slackProvider;
    }

    public void slackProvider(@NotNull RaptorSlackProvider slackProvider) {
        this.slackProvider = slackProvider;
    }

    public Collection<Optimization> optimizations() {
        return optimizations;
    }

    public RaptorRequestBuilder<T> enableOptimization(Optimization optimization) {
        this.optimizations.add(optimization);
        return this;
    }

    public RaptorRequestBuilder<T> clearOptimizations() {
        this.optimizations.clear();
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
