package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.flex.trip.UnscheduledTrip.isUnscheduledTrip;
import static org.opentripplanner.model.StopTime.MISSING_VALUE;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.flex.FlexTest;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;

/**
 * This test makes sure that one of the example feeds in the GTFS-Flex repo works. It's the City of
 * Aspen Downtown taxi service which is a completely unscheduled trip that takes you door-to-door in
 * the city.
 * <p>
 * It only contains a single stop time which in GTFS static would not work but is valid in GTFS
 * Flex.
 */
public class UnscheduledTripTest extends FlexTest {

  static TransitModel transitModel;

  private static final StopLocation AREA_STOP = TransitModelForTest.areaStopForTest(
    "area",
    GeometryUtils
      .getGeometryFactory()
      .createPolygon(
        new Coordinate[] {
          new Coordinate(11.0, 63.0),
          new Coordinate(11.5, 63.0),
          new Coordinate(11.5, 63.5),
          new Coordinate(11.0, 63.5),
          new Coordinate(11.0, 63.0),
        }
      )
  );

  @Test
  void testIsUnscheduledTrip() {
    var scheduledStop = new StopTime();
    scheduledStop.setArrivalTime(30);
    scheduledStop.setDepartureTime(60);

    var unscheduledStop = new StopTime();
    unscheduledStop.setFlexWindowStart(30);
    unscheduledStop.setFlexWindowEnd(300);

    assertFalse(isUnscheduledTrip(List.of()), "Empty stop times is not a unscheduled trip");
    assertFalse(
      isUnscheduledTrip(List.of(scheduledStop)),
      "Single scheduled stop time is not unscheduled"
    );
    assertTrue(
      isUnscheduledTrip(List.of(unscheduledStop)),
      "Single unscheduled stop time is unscheduled"
    );
    assertTrue(
      isUnscheduledTrip(List.of(unscheduledStop, unscheduledStop)),
      "Two unscheduled stop times is unscheduled"
    );
    assertTrue(
      isUnscheduledTrip(List.of(unscheduledStop, scheduledStop)),
      "Unscheduled + scheduled stop times is unscheduled"
    );
    assertTrue(
      isUnscheduledTrip(List.of(scheduledStop, unscheduledStop)),
      "Scheduled + unscheduled stop times is unscheduled"
    );
    assertFalse(
      isUnscheduledTrip(List.of(scheduledStop, scheduledStop)),
      "Two scheduled stop times is not unscheduled"
    );
    assertFalse(
      isUnscheduledTrip(List.of(unscheduledStop, unscheduledStop, unscheduledStop)),
      "Three unscheduled stop times is not unscheduled"
    );
    assertFalse(
      isUnscheduledTrip(List.of(scheduledStop, scheduledStop, scheduledStop)),
      "Three scheduled stop times is not unscheduled"
    );
  }

  @Test
  void parseAspenTaxiAsUnscheduledTrip() {
    var flexTrips = transitModel.getAllFlexTrips();
    assertFalse(flexTrips.isEmpty());
    assertEquals(
      Set.of("t_1289262_b_29084_tn_0", "t_1289257_b_28352_tn_0"),
      flexTrips.stream().map(FlexTrip::getId).map(FeedScopedId::getId).collect(Collectors.toSet())
    );

    assertEquals(
      Set.of(UnscheduledTrip.class),
      flexTrips.stream().map(FlexTrip::getClass).collect(Collectors.toSet())
    );
  }

  @Test
  void calculateAccessTemplate() {
    var trip = getFlexTrip();
    var nearbyStop = getNearbyStop(trip);

    var accesses = trip
      .getFlexAccessTemplates(nearbyStop, flexDate, calculator, FlexConfig.DEFAULT)
      .toList();

    assertEquals(1, accesses.size());

    var access = accesses.get(0);
    assertEquals(0, access.fromStopIndex);
    assertEquals(0, access.toStopIndex);
  }

  @Test
  void calculateEgressTemplate() {
    var trip = getFlexTrip();
    var nearbyStop = getNearbyStop(trip);
    var egresses = trip
      .getFlexEgressTemplates(nearbyStop, flexDate, calculator, FlexConfig.DEFAULT)
      .toList();

    assertEquals(1, egresses.size());

    var egress = egresses.get(0);
    assertEquals(0, egress.fromStopIndex);
    assertEquals(0, egress.toStopIndex);
  }

