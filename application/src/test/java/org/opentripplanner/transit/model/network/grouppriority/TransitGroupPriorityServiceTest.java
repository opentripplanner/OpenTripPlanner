package org.opentripplanner.transit.model.network.grouppriority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_A;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_B;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_D;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestRouteData;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;

class TransitGroupPriorityServiceTest {

  private static final String AGENCY_A1 = "A1";
  private static final String AGENCY_A2 = "A2";
  private static final String AGENCY_A3 = "A3";

  private static final int EXP_GROUP_ID_BASE = 1;
  private static final int EXP_GROUP_1 = 2;
  private static final int EXP_GROUP_2 = 4;
  private static final int EXP_GROUP_3 = 8;

  private final TestRouteData routeR1 = route(
    "R1",
    TransitMode.RAIL,
    AGENCY_A1,
    List.of(STOP_A, STOP_B),
    "10:00 10:10"
  );

  private final TestRouteData routeB2 = route(
    "B2",
    TransitMode.BUS,
    AGENCY_A2,
    List.of(STOP_B, STOP_D),
    "10:15 10:40"
  );
  private final TestRouteData routeR3 = route(
    "R3",
    TransitMode.RAIL,
    AGENCY_A3,
    List.of(STOP_A, STOP_B),
    "10:00 10:10"
  );
  private final TestRouteData routeF3 = route(
    "F3",
    TransitMode.FERRY,
    AGENCY_A3,
    List.of(STOP_A, STOP_B),
    "10:00 10:10"
  );
  private final TestRouteData routeB3 = route(
    "B3",
    TransitMode.BUS,
    AGENCY_A3,
    List.of(STOP_A, STOP_B),
    "10:00 10:10"
  );

  private final TripPattern railR1 = routeR1.getTripPattern();
  private final TripPattern busB2 = routeB2.getTripPattern();
  private final TripPattern railR3 = routeR3.getTripPattern();
  private final TripPattern ferryF3 = routeF3.getTripPattern();
  private final TripPattern busB3 = routeB3.getTripPattern();
  private final TripPattern nullTripPattern = null;
  private final Trip nullTrip = null;

  @Test
  void emptyConfigurationShouldReturnGroupZero() {
    var subject = TransitGroupPriorityService.empty();
    assertEquals(subject.baseGroupId(), subject.lookupTransitGroupPriorityId(railR1));
    assertEquals(subject.baseGroupId(), subject.lookupTransitGroupPriorityId(busB2));
    assertEquals(subject.baseGroupId(), subject.lookupTransitGroupPriorityId(nullTripPattern));
  }

  @Test
  void lookupTransitGroupIdByAgency() {
    var select = TransitGroupSelect.of()
      .addModes(List.of(TransitMode.BUS, TransitMode.RAIL))
      .build();

    // Add matcher `byAgency` for bus and real
    var subject = new TransitGroupPriorityService(List.of(select), List.of());

    // Agency groups are indexed (group-id set) at request time
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitGroupPriorityId(nullTripPattern));
    assertEquals(EXP_GROUP_1, subject.lookupTransitGroupPriorityId(busB2));
    assertEquals(EXP_GROUP_2, subject.lookupTransitGroupPriorityId(railR3));
    assertEquals(EXP_GROUP_3, subject.lookupTransitGroupPriorityId(railR1));
    assertEquals(EXP_GROUP_2, subject.lookupTransitGroupPriorityId(busB3));
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitGroupPriorityId(ferryF3));

    // Verify we get the same result with using the trip, not trip-pattern
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitGroupPriorityId(nullTrip));
    assertEquals(
      EXP_GROUP_1,
      subject.lookupTransitGroupPriorityId(
        busB2.getScheduledTimetable().getTripTimes().getFirst().getTrip()
      )
    );
    assertEquals(
      EXP_GROUP_2,
      subject.lookupTransitGroupPriorityId(
        railR3.getScheduledTimetable().getTripTimes().getFirst().getTrip()
      )
    );
  }

  @Test
  void lookupTransitPriorityGroupIdByGlobalMode() {
    // Global groups are indexed (group-id set) at construction time
    var subject = new TransitGroupPriorityService(
      List.of(),
      List.of(
        TransitGroupSelect.of().addModes(List.of(TransitMode.BUS)).build(),
        TransitGroupSelect.of().addModes(List.of(TransitMode.RAIL)).build()
      )
    );

    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitGroupPriorityId(nullTripPattern));
    assertEquals(EXP_GROUP_2, subject.lookupTransitGroupPriorityId(railR1));
    assertEquals(EXP_GROUP_1, subject.lookupTransitGroupPriorityId(busB2));
    assertEquals(EXP_GROUP_2, subject.lookupTransitGroupPriorityId(railR3));
    assertEquals(EXP_GROUP_1, subject.lookupTransitGroupPriorityId(busB3));
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitGroupPriorityId(ferryF3));

    // Verify we get the same result with using the trip, not trip-pattern
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitGroupPriorityId(nullTrip));
    assertEquals(
      EXP_GROUP_2,
      subject.lookupTransitGroupPriorityId(
        railR1.getScheduledTimetable().getTripTimes().getFirst().getTrip()
      )
    );
  }

  private static TestRouteData route(
    String route,
    TransitMode mode,
    String agency,
    List<RegularStop> stops,
    String times
  ) {
    return new TestRouteData.Builder(route)
      .withMode(mode)
      .withAgency(agency)
      .withStops(stops)
      .build();
  }
}
