package org.opentripplanner.routing.api.request.request;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

// TODO VIA: Javadoc
public class RouteViaRequest {

  private final RoutingPreferences preferences;

  private final RouteRequest routeRequest;

  private final List<ViaLeg> viaLegs = new ArrayList<>();

  public RouteViaRequest(
    RoutingPreferences preferences,
    List<ViaLocation> viaLocations,
    List<JourneyRequest> viaJourneys,
    RouteRequest routeRequest
  ) {
    this.preferences = preferences;
    // Number of trips must match viaPoints + 1 and is limited to max 5 trips
    this.routeRequest = routeRequest;

    // TODO: Assert viaJourneys = viaLocations + 1

    // Last ViaLeg has no ViaLocation
    for (int i = 0; i < viaJourneys.size(); i++) {
      var viaLocation = i < viaJourneys.size() - 1 ? viaLocations.get(i) : null;
      viaLegs.add(new ViaLeg(viaJourneys.get(i), viaLocation));
    }
  }

  public RoutingPreferences preferences() {
    return this.preferences;
  }

  public RouteRequest routeRequest() {
    return this.routeRequest;
  }

  public List<ViaLeg> viaLegs() {
    return this.viaLegs;
  }

  public record ViaLeg(JourneyRequest journeyRequest, ViaLocation viaLocation) {}
}
