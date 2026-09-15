package org.opentripplanner.routing.refetch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.common.AbstractTestBase;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.constrained.ConstrainedTransferService;
import org.opentripplanner.transfer.constrained.internal.DefaultConstrainedTransferService;
import org.opentripplanner.transit.model.site.RegularStop;

class RefetchItineraryServiceTest extends AbstractTestBase {

  @Test
  void refetchSimple() {
    var refetch = createRefetchService();

    var leg1 = legRef("trip1", STOP_A, STOP_B);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1), routeRequest());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B []", itinerary.toStr());
    assertEquals(0, itinerary.legs().getFirst().boardStopPosInPattern());
    assertEquals(1, itinerary.legs().getFirst().alightStopPosInPattern());
  }

  @Test
  void refetchFromSameStation() {
    var refetch = createRefetchService();

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var from = GenericLocation.fromStopId(STATION_A.getId());

    var itinerary = refetch.refetchItinerary(from, null, List.of(leg1), routeRequest());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B []", itinerary.toStr());
    assertEquals(0, itinerary.legs().getFirst().boardStopPosInPattern());
    assertEquals(1, itinerary.legs().getFirst().alightStopPosInPattern());
  }

  @Test
  void refetchWithTransferAtSameStop() {
    var refetch = createRefetchService();

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var leg2 = legRef("trip2", STOP_B, STOP_C);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B ~ BUS trip2 12:00 13:00 ~ C []", itinerary.toStr());
  }

  @Test
  void refetchWithTransfer() {
    var refetch = createRefetchService();

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var leg2 = legRef("trip3", STOP_C, STOP_D);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D []",
      itinerary.toStr()
    );
    assertEquals("11:00", itinerary.legs().get(1).startTime().toLocalTime().toString());
    assertEquals("11:00:10", itinerary.legs().get(1).endTime().toLocalTime().toString());
  }

  /// It should be possible to fetch an itinerary that has a leg that ends after the later one starts.
  /// This is needed to be able to refetch an itinerary that has a delay on an earlier leg that makes
  /// a transfer impossible to make.
  @Test
  void refetchWithImpossibleTransfer() {
    var refetch = createRefetchService();

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var leg2 = legRef("trip4", STOP_C, STOP_D);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip4 8:30 9:30 ~ D []",
      itinerary.toStr()
    );
    assertEquals(Duration.ofMinutes(-30), itinerary.totalDuration());
  }

  @Test
  void refetchItineraryWithAccessEgress() {
    var refetch = createRefetchService();

    var start = GenericLocation.fromCoordinate(V1.coord());
    var end = GenericLocation.fromCoordinate(V2.coord());

    var leg1 = legRef("trip1", STOP_A, STOP_D);

    var itinerary = refetch.refetchItinerary(start, end, List.of(leg1), routeRequest());

    assertEquals(
      "Origin ~ Walk 5s ~ A ~ BUS trip1 10:00 12:00 ~ D ~ Walk 5s ~ Destination []",
      itinerary.toStr()
    );
  }

  @Test
  void refetchItineraryWithAccessFromStop() {
    var refetch = createRefetchService();

    var start = GenericLocation.fromStopId(STOP_B.getId());
    var leg1 = legRef("trip3", STOP_C, STOP_D);

    var itinerary = refetch.refetchItinerary(start, null, List.of(leg1), routeRequest());

    assertEquals("B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D []", itinerary.toStr());
  }

  @Test
  void refetchItineraryWithMultipleLegsAndAccessEgress() {
    var refetch = createRefetchService();

    var start = GenericLocation.fromCoordinate(V1.coord());
    var end = GenericLocation.fromCoordinate(V2.coord());

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var leg2 = legRef("trip3", STOP_C, STOP_D);

    var itinerary = refetch.refetchItinerary(start, end, List.of(leg1, leg2), routeRequest());

    assertEquals(
      "Origin ~ Walk 5s ~ A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D ~ Walk 5s ~ Destination []",
      itinerary.toStr()
    );
  }

  @Test
  void refetchItineraryWithTwoTransitLegsAndConstrainedTransfer() {
    var cts = createConstrainedTransferService(guaranteed("trip1", 1, "trip2", 0));
    var refetch = createRefetchService(cts);

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var leg2 = legRef("trip2", STOP_B, STOP_D);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest());

    var legs = itinerary.legs();

    assertTrue(legs.getFirst().transferToNextLeg().getTransferConstraint().isGuaranteed());
    assertNull(itinerary.legs().getFirst().transferFromPrevLeg());
    assertTrue(
      Objects.requireNonNull(legs.getLast().transferFromPrevLeg())
        .getTransferConstraint()
        .isGuaranteed()
    );
    assertNull(itinerary.legs().getLast().transferToNextLeg());

    assertEquals("A ~ BUS trip1 10:00 11:00 ~ B ~ BUS trip2 12:00 14:00 ~ D []", itinerary.toStr());
  }

  @Test
  void refetchItineraryWithMultipleConstrainedTransfers() {
    var cts = createConstrainedTransferService(
      staySeated("trip1", 1, "trip3", 0),
      guaranteed("trip3", 1, "trip7", 0)
    );
    var refetch = createRefetchService(cts);

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var leg2 = legRef("trip3", STOP_C, STOP_D);
    var leg3 = legRef("trip7", STOP_D, STOP_E);

    var itinerary = refetch.refetchItinerary(null, null, List.of(leg1, leg2, leg3), routeRequest());

    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D ~ BUS trip7 15:00 16:00 ~ E []",
      itinerary.toStr()
    );

    var legs = itinerary.legs();
    assertNull(legs.get(0).transferFromPrevLeg());
    assertTrue(legs.get(0).transferToNextLeg().getTransferConstraint().isStaySeated());
    assertTrue(legs.get(1).isWalkingLeg());
    assertTrue(legs.get(2).transferFromPrevLeg().getTransferConstraint().isStaySeated());
    assertTrue(legs.get(2).transferToNextLeg().getTransferConstraint().isGuaranteed());
    assertTrue(legs.get(3).transferFromPrevLeg().getTransferConstraint().isGuaranteed());
    assertNull(legs.get(3).transferToNextLeg());
  }

  @Test
  void refetchWithFailedLinking() {
    var refetch = createRefetchService();

    var start = GenericLocation.fromCoordinate(V1.coord().moveNorthMeters(10000));
    var leg1 = legRef("trip1", STOP_A, STOP_B);

    var e = assertThrows(RefetchItineraryException.class, () ->
      refetch.refetchItinerary(start, null, List.of(leg1), routeRequest())
    );
    assertEquals("Could not calculate access", e.getMessage());
  }

  @Test
  void refetchWithFailedTransfer() {
    var refetch = createRefetchService();

    var leg1 = legRef("trip3", STOP_C, STOP_D);
    var leg2 = legRef("trip1", STOP_A, STOP_B);

    var e = assertThrows(RefetchItineraryException.class, () ->
      refetch.refetchItinerary(null, null, List.of(leg1, leg2), routeRequest())
    );
    assertEquals("Could not transfer from F:D to F:A", e.getMessage());
  }

  @Test
  void refetchEmptyLegs() {
    var refetch = createRefetchService();

    assertThrows(IllegalArgumentException.class, () ->
      refetch.refetchItinerary(null, null, List.of(), routeRequest())
    );
  }

  @Test
  void refetchWithBoardAlightSlack() {
    var refetch = createRefetchService();

    var leg1 = legRef("trip1", STOP_A, STOP_B);
    var leg2 = legRef("trip3", STOP_C, STOP_D);
    var start = GenericLocation.fromCoordinate(V1.coord());
    var end = GenericLocation.fromCoordinate(V2.coord());

    var request = routeRequest()
      .copyOf()
      .withPreferences(p ->
        p.withTransit(transit ->
          transit.withDefaultBoardSlackSec(2 * 60).withDefaultAlightSlackSec(3 * 60)
        )
      )
      .buildRequest();

    var itinerary = refetch.refetchItinerary(start, end, List.of(leg1, leg2), request);

    assertEquals(
      "Origin ~ Walk 5s ~ A ~ BUS trip1 10:00 11:00 ~ B ~ Walk 10s ~ C ~ BUS trip3 12:30 13:30 ~ D ~ Walk 5s ~ Destination []",
      itinerary.toStr()
    );
    // Access should have two min board slack
    assertEquals("09:58", itinerary.legs().getFirst().endTime().toLocalTime().toString());
    // Transfer should have three min alight slack
    assertEquals("11:03", itinerary.legs().get(2).startTime().toLocalTime().toString());
    // Egress should have three min alight slack
    assertEquals("13:33", itinerary.legs().getLast().startTime().toLocalTime().toString());
  }

  private ScheduledTransitLegReference legRef(
    String tripId,
    RegularStop boardStop,
    RegularStop alightStop
  ) {
    var tripData = TRANSIT_ENV.tripData(tripId);
    var stops = tripData.tripPattern().getStops();
    var boardPos = stops.indexOf(boardStop);
    var alightPos = stops.indexOf(alightStop);
    assertNotEquals(-1, boardPos);
    assertNotEquals(-1, alightPos);
    return new ScheduledTransitLegReference(
      TRANSIT_ENV.tripData(tripId).trip().getId(),
      SERVICE_DATE,
      boardPos,
      alightPos,
      boardStop.getId(),
      alightStop.getId(),
      null
    );
  }

  private RefetchItineraryService createRefetchService() {
    return createRefetchService(new DefaultConstrainedTransferService());
  }

  private RefetchItineraryService createRefetchService(ConstrainedTransferService cts) {
    StreetDetailsService streetDetailsService = null;
    VertexCreationService vertexCreationService = new VertexCreationService(
      new VertexLinker(
        GRAPH,
        GeofencingZoneService.EMPTY,
        VisibilityMode.TRAVERSE_AREA_EDGES,
        10,
        false
      )
    );
    LinkingContextFactory linkingContextFactory = new LinkingContextFactory(
      GRAPH,
      vertexCreationService
    );
    var streetLimitationParametersService = new StreetLimitationParametersService() {
      @Override
      public float maxCarSpeed() {
        return 100f;
      }

      @Override
      public int maxAreaNodes() {
        return 0;
      }

      @Override
      public float getBestWalkSafety() {
        return 0;
      }

      @Override
      public float getBestBikeSafety() {
        return 0;
      }
    };
    return new RefetchItineraryService(
      GRAPH,
      TRANSIT_ENV.transitService(),
      new TransitAlertServiceImpl(),
      TRANSFER_SERVICE,
      streetDetailsService,
      cts,
      linkingContextFactory,
      streetLimitationParametersService
    );
  }
}
