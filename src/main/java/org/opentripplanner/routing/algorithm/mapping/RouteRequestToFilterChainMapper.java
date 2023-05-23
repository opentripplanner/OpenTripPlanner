package org.opentripplanner.routing.algorithm.mapping;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.ext.ridehailing.RideHailingFilter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChainBuilder;
import org.opentripplanner.routing.algorithm.filterchain.ListSection;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

public class RouteRequestToFilterChainMapper {

  /** Filter itineraries down to this limit, but not below. */
  private static final int KEEP_THREE = 3;

  /** Never return more that this limit of itineraries. */
  private static final int MAX_NUMBER_OF_ITINERARIES = 200;

  public static ItineraryListFilterChain createFilterChain(
    RouteRequest request,
    OtpServerRequestContext context,
    Instant filterOnLatestDepartureTime,
    boolean removeWalkAllTheWayResults,
    Consumer<Itinerary> maxLimitReachedSubscriber
  ) {
    var builder = new ItineraryListFilterChainBuilder(request.itinerariesSortOrder());

    ItineraryFilterPreferences params = request.preferences().itineraryFilter();
    // Group by similar legs filter
    if (params.groupSimilarityKeepOne() >= 0.5) {
      builder.addGroupBySimilarity(
        GroupBySimilarity.createWithOneItineraryPerGroup(params.groupSimilarityKeepOne())
      );
    }

    if (params.groupSimilarityKeepThree() >= 0.5) {
      builder.addGroupBySimilarity(
        GroupBySimilarity.createWithMoreThanOneItineraryPerGroup(
          params.groupSimilarityKeepThree(),
          KEEP_THREE,
          true,
          params.groupedOtherThanSameLegsMaxCostMultiplier()
        )
      );
    }

    if (request.maxNumberOfItinerariesCropHead()) {
      builder.withMaxNumberOfItinerariesCrop(ListSection.HEAD);
    }

    builder
      .withMaxNumberOfItineraries(Math.min(request.numItineraries(), MAX_NUMBER_OF_ITINERARIES))
      .withTransitGeneralizedCostLimit(params.transitGeneralizedCostLimit())
      .withBikeRentalDistanceRatio(params.bikeRentalDistanceRatio())
      .withParkAndRideDurationRatio(params.parkAndRideDurationRatio())
      .withNonTransitGeneralizedCostLimit(params.nonTransitGeneralizedCostLimit())
      .withSameFirstOrLastTripFilter(params.filterItinerariesWithSameFirstOrLastTrip())
      .withAccessibilityScore(
        params.useAccessibilityScore() && request.wheelchair(),
        request.preferences().wheelchair().maxSlope()
      )
      .withFares(context.graph().getFareService())
      .withMinBikeParkingDistance(minBikeParkingDistance(request))
      .withRemoveTimeshiftedItinerariesWithSameRoutesAndStops(
        params.removeItinerariesWithSameRoutesAndStops()
      )
      .withTransitAlerts(
        context.transitService().getTransitAlertService(),
        context.transitService()::getMultiModalStationForStation
      )
      .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
      .withLatestDepartureTimeLimit(filterOnLatestDepartureTime)
      .withMaxLimitReachedSubscriber(maxLimitReachedSubscriber)
      .withRemoveWalkAllTheWayResults(removeWalkAllTheWayResults)
      .withDebugEnabled(params.debug());

    if (!context.rideHailingServices().isEmpty()) {
      builder.withRideHailingFilter(new RideHailingFilter(context.rideHailingServices()));
    }

    return builder.build();
  }

  private static double minBikeParkingDistance(RouteRequest request) {
    var modes = request.journey().modes();
    boolean hasBikePark = List
      .of(modes.accessMode, modes.egressMode)
      .contains(StreetMode.BIKE_TO_PARK);

    double minBikeParkingDistance = 0;
    if (hasBikePark) {
      minBikeParkingDistance = request.preferences().itineraryFilter().minBikeParkingDistance();
    }
    return minBikeParkingDistance;
  }
}
