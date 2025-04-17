package org.opentripplanner.routing.algorithm.mapping;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.ext.fares.DecorateWithFare;
import org.opentripplanner.ext.ridehailing.DecorateWithRideHailing;
import org.opentripplanner.ext.stopconsolidation.DecorateConsolidatedStopNames;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChainBuilder;
import org.opentripplanner.routing.algorithm.filterchain.api.GroupBySimilarity;
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
    Instant earliestDepartureTimeUsed,
    Duration searchWindowUsed,
    boolean removeWalkAllTheWayResults,
    Consumer<PageCursorInput> pageCursorInputSubscriber
  ) {
    var builder = new ItineraryListFilterChainBuilder(request.itinerariesSortOrder());

    // Skip filtering itineraries if generalized-cost is not computed
    if (!request.preferences().transit().raptor().profile().producesGeneralizedCost()) {
      return builder.build();
    }

    // The page cursor has deduplication information only in certain cases.
    if (request.pageCursor() != null && request.pageCursor().containsItineraryPageCut()) {
      builder = builder.withPagingDeduplicationFilter(request.pageCursor().itineraryPageCut());
    }

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

    builder
      .withMaxNumberOfItineraries(Math.min(request.numItineraries(), MAX_NUMBER_OF_ITINERARIES))
      .withMaxNumberOfItinerariesCropSection(request.cropItinerariesAt())
      .withTransitGeneralizedCostLimit(params.transitGeneralizedCostLimit())
      .withBikeRentalDistanceRatio(params.bikeRentalDistanceRatio())
      .withParkAndRideDurationRatio(params.parkAndRideDurationRatio())
      .withNonTransitGeneralizedCostLimit(params.nonTransitGeneralizedCostLimit())
      .withRemoveTransitWithHigherCostThanBestOnStreetOnly(
        params.removeTransitWithHigherCostThanBestOnStreetOnly()
      )
      .withSameFirstOrLastTripFilter(params.filterItinerariesWithSameFirstOrLastTrip())
      .withAccessibilityScore(
        params.useAccessibilityScore() && request.wheelchair(),
        request.preferences().wheelchair().maxSlope()
      )
      .withMinBikeParkingDistance(minBikeParkingDistance(request))
      .withRemoveTimeshiftedItinerariesWithSameRoutesAndStops(
        params.removeItinerariesWithSameRoutesAndStops()
      )
      .withTransitAlerts(
        context.transitService().getTransitAlertService(),
        context.transitService()::findMultiModalStation
      )
      .withSearchWindow(earliestDepartureTimeUsed, searchWindowUsed)
      .withPageCursorInputSubscriber(pageCursorInputSubscriber)
      .withRemoveWalkAllTheWayResults(removeWalkAllTheWayResults)
      .withRemoveTransitIfWalkingIsBetter(true)
      .withFilterDirectFlexBySearchWindow(params.filterDirectFlexBySearchWindow())
      .withDebugEnabled(params.debug());

    if (!request.preferences().transit().relaxTransitGroupPriority().isNormal()) {
      builder.withTransitGroupPriority();
    }

    var fareService = context.graph().getFareService();
    if (fareService != null) {
      builder.withFareDecorator(new DecorateWithFare(fareService));
    }

    if (!context.rideHailingServices().isEmpty()) {
      builder.withRideHailingDecoratingFilter(
        new DecorateWithRideHailing(context.rideHailingServices(), request.wheelchair())
      );
    }

    if (OTPFeature.Co2Emissions.isOn()) {
      builder.withEmissions(context.emissionsItineraryDecorator());
    }

    if (
      context.stopConsolidationService() != null && context.stopConsolidationService().isActive()
    ) {
      builder.withConsolidatedStopNamesDecorator(
        new DecorateConsolidatedStopNames(context.stopConsolidationService())
      );
    }

    return builder.build();
  }

  private static double minBikeParkingDistance(RouteRequest request) {
    var modes = request.journey().modes();
    boolean hasBikePark = List.of(modes.accessMode, modes.egressMode).contains(
      StreetMode.BIKE_TO_PARK
    );

    double minBikeParkingDistance = 0;
    if (hasBikePark) {
      minBikeParkingDistance = request.preferences().itineraryFilter().minBikeParkingDistance();
    }
    return minBikeParkingDistance;
  }
}
