package org.opentripplanner.apis.transmodel.mapping;

import static org.opentripplanner.routing.api.response.InputField.INTERMEDIATE_PLACE;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.routing.api.request.ViaLocationDeprecated;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;

@Deprecated
class ViaLocationDeprecatedMapper {

  private final GenericLocationMapper genericLocationMapper;

  ViaLocationDeprecatedMapper(FeedScopedIdMapper idMapper) {
    this.genericLocationMapper = new GenericLocationMapper(idMapper);
  }

  ViaLocationDeprecated mapViaLocation(Map<String, Object> viaLocation) {
    try {
      return new ViaLocationDeprecated(
        genericLocationMapper.toGenericLocation(viaLocation),
        false,
        (Duration) viaLocation.get("minSlack"),
        (Duration) viaLocation.get("maxSlack")
      );
    } catch (IllegalArgumentException e) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, INTERMEDIATE_PLACE))
      );
    }
  }
}
