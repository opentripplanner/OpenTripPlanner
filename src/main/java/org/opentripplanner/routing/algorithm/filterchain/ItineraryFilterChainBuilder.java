package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.AddMinSafeTransferCostFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.DebugFilterWrapper;
import org.opentripplanner.routing.algorithm.filterchain.filters.FilterChain;
import org.opentripplanner.routing.algorithm.filterchain.filters.GroupBySimilarLegsFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.LatestDepartureTimeFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.MaxLimitFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.NonTransitGeneralizedCostFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.OtpDefaultSortOrder;
import org.opentripplanner.routing.algorithm.filterchain.filters.RemoveBikerentalWithMostlyWalkingFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.RemoveParkAndRideWithMostlyWalkingFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.RemoveTransitIfStreetOnlyIsBetterFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.RemoveWalkOnlyFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.SortOnGeneralizedCost;
import org.opentripplanner.routing.algorithm.filterchain.filters.TransitGeneralizedCostFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;


/**
 * Create a filter chain based on the given config.
 */
public class ItineraryFilterChainBuilder {
    private static final int NOT_SET = -1;

    private final boolean arriveBy;
    private final List<GroupBySimilarity> groupBySimilarity = new ArrayList<>();

    private boolean debug = false;
    private int maxNumberOfItineraries = NOT_SET;
    private boolean removeTransitWithHigherCostThanBestOnStreetOnly = true;
    private double minSafeTransferTimeFactor;
    private boolean removeWalkAllTheWayResults;
    private DoubleFunction<Double> transitGeneralizedCostLimit;
    private double bikeRentalDistanceRatio;
    private double parkAndRideDurationRatio;
    private DoubleFunction<Double> nonTransitGeneralizedCostLimit;
    private Instant latestDepartureTimeLimit = null;
    private Consumer<Itinerary> maxLimitReachedSubscriber;


    /**
     * @param arriveBy Used to set the correct sort order. This si the same flag as the
     *        {@link org.opentripplanner.routing.api.request.RoutingRequest#arriveBy}.
     */
    public ItineraryFilterChainBuilder(boolean arriveBy) {
        this.arriveBy = arriveBy;
    }

    /**
     * The maximum number of itineraries returned. This will remove all itineraries at the
     * end of the list AFTER the final sort of the itineraries.
     * <p>
     * Use {@code -1} to disable.
     */
    public ItineraryFilterChainBuilder withMaxNumberOfItineraries(int value) {
        this.maxNumberOfItineraries = value;
        return this;
    }

    /**
     * If the transfer-time for an itinerary is less than the min-safe-transfer-time-limit, then
     * the difference is multiplied with this factor and added to the itinerary generalized-cost.
     * <p>
     * Default is off {@code 0.0}.
     */
    public ItineraryFilterChainBuilder withMinSafeTransferTimeFactor(double minSafeTransferTimeFactor) {
        this.minSafeTransferTimeFactor = minSafeTransferTimeFactor;
        return this;
    }

    /**
     * Group itineraries by the main legs and keeping approximately the given total number of
     * itineraries. The itineraries are grouped by the legs that account for more then 'p' % for the
     * total distance.
     *
     * @see GroupBySimilarity for more details.
     */
    public ItineraryFilterChainBuilder addGroupBySimilarity(GroupBySimilarity groupBySimilarity) {
        this.groupBySimilarity.add(groupBySimilarity);
        return this;
    }

    /**
     * This function is used to compute a max-limit for generalized-cost. The limit
     * is applied to itineraries with at least one transit leg. Street-only itineraries are not
     * considered.
     * <p>
     * The smallest transit leg generalized-cost value is used as input to the function.
     * For example if the function is {@code f(x) = 1800 + 2.0 x} and the smallest cost is
     * {@code 5000}, then all transit itineraries with a cost larger than
     * {@code 1800 + 2 * 5000 = 11 800} is dropped.
     */
    public ItineraryFilterChainBuilder withTransitGeneralizedCostLimit(DoubleFunction<Double> value){
        this.transitGeneralizedCostLimit = value;
        return this;
    }

