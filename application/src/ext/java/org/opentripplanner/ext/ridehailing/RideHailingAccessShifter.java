package org.opentripplanner.ext.ridehailing;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.framework.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility method to shift the start of the journey to the earliest time that a vehicle can arrive.
 */
public class RideHailingAccessShifter {

  private static final Logger LOG = LoggerFactory.getLogger(RideHailingAccessShifter.class);
  /**
   * When is a start time far enough in the future so that we don't need to check the service and
   * simply presume that a vehicle can arrive on time.
   */
  private static final Duration MAX_DURATION_FROM_NOW = Duration.ofMinutes(30);

  /**
   * Given a list of {@link RoutingAccessEgress}, shift the access ones that contain driving
   * so that they only start at the time when the ride hailing vehicle can actually be there
   * to pick up passengers.
   */
  public static List<RoutingAccessEgress> shiftAccesses(
    boolean isAccess,
    List<RoutingAccessEgress> results,
    List<RideHailingService> services,
    RouteRequest request,
    Instant now
  ) {
    return results
      .stream()
      .map(ae -> {
        // only time-shift access legs on a car
        // (there could be walk-only accesses if you're close to the stop)
        if (isAccess && ae.getLastState().containsModeCar()) {
          var duration = fetchArrivalDelay(services, request, now);
          if (duration.isSuccess()) {
            return new RideHailingAccessAdapter(ae, duration.successValue());
          } else {
            return null;
          }
        } else {
          return ae;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  /**
   * When you start a car hailing search for right now (which is common) you cannot assume to leave
   * right away but have to take into account the duration it takes for the hailing vehicle to
   * arrive.
   * <p>
   * This method shifts the departure time by the appropriate amount so that the correct
   * access/egresses can be calculated.
   */
  protected static Result<Duration, Error> arrivalDelay(
    RouteRequest req,
    List<RideHailingService> services,
    Instant now
  ) {
    if (shouldShift(req, now)) {
      return shiftTime(req, services, now);
    } else {
      return Result.success(Duration.ZERO);
    }
  }

  private static Result<Duration, Error> fetchArrivalDelay(
    List<RideHailingService> services,
    RouteRequest request,
    Instant now
  ) {
    // we have to shift the start time of a car hailing request because often we cannot leave right
    // away
    if (RideHailingAccessShifter.shouldShift(request, Instant.now())) {
      var shiftingResult = RideHailingAccessShifter.arrivalDelay(request, services, now);
      if (shiftingResult.isSuccess()) {
        return Result.success(shiftingResult.successValue());
      } else {
        LOG.error(
          "Could not fetch arrival time for car hailing service: {}",
          shiftingResult.failureValue()
        );
        return Result.failure(Error.TECHNICAL_ERROR);
      }
    } else {
      return Result.success(Duration.ZERO);
    }
  }

  private static boolean shouldShift(RouteRequest req, Instant now) {
    return (
      req.journey().modes().accessMode == StreetMode.CAR_HAILING &&
      req.dateTime().isBefore(now.plus(MAX_DURATION_FROM_NOW)) &&
      !req.arriveBy()
    );
  }

  private static Result<Duration, Error> shiftTime(
    RouteRequest req,
    List<RideHailingService> services,
    Instant now
  ) {
    try {
      var service = services.get(0);
      var arrivalTimeOpt = service
        .arrivalTimes(new WgsCoordinate(req.from().getCoordinate()), req.journey().wheelchair())
        .stream()
        .min(Comparator.comparing(ArrivalTime::duration));

      if (arrivalTimeOpt.isPresent()) {
        var earliestArrival = arrivalTimeOpt.get();
        var duration = earliestArrival.duration().minus(Duration.between(now, req.dateTime()));
        if (duration.isNegative()) {
          duration = Duration.ZERO;
        }
        return Result.success(duration);
      } else {
        return Result.failure(Error.NO_ARRIVAL_FOR_LOCATION);
      }
    } catch (ExecutionException e) {
      return Result.failure(Error.TECHNICAL_ERROR);
    }
  }

  enum Error {
    NO_ARRIVAL_FOR_LOCATION,
    TECHNICAL_ERROR,
  }
}
