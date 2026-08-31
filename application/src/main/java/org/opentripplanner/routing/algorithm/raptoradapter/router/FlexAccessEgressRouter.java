package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.util.Collection;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.FlexAccessEgressFinder;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Access/egress router for flex (on-demand/flexible) transportation.
 */
public class FlexAccessEgressRouter implements AccessEgressRouter {

  private final TransitService transitService;
  private final Graph graph;
  private final RegularTransferService transferService;
  private final StreetDetailsService streetDetailsService;
  private final FlexParameters flexParameters;
  private final AdditionalSearchDays additionalSearchDays;

  public FlexAccessEgressRouter(
    TransitService transitService,
    Graph graph,
    RegularTransferService transferService,
    StreetDetailsService streetDetailsService,
    FlexParameters flexParameters,
    AdditionalSearchDays additionalSearchDays
  ) {
    this.transitService = transitService;
    this.graph = graph;
    this.transferService = transferService;
    this.streetDetailsService = streetDetailsService;
    this.flexParameters = flexParameters;
    this.additionalSearchDays = additionalSearchDays;
  }

  @Override
  public Collection<? extends RoutingAccessEgress> route(AccessEgressRouterContext context) {
    var flexAccessList = FlexAccessEgressFinder.routeAccessEgress(
      context.accessRequest(),
      transitService,
      graph,
      transferService,
      streetDetailsService,
      additionalSearchDays,
      flexParameters,
      context.dataOverlayContext(),
      context.type(),
      context.linkingContext()
    );
    return AccessEgressMapper.mapFlexAccessEgresses(flexAccessList);
  }
}
