package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * {@code RouteRequest -> TransferProfileType} mapping, used at request time to pick which
 * profile's {@link org.opentripplanner.transit.transfer.regular.RaptorRegularTransferService}
 * should serve a live request. Only {@code WALK} exists today (see {@code TransferProfileType}),
 * so every request resolves to it; this is the single place to extend once more profiles exist.
 */
public final class RaptorTransferProfileMapper {

  private RaptorTransferProfileMapper() {}

  public static TransferProfileType fromRouteRequest(RouteRequest request) {
    return TransferProfileType.WALK;
  }
}
