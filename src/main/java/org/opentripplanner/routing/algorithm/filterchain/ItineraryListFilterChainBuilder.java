package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.comparator.OtpDefaultSortOrder;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOnGeneralizedCost;
import org.opentripplanner.routing.algorithm.filterchain.filter.AddMinSafeTransferCostFilter;
import org.opentripplanner.routing.algorithm.filterchain.filter.DeletionFlaggingFilter;
import org.opentripplanner.routing.algorithm.filterchain.filter.GroupByFilter;
import org.opentripplanner.routing.algorithm.filterchain.filter.SortingFilter;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByAllSameStations;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByTripIdAndDistance;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.LatestDepartureTimeFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.MaxLimitFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.NonTransitGeneralizedCostFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.OtherThanSameLegsMaxGeneralizedCostFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveBikerentalWithMostlyWalkingFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveParkAndRideWithMostlyWalkingFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveTransitIfStreetOnlyIsBetterFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.RemoveWalkOnlyFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.TransitGeneralizedCostFilter;

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
public class ItineraryListFilterChainBuilder {
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
    public ItineraryListFilterChainBuilder(boolean arriveBy) {
        this.arriveBy = arriveBy;
    }

    /**
     * The maximum number of itineraries returned. This will remove all itineraries at the
     * end of the list AFTER the final sort of the itineraries.
     * <p>
     * Use {@code -1} to disable.
     */
    public ItineraryListFilterChainBuilder withMaxNumberOfItineraries(int value) {
        this.maxNumberOfItineraries = value;
        return this;
    }