  @Test
  void shouldGeneratePatternForFlexTripWithSingleStop() {
    assertFalse(transitModel.getAllTripPatterns().isEmpty());
  }

  @BeforeAll
  static void setup() {
    TestOtpModel model = FlexTest.buildFlexGraph(ASPEN_GTFS);
    transitModel = model.transitModel();
  }

  private static NearbyStop getNearbyStop(FlexTrip<?, ?> trip) {
    assertEquals(1, trip.getStops().size());
    var stopLocation = trip.getStops().iterator().next();
    return new NearbyStop(stopLocation, 0, List.of(), null);
  }

  private static FlexTrip<?, ?> getFlexTrip() {
    var flexTrips = transitModel.getAllFlexTrips();
    return flexTrips.iterator().next();
  }

  @Test
  void testUnscheduledTrip() {
    var fromStopTime = new StopTime();
    fromStopTime.setStop(AREA_STOP);
    fromStopTime.setFlexWindowStart(time(10, 0));
    fromStopTime.setFlexWindowEnd(time(14, 0));

    var toStopTime = new StopTime();
    toStopTime.setStop(AREA_STOP);
    toStopTime.setFlexWindowStart(time(10, 0));
    toStopTime.setFlexWindowEnd(time(14, 0));

    var trip = UnscheduledTrip
      .of(id("UNSCHEDULED"))
      .withStopTimes(List.of(fromStopTime, toStopTime))
      .build();

    assertEquals(time(10, 0), trip.earliestDepartureTime(0));
    assertEquals(time(14, 0), trip.latestArrivalTime(1));

    assertEquals(PickDrop.SCHEDULED, trip.getPickupType(0));
    assertEquals(PickDrop.SCHEDULED, trip.getDropOffType(1));

    // Trip before start of window, return start of window
    assertEquals(time(10, 0), trip.earliestDepartureTime(time(9, 30), 0, 1, time(1, 0)));

    // Trip during window, return current time
    assertEquals(time(10, 30), trip.earliestDepartureTime(time(10, 30), 0, 1, time(1, 0)));

    // Trip at exact end of window, return latest boarding time available
    assertEquals(time(13, 0), trip.earliestDepartureTime(time(13, 0), 0, 1, time(1, 0)));

    // Departs after last possible time for the duration, return not available
    assertEquals(MISSING_VALUE, trip.earliestDepartureTime(time(13, 1), 0, 1, time(1, 0)));

    // This trip fits exactly, return start of window
    assertEquals(time(10, 0), trip.earliestDepartureTime(time(9, 30), 0, 1, time(4, 0)));

    // This trip is too long for the window, return not available
    assertEquals(MISSING_VALUE, trip.earliestDepartureTime(time(6, 30), 0, 1, time(6, 0)));

    // Arrival after end of window, return end of window
    assertEquals(time(14, 0), trip.latestArrivalTime(time(15, 0), 0, 1, time(1, 0)));

    // Arrival during window, return current time
    assertEquals(time(12, 0), trip.latestArrivalTime(time(12, 0), 0, 1, time(1, 0)));

    // Arrival at last possible moment window, return start of window
    assertEquals(time(11, 0), trip.latestArrivalTime(time(11, 0), 0, 1, time(1, 0)));

    // Arrival before first possible time, return not available
    assertEquals(MISSING_VALUE, trip.latestArrivalTime(time(10, 59), 0, 1, time(1, 0)));

    // Arrival after end of window, return end of window
    assertEquals(time(14, 0), trip.latestArrivalTime(time(15, 0), 0, 1, time(4, 0)));

    // Arrival after end of window, return end of window
    assertEquals(MISSING_VALUE, trip.latestArrivalTime(time(21, 0), 0, 1, time(6, 0)));
  }

