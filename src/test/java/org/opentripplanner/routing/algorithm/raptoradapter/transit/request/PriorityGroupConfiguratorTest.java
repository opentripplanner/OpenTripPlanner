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

class PriorityGroupConfiguratorTest {

  private final TestRouteData routeA = TestRouteData.of(
    "R1",
    TransitMode.RAIL,
    List.of(STOP_A, STOP_B),
    "10:00 10:10"
  );
  private final TestRouteData routeB = TestRouteData.of(
    "B2",
    TransitMode.BUS,
    List.of(STOP_B, STOP_D),
    "10:15 10:40"
  );
  private final TestRouteData routeC = TestRouteData.of(
    "R3",
    TransitMode.RAIL,
    List.of(STOP_A, STOP_B),
    "10:00 10:10"
  );
  private final TestRouteData routeD = TestRouteData.of(
    "R3",
    TransitMode.FERRY,
    List.of(STOP_A, STOP_B),
    "10:00 10:10"
  );

  private final RoutingTripPattern railA = routeA.getTripPattern().getRoutingTripPattern();
  private final RoutingTripPattern busB = routeB.getTripPattern().getRoutingTripPattern();
  private final RoutingTripPattern railC = routeC.getTripPattern().getRoutingTripPattern();
  private final RoutingTripPattern ferryC = routeD.getTripPattern().getRoutingTripPattern();

  @Test
  void emptyConfigurationShouldReturnGroupZero() {
    var subject = PriorityGroupConfigurator.of(List.of(), List.of(), List.of());
    assertEquals(0, subject.lookupTransitPriorityGroupId(railA));
    assertEquals(0, subject.lookupTransitPriorityGroupId(busB));
    assertEquals(0, subject.lookupTransitPriorityGroupId(null));
  }

  @Test
  void lookupTransitPriorityGroupIdBySameAgency() {
    var subject = PriorityGroupConfigurator.of(
      List.of(),
      List.of(
        TransitPriorityGroupSelect.of().addModes(List.of(TransitMode.BUS)).build(),
        TransitPriorityGroupSelect.of().addModes(List.of(TransitMode.RAIL)).build()
      ),
      List.of()
    );

    assertEquals(0, subject.lookupTransitPriorityGroupId(null));
    assertEquals(0, subject.lookupTransitPriorityGroupId(ferryC));
    assertEquals(1, subject.lookupTransitPriorityGroupId(railA));
    assertEquals(2, subject.lookupTransitPriorityGroupId(busB));
    assertEquals(1, subject.lookupTransitPriorityGroupId(railC));
  }

  @Test
  void lookupTransitPriorityGroupIdByGlobalMode() {
    var subject = PriorityGroupConfigurator.of(
      List.of(),
      List.of(),
      List.of(
        TransitPriorityGroupSelect.of().addModes(List.of(TransitMode.BUS)).build(),
        TransitPriorityGroupSelect.of().addModes(List.of(TransitMode.RAIL)).build()
      )
    );

    assertEquals(0, subject.lookupTransitPriorityGroupId(null));
    assertEquals(0, subject.lookupTransitPriorityGroupId(ferryC));
    assertEquals(2, subject.lookupTransitPriorityGroupId(railA));
    assertEquals(1, subject.lookupTransitPriorityGroupId(busB));
    assertEquals(2, subject.lookupTransitPriorityGroupId(railC));
  }
}
