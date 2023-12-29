package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_A;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_B;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_D;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.request.filter.TransitPriorityGroupSelect;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

class PriorityGroupConfiguratorTest {

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

  private final RoutingTripPattern railR1 = routeR1.getTripPattern().getRoutingTripPattern();
  private final RoutingTripPattern busB2 = routeB2.getTripPattern().getRoutingTripPattern();
  private final RoutingTripPattern railR3 = routeR3.getTripPattern().getRoutingTripPattern();
  private final RoutingTripPattern ferryF3 = routeF3.getTripPattern().getRoutingTripPattern();
  private final RoutingTripPattern busB3 = routeB3.getTripPattern().getRoutingTripPattern();

  @Test
  void emptyConfigurationShouldReturnGroupZero() {
    var subject = PriorityGroupConfigurator.of(List.of(), List.of());
    assertEquals(subject.baseGroupId(), subject.lookupTransitPriorityGroupId(railR1));
    assertEquals(subject.baseGroupId(), subject.lookupTransitPriorityGroupId(busB2));
    assertEquals(subject.baseGroupId(), subject.lookupTransitPriorityGroupId(null));
  }

  @Test
  void lookupTransitPriorityGroupIdByAgency() {
    var select = TransitPriorityGroupSelect
      .of()
      .addModes(List.of(TransitMode.BUS, TransitMode.RAIL))
      .build();

    // Add matcher `byAgency` for bus and real
    var subject = PriorityGroupConfigurator.of(List.of(select), List.of());

    // Agency groups are indexed (group-id set) at request time
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitPriorityGroupId(null));
    assertEquals(EXP_GROUP_1, subject.lookupTransitPriorityGroupId(busB2));
    assertEquals(EXP_GROUP_2, subject.lookupTransitPriorityGroupId(railR3));
    assertEquals(EXP_GROUP_3, subject.lookupTransitPriorityGroupId(railR1));
    assertEquals(EXP_GROUP_2, subject.lookupTransitPriorityGroupId(busB3));
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitPriorityGroupId(ferryF3));
  }

  @Test
  void lookupTransitPriorityGroupIdByGlobalMode() {
    // Global groups are indexed (group-id set) at construction time
    var subject = PriorityGroupConfigurator.of(
      List.of(),
      List.of(
        TransitPriorityGroupSelect.of().addModes(List.of(TransitMode.BUS)).build(),
        TransitPriorityGroupSelect.of().addModes(List.of(TransitMode.RAIL)).build()
      )
    );

    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitPriorityGroupId(null));
    assertEquals(EXP_GROUP_2, subject.lookupTransitPriorityGroupId(railR1));
    assertEquals(EXP_GROUP_1, subject.lookupTransitPriorityGroupId(busB2));
    assertEquals(EXP_GROUP_2, subject.lookupTransitPriorityGroupId(railR3));
    assertEquals(EXP_GROUP_1, subject.lookupTransitPriorityGroupId(busB3));
    assertEquals(EXP_GROUP_ID_BASE, subject.lookupTransitPriorityGroupId(ferryF3));
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
