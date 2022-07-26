package org.opentripplanner.standalone.config;

import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItineraryFiltersMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ItineraryFiltersMapper.class);

  public static ItineraryFilterParameters map(NodeAdapter c) {
    ItineraryFilterParameters dft = ItineraryFilterParameters.createDefault();

    if (c.isEmpty()) {
      return dft;
    }

    return new ItineraryFilterParameters(
      c.asBoolean("debug", dft.debug),
      c.asDouble("groupSimilarityKeepOne", dft.groupSimilarityKeepOne),
      c.asDouble("groupSimilarityKeepThree", dft.groupSimilarityKeepThree),
      c.asDouble(
        "groupedOtherThanSameLegsMaxCostMultiplier",
        dft.groupedOtherThanSameLegsMaxCostMultiplier
      ),
      parseTransitGeneralizedCostLimit(c, dft.transitGeneralizedCostLimit),
      c.asLinearFunction("nonTransitGeneralizedCostLimit", dft.nonTransitGeneralizedCostLimit),
      c.asDouble("bikeRentalDistanceRatio", dft.bikeRentalDistanceRatio),
      c.asDouble("parkAndRideDurationRatio", dft.parkAndRideDurationRatio),
      c.asBoolean(
        "filterItinerariesWithSameFirstOrLastTrip",
        dft.filterItinerariesWithSameFirstOrLastTrip
      ),
      c.asBoolean("accessibilityScore", dft.accessibilityScore)
    );
  }

  private static TransitGeneralizedCostFilterParams parseTransitGeneralizedCostLimit(
    NodeAdapter nodeAdapter,
    TransitGeneralizedCostFilterParams transitGeneralizedCostLimit
  ) {
    if (!nodeAdapter.exist("transitGeneralizedCostLimit")) {
      return transitGeneralizedCostLimit;
    }

    var params = nodeAdapter.path("transitGeneralizedCostLimit");

    if (params.isObject()) {
      return new TransitGeneralizedCostFilterParams(
        params.asLinearFunction(
          "costLimitFunction",
          transitGeneralizedCostLimit.costLimitFunction()
        ),
        params.asDouble("intervalRelaxFactor", transitGeneralizedCostLimit.intervalRelaxFactor())
      );
    }

    LOG.warn(
      "The format of transitGeneralizedCostLimit has changed, please see the documentation for new " +
      "configuration format. The existing format will cease to work after OTP v2.2"
    );

    return new TransitGeneralizedCostFilterParams(
      nodeAdapter.asLinearFunction("transitGeneralizedCostLimit", null),
      0
    );
  }
}
