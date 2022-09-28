package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
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
      c.of("debug").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.debug),
      c.asDouble("groupSimilarityKeepOne", dft.groupSimilarityKeepOne),
      c.asDouble("groupSimilarityKeepThree", dft.groupSimilarityKeepThree),
      c.asDouble(
        "groupedOtherThanSameLegsMaxCostMultiplier",
        dft.groupedOtherThanSameLegsMaxCostMultiplier
      ),
      parseTransitGeneralizedCostLimit(
        c.path("transitGeneralizedCostLimit"),
        dft.transitGeneralizedCostLimit
      ),
      c.asLinearFunction("nonTransitGeneralizedCostLimit", dft.nonTransitGeneralizedCostLimit),
      c.asDouble("bikeRentalDistanceRatio", dft.bikeRentalDistanceRatio),
      c.asDouble("parkAndRideDurationRatio", dft.parkAndRideDurationRatio),
      c
        .of("filterItinerariesWithSameFirstOrLastTrip")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asBoolean(dft.filterItinerariesWithSameFirstOrLastTrip),
      c.of("accessibilityScore").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(dft.accessibilityScore),
      c
        .of("removeItinerariesWithSameRoutesAndStops")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asBoolean(dft.removeItinerariesWithSameRoutesAndStops)
    );
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
