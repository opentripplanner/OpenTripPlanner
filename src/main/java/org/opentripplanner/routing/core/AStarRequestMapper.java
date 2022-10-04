package org.opentripplanner.routing.core;

import org.opentripplanner.routing.api.request.RouteRequest;

public class AStarRequestMapper {

  public static AStarRequestBuilder map(RouteRequest opt) {
    return AStarRequest
      .of()
      .setStartTime(opt.dateTime())
      .setPreferences(opt.preferences())
      .setArriveBy(opt.arriveBy())
      .setWheelchair(opt.wheelchair())
      .setParking(opt.journey().parking())
      .setRental(opt.journey().rental())
      .setFrom(opt.from())
      .setTo(opt.to());
  }
}
