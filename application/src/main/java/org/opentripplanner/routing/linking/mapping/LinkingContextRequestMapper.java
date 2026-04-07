package org.opentripplanner.routing.linking.mapping;

import org.opentripplanner.routing.algorithm.raptoradapter.router.FilterTransitWhenDirectModeIsEmpty;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextRequest;

public class LinkingContextRequestMapper {

  public static LinkingContextRequest map(RouteRequest request) {
    var emptyDirectModeHandler = new FilterTransitWhenDirectModeIsEmpty(
      request.journey().direct().mode(),
      request.pageCursor() != null
    );
    var directMode = emptyDirectModeHandler.resolveDirectMode();
    var builder = LinkingContextRequest.of()
      .withFrom(request.from())
      .withTo(request.to())
      .withViaLocationsWithCoordinates(request.listViaLocationsWithCoordinates())
      .withDirectMode(directMode);
    if (request.journey().transit().enabled()) {
      builder
        .withAccessMode(request.journey().access().mode())
        .withEgressMode(request.journey().egress().mode())
        .withTransferMode(request.journey().transfer().mode());
    }
    return builder.build();
  }
}
