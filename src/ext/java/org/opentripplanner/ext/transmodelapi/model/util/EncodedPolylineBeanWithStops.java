package org.opentripplanner.ext.transmodelapi.model.util;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.util.model.EncodedPolyline;

public record EncodedPolylineBeanWithStops(
  StopLocation fromQuay,
  StopLocation toQuay,
  EncodedPolyline pointsOnLink
) {}
