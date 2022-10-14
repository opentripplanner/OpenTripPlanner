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
      .withDebug(c.of("debug").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.debug()))
      .withGroupSimilarityKeepOne(
        c
          .of("groupSimilarityKeepOne")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.groupSimilarityKeepOne())
      )
      .withGroupSimilarityKeepThree(
        c
          .of("groupSimilarityKeepThree")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.groupSimilarityKeepThree())
      )
      .withGroupedOtherThanSameLegsMaxCostMultiplier(
        c
          .of("groupedOtherThanSameLegsMaxCostMultiplier")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.groupedOtherThanSameLegsMaxCostMultiplier())
      )
      .withTransitGeneralizedCostLimit(
        parseTransitGeneralizedCostLimit(
          c
            .of("transitGeneralizedCostLimit")
            .withDoc(NA, /*TODO DOC*/"TODO")
            .withDescription(/*TODO DOC*/"TODO")
            .asObject(),
          dft.transitGeneralizedCostLimit()
        )
      )
      .withNonTransitGeneralizedCostLimit(
        c
          .of("nonTransitGeneralizedCostLimit")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asLinearFunction(dft.nonTransitGeneralizedCostLimit())
      )
      .withBikeRentalDistanceRatio(
        c
          .of("bikeRentalDistanceRatio")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.bikeRentalDistanceRatio())
      )
      .withParkAndRideDurationRatio(
        c
          .of("parkAndRideDurationRatio")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDouble(dft.parkAndRideDurationRatio())
      )
      .withFilterItinerariesWithSameFirstOrLastTrip(
        c
          .of("filterItinerariesWithSameFirstOrLastTrip")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(dft.filterItinerariesWithSameFirstOrLastTrip())
      )
      .withAccessibilityScore(
        c
          .of("accessibilityScore")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asBoolean(dft.useAccessibilityScore())
      )
      .withRemoveItinerariesWithSameRoutesAndStops(
        c
          .of("removeItinerariesWithSameRoutesAndStops")
          .withDoc(NA, /*TODO DOC*/"TODO")
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
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asLinearFunction(transitGeneralizedCostLimit.costLimitFunction()),
        node
          .of("intervalRelaxFactor")
          .withDoc(NA, /*TODO DOC*/"TODO")
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
