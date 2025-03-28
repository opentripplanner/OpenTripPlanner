package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import org.opentripplanner.model.plan.ElevationProfile;

@Deprecated
class ElevationMapper {

  public static String mapElevation(ElevationProfile p) {
    if (p == null) {
      return null;
    }
    StringBuilder str = new StringBuilder();
    for (var step : p.steps()) {
      str.append(Math.round(step.x()));
      str.append(",");
      if (step.isYUnknown()) {
        str.append("NaN");
      } else {
        str.append(Math.round(step.y() * 10.0) / 10.0);
      }
      str.append(",");
    }
    if (str.length() > 0) {
      str.setLength(str.length() - 1);
    }
    return str.toString();
  }
}
