package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;

/**
 * This test makes sure that one of the example feeds in the GTFS-Flex repo works. It's the City of
 * Aspen Downtown taxi service which is a completely unscheduled trip that takes you door-to-door in
 * the city.
 * <p>
 * It only contains a single stop time which in GTFS static would not work but is valid in GTFS
 * Flex.
 */
public class GtfsFlexTest extends FlexTest {

  private static TransitModel transitModel;

  @BeforeAll
  static void setup() {
    TestOtpModel model = FlexTest.buildFlexGraph(ASPEN_GTFS);
    transitModel = model.transitModel();
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

  private static NearbyStop getNearbyStop(FlexTrip<?, ?> trip) {
    assertEquals(1, trip.getStops().size());
    var stopLocation = trip.getStops().iterator().next();
    return new NearbyStop(stopLocation, 0, List.of(), null);
  }

  private static FlexTrip<?, ?> getFlexTrip() {
    var flexTrips = transitModel.getAllFlexTrips();
    return flexTrips.iterator().next();
  }
}