  @Test
  void testUnscheduledFeederTripFromScheduledStop() {
    var fromStopTime = new StopTime();
    fromStopTime.setStop(TransitModelForTest.stop("stop").build());
    fromStopTime.setDepartureTime(time(10, 0));

    var toStopTime = new StopTime();
    toStopTime.setStop(AREA_STOP);
    toStopTime.setFlexWindowStart(time(10, 0));
    toStopTime.setFlexWindowEnd(time(14, 0));

    var trip = UnscheduledTrip
      .of(id("UNSCHEDULED"))
      .withStopTimes(List.of(fromStopTime, toStopTime))
      .build();

    assertEquals(time(10, 0), trip.earliestDepartureTime(0));
    assertEquals(time(14, 0), trip.latestArrivalTime(1));

    assertEquals(PickDrop.SCHEDULED, trip.getPickupType(0));
    assertEquals(PickDrop.SCHEDULED, trip.getDropOffType(1));

    // Trip before start of window, return start of window
    assertEquals(time(10, 0), trip.earliestDepartureTime(time(9, 30), 0, 1, time(1, 0)));

    // Trip after last possible time for the duration, return not available
    assertEquals(MISSING_VALUE, trip.earliestDepartureTime(time(10, 30), 0, 1, time(1, 0)));

    // This trip is too long for the window, return not available
    assertEquals(MISSING_VALUE, trip.earliestDepartureTime(time(6, 30), 0, 1, time(6, 0)));

    // Arrival after end of window, return end of window
    assertEquals(time(14, 0), trip.latestArrivalTime(time(15, 0), 0, 1, time(1, 0)));

    // Arrival during window, return current time
    assertEquals(time(12, 0), trip.latestArrivalTime(time(12, 0), 0, 1, time(1, 0)));

    // Arrival at last possible moment window, return start of window
    assertEquals(time(11, 0), trip.latestArrivalTime(time(11, 0), 0, 1, time(1, 0)));

    // Arrival before first possible time, return not available
    assertEquals(MISSING_VALUE, trip.latestArrivalTime(time(10, 59), 0, 1, time(1, 0)));

    // Arrival after end of window, return end of window
    assertEquals(time(14, 0), trip.latestArrivalTime(time(15, 0), 0, 1, time(4, 0)));

    // Arrival after end of window, return end of window
    assertEquals(MISSING_VALUE, trip.latestArrivalTime(time(21, 0), 0, 1, time(6, 0)));
  }

  @Test
  void testUnscheduledFeederTripToScheduledStop() {
    var fromStopTime = new StopTime();
    fromStopTime.setStop(AREA_STOP);
    fromStopTime.setFlexWindowStart(time(10, 0));
    fromStopTime.setFlexWindowEnd(time(14, 0));

    var toStopTime = new StopTime();
    toStopTime.setStop(TransitModelForTest.stop("stop").build());
    toStopTime.setArrivalTime(time(14, 0));

    var trip = UnscheduledTrip
      .of(id("UNSCHEDULED"))
      .withStopTimes(List.of(fromStopTime, toStopTime))
      .build();

    assertEquals(time(10, 0), trip.earliestDepartureTime(0));
    assertEquals(time(14, 0), trip.latestArrivalTime(1));

    assertEquals(PickDrop.SCHEDULED, trip.getPickupType(0));
    assertEquals(PickDrop.SCHEDULED, trip.getDropOffType(1));

    // Trip before start of window, return start of window
    assertEquals(time(10, 0), trip.earliestDepartureTime(time(9, 30), 0, 1, time(1, 0)));

    // Trip during window, return current time
    assertEquals(time(10, 30), trip.earliestDepartureTime(time(10, 30), 0, 1, time(1, 0)));

    // Trip at exact end of window, return latest boarding time available
    assertEquals(time(13, 0), trip.earliestDepartureTime(time(13, 0), 0, 1, time(1, 0)));

    // Departs after last possible time for the duration, return not available
    assertEquals(MISSING_VALUE, trip.earliestDepartureTime(time(13, 1), 0, 1, time(1, 0)));

    // This trip fits exactly, return start of window
    assertEquals(time(10, 0), trip.earliestDepartureTime(time(9, 30), 0, 1, time(4, 0)));

    // This trip is too long for the window, return not available
    assertEquals(MISSING_VALUE, trip.earliestDepartureTime(time(6, 30), 0, 1, time(6, 0)));

    // Arrival after end of window, return end of window
    assertEquals(time(14, 0), trip.latestArrivalTime(time(15, 0), 0, 1, time(1, 0)));

    // Arrival before first possible time, return not available
    assertEquals(MISSING_VALUE, trip.latestArrivalTime(time(13, 59), 0, 1, time(1, 0)));

    // Arrival after end of window, return end of window
    assertEquals(MISSING_VALUE, trip.latestArrivalTime(time(21, 0), 0, 1, time(6, 0)));
  }

  int time(int hours, int minutes) {
    return (hours * 60 + minutes) * 60;
  }
}
