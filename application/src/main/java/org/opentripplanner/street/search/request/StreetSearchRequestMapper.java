package org.opentripplanner.street.search.request;

import java.time.Instant;
import org.opentripplanner.routing.api.request.RouteRequest;

public class StreetSearchRequestMapper {

  public static StreetSearchRequestBuilder map(RouteRequest opt) {
    return StreetSearchRequest.of()
      .withStartTime(opt.dateTime())
      .withPreferences(opt.preferences())
      .withWheelchair(opt.wheelchair())
      .withFrom(opt.from())
      .withTo(opt.to());
  }

  public static StreetSearchRequestBuilder mapToTransferRequest(RouteRequest opt) {
    return StreetSearchRequest.of()
      .withStartTime(Instant.ofEpochSecond(0))
      .withPreferences(opt.preferences())
      .withWheelchair(opt.wheelchair())
      .withMode(opt.journey().transfer().mode());
  }
}
