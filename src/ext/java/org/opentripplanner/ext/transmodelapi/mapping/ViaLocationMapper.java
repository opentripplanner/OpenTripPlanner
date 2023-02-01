package org.opentripplanner.ext.transmodelapi.mapping;

import java.time.Duration;
import java.util.Map;
import org.opentripplanner.routing.api.request.ViaLocation;

class ViaLocationMapper {

  static ViaLocation mapViaLocation(Map<String, Object> viaLocation) {
    return new ViaLocation(
      GenericLocationMapper.toGenericLocation(viaLocation),
      false,
      (Duration) viaLocation.get("minSlack"),
      (Duration) viaLocation.get("maxSlack")
    );
  }
}
