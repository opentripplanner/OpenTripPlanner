package org.opentripplanner.routing.api.request.refactor.preference;

import java.util.List;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.opentripplanner.routing.api.request.RoutingTag;

public class SystemPreferences {
  ItineraryFilterParameters itineraryFilters = ItineraryFilterParameters.createDefault();
  // TODO: 2022-08-17 is this right?
  List<RoutingTag> tags = List.of();
  DataOverlayParameters dataOverlay;
  boolean geoidElevation=false;

  // The REST API should hold onto this, and pass it to the mapper, no need to include it in the
  // request.
  // boolean showIntermediateStops=false;
}
