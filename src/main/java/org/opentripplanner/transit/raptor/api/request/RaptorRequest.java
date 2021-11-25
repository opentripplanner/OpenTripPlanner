package org.opentripplanner.transit.raptor.api.request;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * All input parameters to RangeRaptor that is specific to a routing request.
 * See {@link RaptorTransitDataProvider} for transit data.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorRequest<T extends RaptorTripSchedule> {
    private static final Logger LOG = LoggerFactory.getLogger(RaptorRequest.class);

    private final SearchParams searchParams;
    private final RaptorProfile profile;
    private final SearchDirection searchDirection;
    private final Set<Optimization> optimizations;
    private final DebugRequest debug;
    private final RaptorSlackProvider slackProvider;


    static <T extends RaptorTripSchedule> RaptorRequest<T> defaults() {
        return new RaptorRequest<>();
    }

    private RaptorRequest() {
        searchParams = SearchParams.defaults();
        profile = RaptorProfile.MULTI_CRITERIA;
        searchDirection = SearchDirection.FORWARD;
        optimizations = Collections.emptySet();
        // Slack defaults: 1 minute for transfer-slack, 0 minutes for board- and alight-slack.
        slackProvider = RaptorSlackProvider.defaultSlackProvider(60, 0, 0);
        debug = DebugRequest.defaults();
    }

    RaptorRequest(RaptorRequestBuilder<T> builder) {
        this.searchParams = builder.searchParams().buildSearchParam();
        this.profile = builder.profile();
        this.searchDirection = builder.searchDirection();
        this.optimizations = Set.copyOf(builder.optimizations());
        this.slackProvider = builder.slackProvider();
        this.debug = builder.debug().build();
        verify();
    }

    public RaptorRequestBuilder<T> mutate() {
        return new RaptorRequestBuilder<>(this);
    }

    /**
     * Required travel search parameters.
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

    public SearchDirection searchDirection() {
        return searchDirection;
    }

    public RaptorSlackProvider slackProvider() {
        return slackProvider;
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

    public boolean useDestinationPruning() {
        return optimizationEnabled(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
    }

    public boolean runInParallel() {
        return optimizationEnabled(Optimization.PARALLEL);
    }

    /**
     * Specify what to debug in the debug request.
     * <p/>
     * This feature is optional, by default debugging is turned off.
     */
    public DebugRequest debug() {
        return debug;
    }

    @Override
    public String toString() {
        return "RaptorRequest{" +
                "profile=" + profile +
                ", searchForward=" + searchDirection +
                ", optimizations=" + optimizations +
                ", debug=" + debug +
                ", searchParams=" + searchParams +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        RaptorRequest<?> that = (RaptorRequest<?>) o;
        return profile == that.profile &&
                Objects.equals(searchParams, that.searchParams) &&
                Objects.equals(debug, that.debug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchParams, profile, debug);
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
            if(useDestinationPruning()) {
                LOG.warn("Destination pruning is only supported using McRangeRaptor");
            }
        }
    }
}
