package org.opentripplanner.ext.ridehailing;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.framework.Result;

/**
 * Utility method to shift the start of the journey to the earliest time that a vehicle can arrive.
 */
public class RideHailingDepartureTimeShifter {

  /**
   * When is a start time far enough in the future so that we don't need to check the service and
   * simply presume that a vehicle can arrive on time.
   */
  private static final Duration MAX_DURATION_FROM_NOW = Duration.ofMinutes(30);

  /**
   * When you start a car hailing search for right now (which is common) you cannot assume to leave
   * straight away but have to take into account the duration it takes for the hailing vehicle to
   * arrive.
   * <p>
   * This method shifts the departure time by the appropriate amount so that the correct
   * access/egresses can be calculated.
   */
  public static Result<RouteRequest, Error> shiftDepartureTime(
    RouteRequest req,
    List<RideHailingService> services,
    Instant now
  ) {
    if (shouldShift(req, now)) {
      return shiftTime(req, services, now);
    } else {
      return Result.success(req);
    }
  }

  public static boolean shouldShift(RouteRequest req, Instant now) {
    return (
      req.journey().modes().accessMode == StreetMode.CAR_HAILING &&
      req.dateTime().isBefore(now.plus(MAX_DURATION_FROM_NOW)) &&
      !req.arriveBy()
    );
  }

  private static Result<RouteRequest, Error> shiftTime(
    RouteRequest req,
    List<RideHailingService> services,
    Instant now
  ) {
    try {
      var service = services.get(0);
      var arrivalTimeOpt = service
        .arrivalTimes(new WgsCoordinate(req.from().getCoordinate()))
        .stream()
        .min(Comparator.comparing(ArrivalTime::duration));

      if (arrivalTimeOpt.isPresent()) {
        var earliestArrival = arrivalTimeOpt.get();
        return shift(req, now, earliestArrival);
      } else {
        return Result.failure(Error.NO_ARRIVAL_FOR_LOCATION);
      }
    } catch (ExecutionException e) {
      return Result.failure(Error.TECHNICAL_ERROR);
    }
  }

  @Nonnull
  private static Result<RouteRequest, Error> shift(
    RouteRequest req,
    Instant now,
    ArrivalTime earliestArrival
  ) {
    var earliestPickupTime = now.plus(earliestArrival.duration());
    if (earliestPickupTime.isAfter(req.dateTime())) {
      var clone = req.clone();
      clone.setDateTime(earliestPickupTime);
      return Result.success(clone);
    } else {
      return Result.success(req);
    }
  }

  enum Error {
    NO_ARRIVAL_FOR_LOCATION,
    TECHNICAL_ERROR,
  }
}
