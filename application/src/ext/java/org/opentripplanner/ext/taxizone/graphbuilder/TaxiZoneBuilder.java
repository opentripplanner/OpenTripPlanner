package org.opentripplanner.ext.taxizone.graphbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.AreaStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a collection of {@link FlexTrip}s from a taxi zone provider feed into
 * {@link TaxiZone} objects. Trips that do not satisfy the data requirements are skipped
 * with a warning.
 */
public class TaxiZoneBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(TaxiZoneBuilder.class);
  private static final int SECONDS_IN_DAY = 86_400;

  private TaxiZoneBuilder() {}

  public static List<TaxiZone> buildZones(
    Collection<FlexTrip<?, ?>> flexTrips,
    CalendarServiceData calendarServiceData
  ) {
    List<TaxiZone> result = new ArrayList<>();
    for (FlexTrip<?, ?> flexTrip : flexTrips) {
      if (!isValidTaxiZoneTrip(flexTrip)) {
        continue;
      }
      var serviceDateRange = continuousServiceDateRange(flexTrip, calendarServiceData);
      if (serviceDateRange.isEmpty()) {
        continue;
      }
      var areaStop = (AreaStop) flexTrip.getStop(0);
      result.add(
        new TaxiZone(
          areaStop.getGeometry(),
          flexTrip.getTrip().getRoute(),
          flexTrip.getPickupBookingInfo(0),
          flexTrip.getDropOffBookingInfo(1),
          serviceDateRange.get()
        )
      );
    }
    return result;
  }

  private static boolean isValidTaxiZoneTrip(FlexTrip<?, ?> flexTrip) {
    // Order matters!
    // - isUnscheduledTrip must run first, since only UnscheduledTrip guarantees getTrip() is
    //   non-null, which the checks after it (and continuousServiceDateRange) rely on.
    // - hasTwoStops must run before hasSingleZone and hasValidPickupDropoffTypes, since those
    //   access stop index 1 directly and would throw if fewer than two stops are present.
    return (
      isUnscheduledTrip(flexTrip) &&
      hasTaxiRouteType(flexTrip) &&
      hasNoTimeRestrictions(flexTrip) &&
      hasTwoStops(flexTrip) &&
      hasSingleZone(flexTrip) &&
      hasValidPickupDropoffTypes(flexTrip)
    );
  }

  private static boolean isUnscheduledTrip(FlexTrip<?, ?> flexTrip) {
    if (flexTrip instanceof UnscheduledTrip) {
      return true;
    }
    LOG.warn(
      "Skipping trip {} for taxi zones: only UnscheduledTrip is supported; got {}",
      flexTrip.getId(),
      flexTrip.getClass().getSimpleName()
    );
    return false;
  }

  private static boolean hasTaxiRouteType(FlexTrip<?, ?> flexTrip) {
    TransitMode mode = flexTrip.getTrip().getMode();
    if (mode == TransitMode.TAXI) {
      return true;
    }
    LOG.warn(
      "Skipping trip {} for taxi zones: route mode is {}; must be TAXI (GTFS route_type 1500-1599)",
      flexTrip.getId(),
      mode
    );
    return false;
  }

  private static boolean hasNoTimeRestrictions(FlexTrip<?, ?> flexTrip) {
    for (int i = 0; i < flexTrip.numberOfStops(); i++) {
      int start = flexTrip.earliestDepartureTime(i);
      int end = flexTrip.latestArrivalTime(i);
      boolean hasWindow = start != StopTime.MISSING_VALUE;
      boolean isFullDay = start == 0 && end == SECONDS_IN_DAY;
      if (hasWindow && !isFullDay) {
        LOG.warn(
          "Skipping trip {} for taxi zones: stop {} has a time restriction" +
            " (start_pickup_dropoff_window / end_pickup_dropoff_window must not be set," +
            " or must span the full day 0:00:00-24:00:00)",
          flexTrip.getId(),
          flexTrip.getStop(i)
        );
        return false;
      }
    }
    return true;
  }

  private static boolean hasTwoStops(FlexTrip<?, ?> flexTrip) {
    if (flexTrip.numberOfStops() == 2) {
      return true;
    }
    LOG.warn(
      "Skipping trip {} for taxi zones: expected exactly 2 stop times " +
        "(one pickup stop and one drop-off stop), got {}",
      flexTrip.getId(),
      flexTrip.numberOfStops()
    );
    return false;
  }

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
      "Skipping trip {} for taxi zones: both stop times must reference the same " +
        "GTFS Flex area (location_id) with a geometry",
      flexTrip.getId()
    );
    return false;
  }

  private static boolean hasValidPickupDropoffTypes(FlexTrip<?, ?> flexTrip) {
    PickDrop boardRule = flexTrip.getBoardRule(0);
    PickDrop alightRule = flexTrip.getAlightRule(1);
    if (boardRule != PickDrop.CALL_AGENCY) {
      LOG.warn(
        "Skipping trip {} for taxi zones: stop 0 has pickup_type {} ({}); " +
          "must be 2 (CALL_AGENCY)",
        flexTrip.getId(),
        boardRule.ordinal(),
        boardRule
      );
      return false;
    }
    if (alightRule != PickDrop.CALL_AGENCY) {
      LOG.warn(
        "Skipping trip {} for taxi zones: stop 1 has drop_off_type {} ({}); " +
          "must be 2 (CALL_AGENCY)",
        flexTrip.getId(),
        alightRule.ordinal(),
        alightRule
      );
      return false;
    }
    return true;
  }

  /**
   * Resolves a trip's GTFS service calendar and, if its dates form one contiguous run of days
   * with no gaps, returns the equivalent {@link LocalDateRange}. Returns empty (with a warning)
   * if the service has no valid dates, or if its dates contain any gap (e.g. a
   * partial/weekday-only calendar).
   */
  private static Optional<LocalDateRange> continuousServiceDateRange(
    FlexTrip<?, ?> flexTrip,
    CalendarServiceData calendarServiceData
  ) {
    var serviceId = flexTrip.getTrip().getServiceId();
    var serviceDates = calendarServiceData.getServiceDatesForServiceId(serviceId);
    if (serviceDates != null && !serviceDates.isEmpty()) {
      var sortedDates = new ArrayList<>(serviceDates);
      sortedDates.sort(null);
      var range = LocalDateRange.ofInclusiveEnd(
        sortedDates.get(0),
        sortedDates.get(sortedDates.size() - 1)
      );
      if (sortedDates.size() == range.daysInPeriod()) {
        return Optional.of(range);
      }
    }
    LOG.warn(
      "Skipping trip {} for taxi zones: service {} must have at least one valid service date " +
        "and run every day within its service period, with no gaps " +
        "(missing from calendar.txt / calendar_dates.txt, or a partial/weekday-only calendar)",
      flexTrip.getId(),
      serviceId
    );
    return Optional.empty();
  }
}
