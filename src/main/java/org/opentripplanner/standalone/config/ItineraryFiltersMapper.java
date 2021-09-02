package org.opentripplanner.standalone.config;

import org.opentripplanner.routing.api.request.ItineraryFilterParameters;

public class ItineraryFiltersMapper {

  public static ItineraryFilterParameters map(NodeAdapter c) {
    ItineraryFilterParameters dft = ItineraryFilterParameters.createDefault();

    if(c.isEmpty()) {
      return dft;
    }

    return new ItineraryFilterParameters(
        c.asBoolean("debug", dft.debug),
        c.asDouble("groupSimilarityKeepOne", dft.groupSimilarityKeepOne),
        c.asDouble("groupSimilarityKeepNumOfItineraries", dft.groupSimilarityKeepNumOfItineraries),
        c.asDouble("minSafeTransferTimeFactor", dft.minSafeTransferTimeFactor),
        c.asLinearFunction("transitGeneralizedCostLimit", dft.transitGeneralizedCostLimit),
        c.asLinearFunction("nonTransitGeneralizedCostLimit", dft.nonTransitGeneralizedCostLimit),
        c.asDouble("bikeRentalDistanceRatio", dft.bikeRentalDistanceRatio),
        c.asDouble("parkAndRideDurationRatio", dft.parkAndRideDurationRatio)
    );
  }
}
