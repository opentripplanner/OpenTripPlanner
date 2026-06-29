package org.opentripplanner.ext.carpickupzone.graphbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.carpickupzone.model.CarPickupZone;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.site.AreaStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a collection of {@link FlexTrip}s from a car pickup zone provider feed into
 * {@link CarPickupZone} objects. Trips that do not satisfy the data requirements are skipped
 * with a warning.
 */
public class CarPickupZoneBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(CarPickupZoneBuilder.class);
  private static final int SECONDS_IN_DAY = 86_400;

  private CarPickupZoneBuilder() {}

  public static List<CarPickupZone> buildZones(Collection<FlexTrip<?, ?>> flexTrips) {
    List<CarPickupZone> result = new ArrayList<>();
    for (FlexTrip<?, ?> flexTrip : flexTrips) {
      if (isValidCarPickupZoneTrip(flexTrip)) {
        var areaStop = (AreaStop) flexTrip.getStop(0);
        result.add(
          new CarPickupZone(
            areaStop.getGeometry(),
            flexTrip.getTrip().getRoute(),
            flexTrip.getPickupBookingInfo(0),
            flexTrip.getDropOffBookingInfo(1)
          )
        );
      }
    }
    return result;
  }

  /**
   * Returns {@code true} if the flex trip satisfies all car pickup zone data requirements.
   * Logs a warning and returns {@code false} for each failed constraint.
   */
  private static boolean isValidCarPickupZoneTrip(FlexTrip<?, ?> flexTrip) {
    return (
      isUnscheduledTrip(flexTrip) &&
      hasNoTimeRestrictions(flexTrip) &&
      hasTwoStops(flexTrip) &&
      hasSingleZone(flexTrip) &&
      hasValidPickupDropoffTypes(flexTrip)
    );
  }

  /** Trip must be an {@link UnscheduledTrip}. */
  private static boolean isUnscheduledTrip(FlexTrip<?, ?> flexTrip) {
    if (flexTrip instanceof UnscheduledTrip) {
      return true;
    }
    LOG.warn(
      "Skipping trip {} for car pickup zones: only UnscheduledTrip is supported; got {}",
      flexTrip.getId(),
      flexTrip.getClass().getSimpleName()
    );
    return false;
  }

  /**
   * No stop may have a meaningful time restriction (start_pickup_dropoff_window / end_pickup_dropoff_window).
   * A full-day window (0:00:00-24:00:00) is treated as "always available" and is allowed.
   */
  private static boolean hasNoTimeRestrictions(FlexTrip<?, ?> flexTrip) {
    for (int i = 0; i < flexTrip.numberOfStops(); i++) {
      int start = flexTrip.earliestDepartureTime(i);
      int end = flexTrip.latestArrivalTime(i);
      boolean hasWindow = start != StopTime.MISSING_VALUE;
      boolean isFullDay = start == 0 && end == SECONDS_IN_DAY;
      if (hasWindow && !isFullDay) {
        LOG.warn(
          "Skipping trip {} for car pickup zones: stop {} has a time restriction" +
            " (start_pickup_dropoff_window / end_pickup_dropoff_window must not be set," +
            " or must span the full day 0:00:00-24:00:00)",
          flexTrip.getId(),
          i
        );
        return false;
      }
    }
    return true;
  }

  /**
   * Trip must have exactly 2 stop times: one pickup stop (index 0) and one drop-off stop (index 1).
   */
  private static boolean hasTwoStops(FlexTrip<?, ?> flexTrip) {
    if (flexTrip.numberOfStops() == 2) {
      return true;
    }
    LOG.warn(
      "Skipping trip {} for car pickup zones: expected exactly 2 stop times, got {}",
      flexTrip.getId(),
      flexTrip.numberOfStops()
    );
    return false;
  }

  /**
   * Both stop times must reference the same GTFS Flex area ({@code location_id}) and that area
   * must have a geometry.
   */
  private static boolean hasSingleZone(FlexTrip<?, ?> flexTrip) {
    if (
      flexTrip.getStop(0) instanceof AreaStop stop0 &&
      flexTrip.getStop(1) instanceof AreaStop stop1 &&
      stop0.equals(stop1) &&
      stop0.getGeometry() != null
    ) {
      return true;
    }
    LOG.warn(
      "Skipping trip {} for car pickup zones: both stop times must reference the same " +
        "GTFS Flex area (location_id) with a geometry",
      flexTrip.getId()
    );
    return false;
  }

  /**
   * Stop 0 must allow pickup and stop 1 must allow drop-off, both with type {@code CALL_AGENCY}
   * or {@code COORDINATE_WITH_DRIVER}.
   */
  private static boolean hasValidPickupDropoffTypes(FlexTrip<?, ?> flexTrip) {
    PickDrop boardRule = flexTrip.getBoardRule(0);
    PickDrop alightRule = flexTrip.getAlightRule(1);
    if (boardRule != PickDrop.CALL_AGENCY && boardRule != PickDrop.COORDINATE_WITH_DRIVER) {
      LOG.warn(
        "Skipping trip {} for car pickup zones: stop 0 has pickup_type {} ({}); " +
          "must be 2 (CALL_AGENCY) or 3 (COORDINATE_WITH_DRIVER)",
        flexTrip.getId(),
        boardRule.ordinal(),
        boardRule
      );
      return false;
    }
    if (alightRule != PickDrop.CALL_AGENCY && alightRule != PickDrop.COORDINATE_WITH_DRIVER) {
      LOG.warn(
        "Skipping trip {} for car pickup zones: stop 1 has drop_off_type {} ({}); " +
          "must be 2 (CALL_AGENCY) or 3 (COORDINATE_WITH_DRIVER)",
        flexTrip.getId(),
        alightRule.ordinal(),
        alightRule
      );
      return false;
    }
    return true;
  }
}
