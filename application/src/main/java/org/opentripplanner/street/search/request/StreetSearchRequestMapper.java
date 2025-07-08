package org.opentripplanner.street.search.request;

import java.time.Instant;
import org.opentripplanner.routing.api.request.RouteRequest;

public class StreetSearchRequestMapper {

  public static StreetSearchRequestBuilder map(RouteRequest request) {
    var time = request.dateTime() == null ? RouteRequest.normalizeNow() : request.dateTime();
    return StreetSearchRequest.of()
      .withStartTime(time)
      .withPreferences(request.preferences())
      .withWheelchair(request.journey().wheelchair())
      .withFrom(request.from())
      .withTo(request.to());
  }

  public static StreetSearchRequestBuilder mapToTransferRequest(RouteRequest request) {
    return StreetSearchRequest.of()
      .withStartTime(Instant.ofEpochSecond(0))
      .withPreferences(request.preferences())
      .withWheelchair(request.journey().wheelchair())
      .withMode(request.journey().transfer().mode());
  }
}
