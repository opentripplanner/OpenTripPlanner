package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiEmissions;
import org.opentripplanner.model.plan.Emissions;

public class EmissionsMapper {

  public static ApiEmissions mapEmissions(Emissions emissions) {
    if (emissions == null) {
      return null;
    }
    return new ApiEmissions(emissions.getCo2Grams());
  }
}