    /**
     * This is a a bit similar to {@link #withTransitGeneralizedCostLimit(DoubleFunction)}, with
     * a few important differences.
     *
     * This function is used to compute a max-limit for generalized-cost. The limit
     * is applied to itineraries with no transit legs, however ALL itineraries (including those with
     * transit legs) are considered when calculating the minimum cost.
     * <p>
     * The smallest generalized-cost value is used as input to the function.
     * For example if the function is {@code f(x) = 1800 + 2.0 x} and the smallest cost is
     * {@code 5000}, then all non-transit itineraries with a cost larger than
     * {@code 1800 + 2 * 5000 = 11 800} is dropped.
     */
    public ItineraryFilterChainBuilder withNonTransitGeneralizedCostLimit(DoubleFunction<Double> value){
        this.nonTransitGeneralizedCostLimit = value;
        return this;
    }

    /**
     * This is used to filter out bike rental itineraries that contain mostly walking. The value
     * describes the ratio of the total itinerary that has to consist of bike rental to allow the
     * itinerary.
     */
    public ItineraryFilterChainBuilder withBikeRentalDistanceRatio(double value){
        this.bikeRentalDistanceRatio = value;
        return this;
    }

    /**
     * This is used to filter out park and ride itineraries that contain only driving and a
     * very long walking leg.
     * The value describes the amount of driving vs. walking to allow the itinerary.
     */
    public ItineraryFilterChainBuilder withParkAndRideDurationRatio(double value){
        this.parkAndRideDurationRatio = value;
        return this;
    }

    /**
     * The direct street search(walk, bicycle, car) is not pruning the transit search, so in some
     * cases we get "silly" transit itineraries that is marginally better on travel-duration
     * compared with a on-street-all-the-way itinerary. Use this method to turn this filter
     * on/off.
     * <p>
     * The filter remove all itineraries with a generalized-cost that is higher than the best
     * on-street-all-the-way itinerary.
     * <p>
     * This filter only have an effect, if an on-street-all-the-way(WALK, BICYCLE, CAR) itinerary
     * exist.
     */
    public ItineraryFilterChainBuilder withRemoveTransitWithHigherCostThanBestOnStreetOnly(boolean value) {
        this.removeTransitWithHigherCostThanBestOnStreetOnly = value;
        return this;
    }

    /**
     * This will NOT delete itineraries, but tag them as deleted using the
     * {@link Itinerary#systemNotices}.
     */
    public ItineraryFilterChainBuilder withDebugEnabled(boolean value) {
        this.debug = value;
        return this;
    }

    /**
     * Max departure time. This is a absolute filter on the itinerary departure time from the
     * origin.
     */
    public ItineraryFilterChainBuilder withLatestDepartureTimeLimit(Instant latestDepartureTimeLimit) {
        this.latestDepartureTimeLimit = latestDepartureTimeLimit;
        return this;
    }

    /**
     * If the maximum number of itineraries is exceeded, then the excess itineraries are removed.
     * To get notified about this a subscriber can be added. The first itinerary removed by the
     * {@code maxLimit} is retuned. The 'maxLimit' check is last thing happening in the
     * filter-chain after the final sort. So, if another filter remove an itinerary, the
     * itinerary is not considered with the respect to this feature.
     *
     * @param maxLimitReachedSubscriber the subscriber to notify in case any elements are removed.
     *                                  Only the first element removed is passed to the subscriber.
     */
    public ItineraryFilterChainBuilder withMaxLimitReachedSubscriber(Consumer<Itinerary> maxLimitReachedSubscriber) {
        this.maxLimitReachedSubscriber = maxLimitReachedSubscriber;
        return this;
    }

