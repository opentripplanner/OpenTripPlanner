package org.opentripplanner.ext.transmodelapi.model.plan;

import gnu.trove.list.TIntList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.response.RoutingError;

public record ViaPlanResponse(
  List<List<Itinerary>> viaJourneys,
  List<List<List<Integer>>> viaJourneyConnections,
  List<RoutingError> routingErrors
) {}
