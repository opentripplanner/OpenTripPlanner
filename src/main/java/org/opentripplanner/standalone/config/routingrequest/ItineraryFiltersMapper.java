package org.opentripplanner.standalone.config.routingrequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.RequestFunctions;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
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
      .withDebug(c.of("debug").since(NA).summary("TODO").asBoolean(dft.debug()))
      .withGroupSimilarityKeepOne(
        c
          .of("groupSimilarityKeepOne")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.groupSimilarityKeepOne())
      )
      .withGroupSimilarityKeepThree(
        c
          .of("groupSimilarityKeepThree")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.groupSimilarityKeepThree())
      )
      .withGroupedOtherThanSameLegsMaxCostMultiplier(
        c
          .of("groupedOtherThanSameLegsMaxCostMultiplier")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.groupedOtherThanSameLegsMaxCostMultiplier())
      )
      .withTransitGeneralizedCostLimit(
        parseTransitGeneralizedCostLimit(
          c
            .of("transitGeneralizedCostLimit")
            .since(NA)
            .summary("TODO")
            .description(/*TODO DOC*/"TODO")
            .asObject(),
          dft.transitGeneralizedCostLimit()
        )
      )
      .withNonTransitGeneralizedCostLimit(
        c
          .of("nonTransitGeneralizedCostLimit")
          .since(NA)
          .summary("TODO")
          .asLinearFunction(dft.nonTransitGeneralizedCostLimit())
      )
      .withBikeRentalDistanceRatio(
        c
          .of("bikeRentalDistanceRatio")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.bikeRentalDistanceRatio())
      )
      .withParkAndRideDurationRatio(
        c
          .of("parkAndRideDurationRatio")
          .since(NA)
          .summary("TODO")
          .asDouble(dft.parkAndRideDurationRatio())
      )
      .withFilterItinerariesWithSameFirstOrLastTrip(
        c
          .of("filterItinerariesWithSameFirstOrLastTrip")
          .since(NA)
          .summary("TODO")
          .asBoolean(dft.filterItinerariesWithSameFirstOrLastTrip())
      )
      .withAccessibilityScore(
        c.of("accessibilityScore").since(NA).summary("TODO").asBoolean(dft.useAccessibilityScore())
      )
      .withRemoveItinerariesWithSameRoutesAndStops(
        c
          .of("removeItinerariesWithSameRoutesAndStops")
          .since(NA)
          .summary("TODO")
          .asBoolean(dft.removeItinerariesWithSameRoutesAndStops())
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
        node
          .of("costLimitFunction")
          .since(NA)
          .summary("TODO")
          .asLinearFunction(transitGeneralizedCostLimit.costLimitFunction()),
        node
          .of("intervalRelaxFactor")
          .since(NA)
          .summary("TODO")
          .asDouble(transitGeneralizedCostLimit.intervalRelaxFactor())
      );
    }

    LOG.warn(
      "The format of transitGeneralizedCostLimit has changed, please see the documentation for new " +
      "configuration format. The existing format will cease to work after OTP v2.2"
    );

    return new TransitGeneralizedCostFilterParams(RequestFunctions.parse(node.asText()), 0);
  }
}
