package org.opentripplanner.apis.transmodel.mapping;

import java.util.Map;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.StreetMode;

class RequestModesMapper {

  /**
   * Maps a GraphQL Modes input type to a RequestModes.
   *
   * This only maps access, egress, direct & transfer.
   * Transport modes are now part of filters.
   * Only in case filters are not present we will use this mapping
   */
  @SuppressWarnings("unchecked")
  static RequestModes mapRequestModes(Map<String, ?> modesInput) {
    StreetMode accessMode = (StreetMode) modesInput.get("accessMode");
    RequestModesBuilder mBuilder = RequestModes
      .of()
      .withAccessMode(accessMode)
      .withEgressMode((StreetMode) modesInput.get("egressMode"))
      .withDirectMode((StreetMode) modesInput.get("directMode"));

    mBuilder.withTransferMode(accessMode == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK);

    return mBuilder.build();
  }
}
