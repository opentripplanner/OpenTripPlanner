package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.AdjustedCost;
import org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder;
import org.opentripplanner.routing.api.request.GroupBySimilarityParams;
import org.opentripplanner.routing.api.request.RoutingRequest;

import java.time.Instant;
import java.util.function.Consumer;

public class RoutingRequestToFilterChainMapper {
  private static final int KEEP_ONE = 1;

  /** Filter itineraries down to this limit, but not below. */
  private static final int MIN_NUMBER_OF_ITINERARIES = 3;

  /** Never return more that this limit of itineraries. */
  private static final int MAX_NUMBER_OF_ITINERARIES = 200;

  public static ItineraryFilter createFilterChain(
      RoutingRequest request,
      Instant filterOnLatestDepartureTime,
      Consumer<Itinerary> maxLimitReachedSubscriber
  ) {
    var builder = new ItineraryFilterChainBuilder(request.arriveBy);

    // Group by similar legs filter
    if(request.groupBySimilarity != null) {
      GroupBySimilarityParams p = request.groupBySimilarity;

      AdjustedCost adjustedCost = AdjustedCost.create(p.minSafeTransferTimeFactor);

      if (p.keepOne >= 0.5) {
        builder.addGroupBySimilarity(
            new GroupBySimilarity(p.keepOne, KEEP_ONE, adjustedCost)
        );
      }

      if (p.keepNumOfItineraries >= 0.5) {
        int minLimit = request.numItineraries;

        if (minLimit < 0 || minLimit > MIN_NUMBER_OF_ITINERARIES) {
          minLimit = MIN_NUMBER_OF_ITINERARIES;
        }

        builder.addGroupBySimilarity(
            new GroupBySimilarity(p.keepNumOfItineraries, minLimit, adjustedCost)
        );
      }
    }

    builder
        .withMaxNumberOfItineraries(Math.min(request.numItineraries, MAX_NUMBER_OF_ITINERARIES))
        .withTransitGeneralizedCostLimit(request.transitGeneralizedCostLimit)
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
        .withLatestDepartureTimeLimit(filterOnLatestDepartureTime)
        .withMaxLimitReachedSubscriber(maxLimitReachedSubscriber)
        .withDebugEnabled(request.debugItineraryFilter);

    return builder.build();
  }
}
