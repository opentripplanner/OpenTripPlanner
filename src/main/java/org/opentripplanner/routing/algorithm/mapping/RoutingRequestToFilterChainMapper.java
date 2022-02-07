package org.opentripplanner.routing.algorithm.mapping;

import java.time.Instant;
import java.util.function.Consumer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChainBuilder;
import org.opentripplanner.routing.algorithm.filterchain.ListSection;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;

public class RoutingRequestToFilterChainMapper {
  /** Filter itineraries down to this limit, but not below. */
  private static final int KEEP_THREE = 3;

  /** Never return more that this limit of itineraries. */
  private static final int MAX_NUMBER_OF_ITINERARIES = 200;

  public static ItineraryListFilterChain createFilterChain(
      SortOrder sortOrder,
      ItineraryFilterParameters params,
      int maxNumOfItineraries,
      Instant filterOnLatestDepartureTime,
      boolean removeWalkAllTheWayResults,
      boolean maxNumberOfItinerariesCropHead,
      Consumer<Itinerary> maxLimitReachedSubscriber
  ) {
    var builder = new ItineraryListFilterChainBuilder(sortOrder);

    // Group by similar legs filter
    if (params.groupSimilarityKeepOne >= 0.5) {
      builder.addGroupBySimilarity(
          GroupBySimilarity.createWithOneItineraryPerGroup(params.groupSimilarityKeepOne)
      );
    }

    if (params.groupSimilarityKeepThree >= 0.5) {
      builder.addGroupBySimilarity(
        GroupBySimilarity.createWithMoreThanOneItineraryPerGroup(
            params.groupSimilarityKeepThree,
            KEEP_THREE,
            true,
            params.groupedOtherThanSameLegsMaxCostMultiplier
        )
      );
    }

    if(maxNumberOfItinerariesCropHead) {
      builder.withMaxNumberOfItinerariesCrop(ListSection.HEAD);
    }

    builder
        .withMaxNumberOfItineraries(Math.min(maxNumOfItineraries, MAX_NUMBER_OF_ITINERARIES))
        .withTransitGeneralizedCostLimit(params.transitGeneralizedCostLimit)
        .withBikeRentalDistanceRatio(params.bikeRentalDistanceRatio)
        .withParkAndRideDurationRatio(params.parkAndRideDurationRatio)
        .withNonTransitGeneralizedCostLimit(params.nonTransitGeneralizedCostLimit)
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
        .withLatestDepartureTimeLimit(filterOnLatestDepartureTime)
        .withMaxLimitReachedSubscriber(maxLimitReachedSubscriber)
        .withRemoveWalkAllTheWayResults(removeWalkAllTheWayResults)
        .withDebugEnabled(params.debug);

    return builder.build();
  }
}
