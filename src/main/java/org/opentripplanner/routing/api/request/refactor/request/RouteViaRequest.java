package org.opentripplanner.routing.api.request.refactor.request;

import java.util.List;

//  User request: from/to time/location; preferences slack/cost/reluctance
public class RouteViaRequest extends RouteRequest {

  // TODO: 2022-08-18 documentation
  List<ViaLocation> viaPoints;

  // TODO: 2022-08-18 documentation
  // Number of trips must match viaPoints + 1 and is limited to max 5 trips
  List<JourneyRequest> viaJourneys;

  public RouteViaRequest() {
    // ONLY SUPPORT true
    super.timetableView = true;
  }

}
