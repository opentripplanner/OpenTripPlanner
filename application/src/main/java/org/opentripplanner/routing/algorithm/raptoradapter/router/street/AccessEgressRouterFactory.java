package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import org.opentripplanner.graph_builder.module.nearbystops.StopResolver;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * This factory encapsulates the logic for deciding which access/egress router to use.
 */
public class AccessEgressRouterFactory {

  /**
   * @return {@link DefaultAccessEgressRouter} if there are no via locations, otherwise
   * {@link ViaAccessEgressRouter}.
   */
  public static AccessEgressRouter create(RouteRequest request, StopResolver stopResolver) {
    return request.isViaSearch()
      ? new ViaAccessEgressRouter(stopResolver)
      : new DefaultAccessEgressRouter(stopResolver);
  }
}