    /**
     * If set, walk-all-the-way itineraries are removed. This happens AFTER e.g. the group-by
     * and remove-transit-with-higher-cost-than-best-on-street-only filters. This make sure that
     * poor transit itineraries are filtered away before the walk-all-the-way itinerary is removed.
     */
    public ItineraryFilterChainBuilder withRemoveWalkAllTheWayResults(boolean enable) {
        this.removeWalkAllTheWayResults = enable;
        return this;
    }

    public ItineraryFilter build() {
        List<ItineraryFilter> filters = new ArrayList<>();

        if(minSafeTransferTimeFactor > 0.01) {
            filters.add(new AddMinSafeTransferCostFilter(minSafeTransferTimeFactor));
        }

        // Sort list on {@code groupByP} in ascending order to keep as many of the elements in the
        // groups where the grouping parameter is relaxed as possible.
        {
            List<GroupBySimilarity> groupBy = groupBySimilarity
                .stream()
                .sorted(Comparator.comparingDouble(o -> o.groupByP))
                .collect(Collectors.toList());

            for (GroupBySimilarity it : groupBy) {
                filters.add(
                    new GroupBySimilarLegsFilter(
                        it.groupByP,
                        it.approximateMinLimit,
                        new SortOnGeneralizedCost()
                    )
                );
            }
        }

        // Filter transit itineraries on generalized-cost
        if(transitGeneralizedCostLimit != null) {
            filters.add(new TransitGeneralizedCostFilter(transitGeneralizedCostLimit));
        }

        // Filter non-transit itineraries on generalized-cost
        if(nonTransitGeneralizedCostLimit != null) {
            filters.add(new NonTransitGeneralizedCostFilter(nonTransitGeneralizedCostLimit));
        }

        // Apply all absolute filters AFTER the groupBy filters. Absolute filters are filters that
        // remove elements/ based on the given itinerary properties - not considering other
        // itineraries. This may remove itineraries in the "groupBy" filters that are considered
        // worse than the itineraries removed here. Let take an example, 2 itineraries, A and B, are
        // returned. A have a significant higher cost than B, but share the same long last transit
        // leg. B depart AFTER the latest-departure-time (this may happen if the access is
        // time-shifted). Then, A will be removed by the "group-by" filters(similar to B, but cost
        // is worse). B is removed by the {@link LatestDepartureTimeFilter} below. This is exactly
        // what we want, since both itineraries are none optimal.
        {
            if (removeTransitWithHigherCostThanBestOnStreetOnly) {
                filters.add(new RemoveTransitIfStreetOnlyIsBetterFilter());
            }

            if(removeWalkAllTheWayResults) {
                filters.add(new RemoveWalkOnlyFilter());
            }

            if (latestDepartureTimeLimit != null) {
                filters.add(new LatestDepartureTimeFilter(latestDepartureTimeLimit));
            }

            if (bikeRentalDistanceRatio > 0) {
                filters.add(new RemoveBikerentalWithMostlyWalkingFilter(bikeRentalDistanceRatio));
            }

            if (parkAndRideDurationRatio > 0) {
                filters.add(new RemoveParkAndRideWithMostlyWalkingFilter(parkAndRideDurationRatio));
            }
        }

        // Remove itineraries if max limit is set
        if (maxNumberOfItineraries > 0) {
            filters.add(new OtpDefaultSortOrder(arriveBy));
            filters.add(
                new MaxLimitFilter(
                    "number-of-itineraries-filter",
                    maxNumberOfItineraries,
                    maxLimitReachedSubscriber
                )
            );
        }

        // Do the final itineraries sort
        filters.add(new OtpDefaultSortOrder(arriveBy));
        
        if(debug) {
            filters = addDebugWrappers(filters);
        }

        return new FilterChain(filters);
    }


    /* private methods */

    private List<ItineraryFilter> addDebugWrappers(List<ItineraryFilter> filters) {
        final DebugFilterWrapper.Factory factory = new DebugFilterWrapper.Factory();
        return filters.stream().map(factory::wrap).collect(Collectors.toList());
    }
}
