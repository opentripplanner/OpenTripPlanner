package org.opentripplanner.apis.transmodel.model.util;

import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.transit.model.site.StopLocation;

public record EncodedPolylineBeanWithStops(
  StopLocation fromQuay,
  StopLocation toQuay,
  EncodedPolyline pointsOnLink
) {}
