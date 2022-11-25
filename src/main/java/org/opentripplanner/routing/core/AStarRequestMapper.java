package org.opentripplanner.routing.core;

import java.time.Instant;
import org.opentripplanner.routing.api.request.RouteRequest;

public class AStarRequestMapper {

  public static AStarRequestBuilder map(RouteRequest opt) {
    return AStarRequest
      .of()
      .withStartTime(opt.dateTime())
      .withPreferences(opt.preferences())
      .withWheelchair(opt.wheelchair())
      .withParking(opt.journey().parking())
      .withRental(opt.journey().rental())
      .withFrom(opt.from())
      .withTo(opt.to());
  }

  public static AStarRequestBuilder mapToTransferRequest(RouteRequest opt) {
    return AStarRequest
      .of()
      .withStartTime(Instant.ofEpochSecond(0))
      .withPreferences(opt.preferences())
      .withWheelchair(opt.wheelchair())
      .withParking(opt.journey().parking())
      .withRental(opt.journey().rental())
      .withMode(opt.journey().transfer().mode());
  }
}
