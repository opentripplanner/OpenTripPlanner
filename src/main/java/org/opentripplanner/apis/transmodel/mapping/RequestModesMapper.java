package org.opentripplanner.apis.transmodel.mapping;

import java.util.Map;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.StreetMode;

class RequestModesMapper {

  /**
   * Maps GraphQL Modes input type to RequestModes.
   * <p>
   * This only maps access, egress, direct & transfer modes. Transport modes are set using filters.
   * Default modes are WALK for access, egress, direct & transfer.
   */
  static RequestModes mapRequestModes(Map<String, ?> modesInput) {
    RequestModesBuilder mBuilder = RequestModes.of();

    if (modesInput.containsKey("accessMode")) {
      StreetMode accessMode = (StreetMode) modesInput.get("accessMode");
      mBuilder.withAccessMode(accessMode);
      mBuilder.withTransferMode(accessMode == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK);
    }
    if (modesInput.containsKey("egressMode")) {
      mBuilder.withEgressMode((StreetMode) modesInput.get("egressMode"));
    }
    if (modesInput.containsKey("directMode")) {
      mBuilder.withDirectMode((StreetMode) modesInput.get("directMode"));
    }

    return mBuilder.build();
  }
}
