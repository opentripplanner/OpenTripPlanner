package org.opentripplanner.routing.core;

import org.opentripplanner.routing.api.request.RouteRequest;

public class AStarRequestMapper {

  public static AStarRequestBuilder map(RouteRequest opt) {
    return AStarRequest
      .of()
      .withStartTime(opt.dateTime())
      .withPreferences(opt.preferences())
      .withArriveBy(opt.arriveBy())
      .withWheelchair(opt.wheelchair())
      .withParking(opt.journey().parking())
      .withRental(opt.journey().rental())
      .withFrom(opt.from())
      .withTo(opt.to());
  }
}
