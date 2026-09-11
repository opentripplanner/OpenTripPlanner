package org.opentripplanner.ext.taxizone.graphbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * Converts a collection of {@link FlexTrip}s from a taxi zone provider feed into
 * {@link TaxiZone} objects. Trips that do not satisfy the data requirements are skipped
 * and reported as {@link TaxiZoneTripSkipped} data import issues.
 */
public class TaxiZoneBuilder {

  private static final int SECONDS_IN_DAY = 86_400;

  private final DataImportIssueStore issueStore;

  public TaxiZoneBuilder(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  public List<TaxiZone> buildZones(Collection<FlexTrip<?, ?>> flexTrips) {
    List<TaxiZone> result = new ArrayList<>();
    for (FlexTrip<?, ?> flexTrip : flexTrips) {
      if (isValidTaxiZoneTrip(flexTrip)) {
        var areaStop = (AreaStop) flexTrip.getStop(0);
        result.add(
          new TaxiZone(
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

  private boolean isValidTaxiZoneTrip(FlexTrip<?, ?> flexTrip) {
    // Order matters!
    // - isUnscheduledTrip must run first, since only UnscheduledTrip guarantees getTrip() is
    //   non-null, which the checks after it rely on.
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

  private boolean isUnscheduledTrip(FlexTrip<?, ?> flexTrip) {
    if (flexTrip instanceof UnscheduledTrip) {
      return true;
    }
    issueStore.add(
      new TaxiZoneTripSkipped(
        flexTrip.getId(),
        "only UnscheduledTrip is supported; got %s".formatted(flexTrip.getClass().getSimpleName())
      )
    );
    return false;
  }

  private boolean hasTaxiRouteType(FlexTrip<?, ?> flexTrip) {
    TransitMode mode = flexTrip.getTrip().getMode();
    if (mode == TransitMode.TAXI) {
      return true;
    }
    issueStore.add(
      new TaxiZoneTripSkipped(
        flexTrip.getId(),
        "route mode is %s; must be TAXI (GTFS route_type 1500-1599)".formatted(mode)
      )
    );
    return false;
  }

  private boolean hasNoTimeRestrictions(FlexTrip<?, ?> flexTrip) {
    for (int i = 0; i < flexTrip.numberOfStops(); i++) {
      int start = flexTrip.earliestDepartureTime(i);
      int end = flexTrip.latestArrivalTime(i);
      boolean hasWindow = start != StopTime.MISSING_VALUE;
      boolean isFullDay = start == 0 && end == SECONDS_IN_DAY;
      if (hasWindow && !isFullDay) {
        issueStore.add(
          new TaxiZoneTripSkipped(
            flexTrip.getId(),
            (
              "stop %s has a time restriction (start_pickup_dropoff_window / " +
              "end_pickup_dropoff_window must not be set, or must span the full day " +
              "0:00:00-24:00:00)"
            ).formatted(flexTrip.getStop(i))
          )
        );
        return false;
      }
    }
    return true;
  }

  private boolean hasTwoStops(FlexTrip<?, ?> flexTrip) {
    if (flexTrip.numberOfStops() == 2) {
      return true;
    }
    issueStore.add(
      new TaxiZoneTripSkipped(
        flexTrip.getId(),
        "expected exactly 2 stop times (one pickup stop and one drop-off stop), got %d".formatted(
          flexTrip.numberOfStops()
        )
      )
    );
    return false;
  }

  private boolean hasSingleZone(FlexTrip<?, ?> flexTrip) {
    if (
      flexTrip.getStop(0) instanceof AreaStop stop0 &&
      flexTrip.getStop(1) instanceof AreaStop stop1 &&
      stop0.equals(stop1) &&
      stop0.getGeometry() != null
    ) {
      return true;
    }
    issueStore.add(
      new TaxiZoneTripSkipped(
        flexTrip.getId(),
        "both stop times must reference the same GTFS Flex area (location_id) with a geometry"
      )
    );
    return false;
  }

  private boolean hasValidPickupDropoffTypes(FlexTrip<?, ?> flexTrip) {
    PickDrop boardRule = flexTrip.getBoardRule(0);
    PickDrop alightRule = flexTrip.getAlightRule(1);
    if (boardRule != PickDrop.CALL_AGENCY) {
      issueStore.add(
        new TaxiZoneTripSkipped(
          flexTrip.getId(),
          "stop 0 has pickup_type %d (%s); must be 2 (CALL_AGENCY)".formatted(
            boardRule.ordinal(),
            boardRule
          )
        )
      );
      return false;
    }
    if (alightRule != PickDrop.CALL_AGENCY) {
      issueStore.add(
        new TaxiZoneTripSkipped(
          flexTrip.getId(),
          "stop 1 has drop_off_type %d (%s); must be 2 (CALL_AGENCY)".formatted(
            alightRule.ordinal(),
            alightRule
          )
        )
      );
      return false;
    }
    return true;
  }
}
