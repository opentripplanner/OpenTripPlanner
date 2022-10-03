package org.opentripplanner.standalone.config;

import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.RequestFunctions;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItineraryFiltersMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ItineraryFiltersMapper.class);

  public static void mapItineraryFilterParams(
    NodeAdapter c,
    ItineraryFilterPreferences.Builder builder
  ) {
    if (c.isEmpty()) {
      return;
    }
    var dft = builder.original();

    builder
      .withDebug(c.asBoolean("debug", dft.debug()))
      .withGroupSimilarityKeepOne(
        c.asDouble("groupSimilarityKeepOne", dft.groupSimilarityKeepOne())
      )
      .withGroupSimilarityKeepThree(
        c.asDouble("groupSimilarityKeepThree", dft.groupSimilarityKeepThree())
      )
      .withGroupedOtherThanSameLegsMaxCostMultiplier(
        c.asDouble(
          "groupedOtherThanSameLegsMaxCostMultiplier",
          dft.groupedOtherThanSameLegsMaxCostMultiplier()
        )
      )
      .withTransitGeneralizedCostLimit(
        parseTransitGeneralizedCostLimit(
          c.path("transitGeneralizedCostLimit"),
          dft.transitGeneralizedCostLimit()
        )
      )
      .withNonTransitGeneralizedCostLimit(
        c.asLinearFunction("nonTransitGeneralizedCostLimit", dft.nonTransitGeneralizedCostLimit())
      )
      .withBikeRentalDistanceRatio(
        c.asDouble("bikeRentalDistanceRatio", dft.bikeRentalDistanceRatio())
      )
      .withParkAndRideDurationRatio(
        c.asDouble("parkAndRideDurationRatio", dft.parkAndRideDurationRatio())
      )
      .withFilterItinerariesWithSameFirstOrLastTrip(
        c.asBoolean(
          "filterItinerariesWithSameFirstOrLastTrip",
          dft.filterItinerariesWithSameFirstOrLastTrip()
        )
      )
      .withAccessibilityScore(c.asBoolean("accessibilityScore", dft.useAccessibilityScore()))
      .withRemoveItinerariesWithSameRoutesAndStops(
        c.asBoolean(
          "removeItinerariesWithSameRoutesAndStops",
          dft.removeItinerariesWithSameRoutesAndStops()
        )
      )
      .build();
  }

  private static TransitGeneralizedCostFilterParams parseTransitGeneralizedCostLimit(
    NodeAdapter node,
    TransitGeneralizedCostFilterParams transitGeneralizedCostLimit
  ) {
    if (node.isEmpty()) {
      return transitGeneralizedCostLimit;
    }

    if (node.isObject()) {
      return new TransitGeneralizedCostFilterParams(
        node.asLinearFunction("costLimitFunction", transitGeneralizedCostLimit.costLimitFunction()),
        node.asDouble("intervalRelaxFactor", transitGeneralizedCostLimit.intervalRelaxFactor())
      );
    }

    LOG.warn(
      "The format of transitGeneralizedCostLimit has changed, please see the documentation for new " +
      "configuration format. The existing format will cease to work after OTP v2.2"
    );

    return new TransitGeneralizedCostFilterParams(RequestFunctions.parse(node.asText()), 0);
  }
}