    /**
     * If the transfer-time for an itinerary is less than the min-safe-transfer-time-limit, then
     * the difference is multiplied with this factor and added to the itinerary generalized-cost.
     * <p>
     * Default is off {@code 0.0}.
     */
    public ItineraryListFilterChainBuilder withMinSafeTransferTimeFactor(double minSafeTransferTimeFactor) {
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
    public ItineraryListFilterChainBuilder addGroupBySimilarity(GroupBySimilarity groupBySimilarity) {
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
    public ItineraryListFilterChainBuilder withTransitGeneralizedCostLimit(DoubleFunction<Double> value){
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
    public ItineraryListFilterChainBuilder withNonTransitGeneralizedCostLimit(DoubleFunction<Double> value){
        this.nonTransitGeneralizedCostLimit = value;
        return this;
    }

    /**
     * This is used to filter out bike rental itineraries that contain mostly walking. The value
     * describes the ratio of the total itinerary that has to consist of bike rental to allow the
     * itinerary.
     */
    public ItineraryListFilterChainBuilder withBikeRentalDistanceRatio(double value){
        this.bikeRentalDistanceRatio = value;
        return this;
    }

    /**
     * This is used to filter out park and ride itineraries that contain only driving and a
     * very long walking leg.
     * The value describes the amount of driving vs. walking to allow the itinerary.
     */
    public ItineraryListFilterChainBuilder withParkAndRideDurationRatio(double value){
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
    public ItineraryListFilterChainBuilder withRemoveTransitWithHigherCostThanBestOnStreetOnly(boolean value) {
        this.removeTransitWithHigherCostThanBestOnStreetOnly = value;
        return this;
    }

    /**
     * This will NOT delete itineraries, but tag them as deleted using the
     * {@link Itinerary#systemNotices}.
     */
    public ItineraryListFilterChainBuilder withDebugEnabled(boolean value) {
        this.debug = value;
        return this;
    }

    /**
     * Max departure time. This is a absolute filter on the itinerary departure time from the
     * origin.
     */
    public ItineraryListFilterChainBuilder withLatestDepartureTimeLimit(Instant latestDepartureTimeLimit) {
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
    public ItineraryListFilterChainBuilder withMaxLimitReachedSubscriber(Consumer<Itinerary> maxLimitReachedSubscriber) {
        this.maxLimitReachedSubscriber = maxLimitReachedSubscriber;
        return this;
    }

    /**
     * If set, walk-all-the-way itineraries are removed. This happens AFTER e.g. the group-by
     * and remove-transit-with-higher-cost-than-best-on-street-only filter. This make sure that
     * poor transit itineraries are filtered away before the walk-all-the-way itinerary is removed.
     */
    public ItineraryListFilterChainBuilder withRemoveWalkAllTheWayResults(boolean enable) {
        this.removeWalkAllTheWayResults = enable;
        return this;
    }

    public ItineraryListFilterChain build() {
        List<ItineraryListFilter> filters = new ArrayList<>();

        if(minSafeTransferTimeFactor > 0.01) {
            filters.add(new AddMinSafeTransferCostFilter(minSafeTransferTimeFactor));
        }

        filters.addAll(buildGroupByTripIdAndDistanceFilters());

        // Filter transit itineraries on generalized-cost
        if(transitGeneralizedCostLimit != null) {
            filters.add(new DeletionFlaggingFilter(new TransitGeneralizedCostFilter(transitGeneralizedCostLimit)));
        }

        // Filter non-transit itineraries on generalized-cost
        if(nonTransitGeneralizedCostLimit != null) {
            filters.add(new DeletionFlaggingFilter(new NonTransitGeneralizedCostFilter(nonTransitGeneralizedCostLimit)));
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
                filters.add(new DeletionFlaggingFilter(new RemoveTransitIfStreetOnlyIsBetterFilter()));
            }

            if(removeWalkAllTheWayResults) {
                filters.add(new DeletionFlaggingFilter(new RemoveWalkOnlyFilter()));
            }

            if (latestDepartureTimeLimit != null) {
                filters.add(new DeletionFlaggingFilter(new LatestDepartureTimeFilter(latestDepartureTimeLimit)));
            }

            if (bikeRentalDistanceRatio > 0) {
                filters.add(new DeletionFlaggingFilter(new RemoveBikerentalWithMostlyWalkingFilter(bikeRentalDistanceRatio)));
            }

            if (parkAndRideDurationRatio > 0) {
                filters.add(new DeletionFlaggingFilter(new RemoveParkAndRideWithMostlyWalkingFilter(parkAndRideDurationRatio)));
            }
        }

        // Remove itineraries if max limit is set
        if (maxNumberOfItineraries > 0) {
            filters.add(new SortingFilter(new OtpDefaultSortOrder(arriveBy)));
            filters.add(
                new DeletionFlaggingFilter(
                    new MaxLimitFilter(
                        "number-of-itineraries-filter",
                        maxNumberOfItineraries,
                        maxLimitReachedSubscriber
                    )
                )
            );
        }

        // Do the final itineraries sort
        filters.add(new SortingFilter(new OtpDefaultSortOrder(arriveBy)));

        return new ItineraryListFilterChain(filters, debug);
    }

    /**
     * These filters will group the itineraries by the main-legs and reduce the number of
     * itineraries in each group. The main legs is the legs that together constitute more than a
     * given　percentage of the total travel distance.
     * <p>
     * Each group is filtered using generalized-cost, keeping only the itineraries with the lowest
     * cost. If there is a tie, the filter look at the number-of-transfers as a tie breaker.
     * <p>
     * The filter name is dynamically created: similar-legs-filter-68p-1
     */
    private List<ItineraryListFilter> buildGroupByTripIdAndDistanceFilters() {
        List<GroupBySimilarity> groupBy = groupBySimilarity
            .stream()
            .sorted(Comparator.comparingDouble(o -> o.groupByP))
            .collect(Collectors.toList());

        List<ItineraryListFilter> groupByFilters = new ArrayList<>();

        for (GroupBySimilarity it : groupBy) {
            String name = "similar-legs-filter-" +
                    (int)(100d * it.groupByP) + "p-" + it.maxNumOfItinerariesPerGroup + "x";

            List<ItineraryListFilter> nested = new ArrayList<>();

            if (it.nestedGroupingByAllSameStations) {
                final String innerGroupName = name + "-group-by-all-same-stations";
                nested.add(new GroupByFilter<>(
                    GroupByAllSameStations::new,
                    List.of(
                        new SortingFilter(new SortOnGeneralizedCost()),
                        new DeletionFlaggingFilter( new MaxLimitFilter(innerGroupName, 1))
                    )
                ));
            }

            if (it.maxCostOtherLegsFactor > 1.0) {
                nested.add(new DeletionFlaggingFilter(new OtherThanSameLegsMaxGeneralizedCostFilter(
                    it.maxCostOtherLegsFactor
                )));
            }

            nested.add(new SortingFilter(new SortOnGeneralizedCost()));
            nested.add(new DeletionFlaggingFilter(new MaxLimitFilter(
                name,
                it.maxNumOfItinerariesPerGroup
            )));

            groupByFilters.add(
                new GroupByFilter<>(
                    itinerary -> new GroupByTripIdAndDistance(itinerary, it.groupByP),
                    nested
                )
            );
        }

        return groupByFilters;
    }
}
