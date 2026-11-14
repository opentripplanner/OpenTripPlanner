package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * This factory encapsulates the logic for deciding which direct street router to use.
 */
public class DirectStreetRouterFactory {

  /**
   * @return {@link DefaultDirectStreetRouter} if there are no via locations, otherwise
   * {@link ViaDirectStreetRouter}.
   */
  public static DirectStreetRouter create(RouteRequest request) {
    return request.isViaSearch() ? new ViaDirectStreetRouter() : new DefaultDirectStreetRouter();
  }
}
