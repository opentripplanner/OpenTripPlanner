package org.opentripplanner.transit.raptor.api.request;

import com.esotericsoftware.minlog.Log;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitDataProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;


/**
 * All input parameters to RangeRaptor that is spesific to a routing request.
 * See {@link TransitDataProvider} for transit data.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorRequest<T extends RaptorTripSchedule> {
    private final SearchParams searchParams;
    private final RaptorProfile profile;
    private final boolean searchForward;
    private final Set<Optimization> optimizations;
    private final McCostParams mcCostParams;
    private final DebugRequest<T> debug;


    static <T extends RaptorTripSchedule> RaptorRequest<T> defaults() {
        return new RaptorRequest<>();
    }

    private RaptorRequest() {
        searchParams = SearchParams.defaults();
        profile = RaptorProfile.MULTI_CRITERIA;
        searchForward = true;
        optimizations = Collections.emptySet();
        mcCostParams = McCostParams.DEFAULTS;
        debug = DebugRequest.defaults();
    }

    RaptorRequest(RaptorRequestBuilder<T> builder) {
        this.searchParams = builder.searchParams().buildSearchParam();
        this.profile = builder.profile();
        this.searchForward = builder.searchForward();
        this.optimizations = Set.copyOf(builder.optimizations());
        this.mcCostParams = new McCostParams(builder.mcCostFactors());
        this.debug = builder.debug().build();
        verify();
    }

    public RaptorRequestBuilder<T> mutate() {
        return new RaptorRequestBuilder<T>(this);
    }

    /**
     * Requered travel search parameters.
     */
    public SearchParams searchParams() {
        return searchParams;
    }

    /**
     * A dynamic search is a search witch uses heuristics to resolve search parameters
     * as earliest-departure-time, latest-arrival-time and search-window. This is an aggregated
     * value:
     * <ul>
     *     <li>A multi-criteria search is a dynamic search.</li>
     *     <li>A standard range-raptor search with more than one iteration.</li>
     * </ul>
     * In principle any search could be run using dynamic resolving of EDT, LAT and search-window,
     * but for other "simpler" searches we would rather have it fail than magically run, if
     * configured wrong.
     */
    public boolean isDynamicSearch() {
        if(profile().is(RaptorProfile.MULTI_CRITERIA)) {
            return true;
        }
        if(profile.is(RaptorProfile.STANDARD)) {
            return !searchParams().searchOneIterationOnly();
        }
        return false;
    }

    /**
     * The profile/algorithm to use for this request.
     * <p/>
     * The default value is {@link RaptorProfile#MULTI_CRITERIA}
     */
    public RaptorProfile profile() {
        return profile;
    }

    /**
     * Set search direction to REVERSE to performed a search  from the destination
     * to the origin. Thie traverse the transit graph backwards in time.
     * This parameter is used internally to produce heuristics, and is normally not
     * something you would like to do unless you are testing or analyzing.
     * <p/>
     *
     * @return  NOT {@link #searchForward()}
     */
    public boolean searchInReverse() {
        return !searchForward;
    }

    /**
     * When TRUE the search is performed from the origin to the destination in a
     * normal way.
     * <p/>
     * Optional. Default value is 'true'.
     *
     * @return true is search is forward.
     */
    public boolean searchForward() {
        return searchForward;
    }

    /**
     * Return list of enabled optimizations.
     */
    public Collection<Optimization> optimizations() {
        return optimizations;
    }

    /**
     * TRUE if the given optimization is enabled.
     */
    public boolean optimizationEnabled(Optimization optimization) {
        return optimization.isOneOf(optimizations);
    }

    public boolean useTransfersStopFilter() {
        return optimizationEnabled(Optimization.TRANSFERS_STOP_FILTER);
    }

    public boolean useDestinationPruning() {
        return optimizationEnabled(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
    }

    public boolean runInParallel() {
        return optimizationEnabled(Optimization.PARALLEL);
    }

    /**
     * The multi-criteria cost criteria factors.
     */
    public McCostParams multiCriteriaCostFactors() {
        return mcCostParams;
    }

    /**
     * Specify what to debug in the debug request.
     * <p/>
     * This feature is optional, by default debugging is turned off.
     */
    public DebugRequest<T> debug() {
        return debug;
    }

    @Override
    public String toString() {
        return "RangeRaptorRequest{" +
                "searchParams=" + searchParams +
                ", profile=" + profile +
                ", searchForward=" + searchForward +
                ", optimizations=" + optimizations +
                ", mcCostParams=" + mcCostParams +
                ", debug=" + debug +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RaptorRequest<?> that = (RaptorRequest<?>) o;
        return profile == that.profile &&
                Objects.equals(searchParams, that.searchParams) &&
                Objects.equals(mcCostParams, that.mcCostParams) &&
                Objects.equals(debug, that.debug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchParams, profile, mcCostParams, debug);
    }



    static void assertProperty(boolean predicate, String message) {
        if(!predicate) {
            throw new IllegalArgumentException(message);
        }
    }

    /* private methods */

    private void verify() {
        searchParams.verify();
        if(!profile.is(RaptorProfile.MULTI_CRITERIA)) {
            if(useTransfersStopFilter()) {
                Log.warn("Stop filtering is only supported using McRangeRaptor");
            }
            if(useDestinationPruning()) {
                Log.warn("Destination pruning is only supported using McRangeRaptor");
            }
        }
    }
}
