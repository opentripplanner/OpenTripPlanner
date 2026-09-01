package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;

/**
 * The per-call inputs shared by every {@link AccessEgressRouter} for a single
 * {@link AccessEgressFetcher#fetchAccess()}/{@link AccessEgressFetcher#fetchEgress()} call.
 *
 * @param routeRequest        the request used for this access/egress search
 * @param streetRequest       the original, unmodified access or egress {@link StreetRequest}
 * @param type                whether this is an access or an egress search
 * @param dataOverlayContext  extension request contexts resolved from the data-overlay config
 * @param linkingContext      contains the temporary vertices for the request locations
 */
record AccessEgressRouterContext(
  RouteRequest routeRequest,
  StreetRequest streetRequest,
  AccessEgressType type,
  Collection<ExtensionRequestContext> dataOverlayContext,
  LinkingContext linkingContext
) {}
