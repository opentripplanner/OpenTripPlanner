package org.opentripplanner.ext.realtimeresolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.ext.common.AbstractTestBase;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;
import org.opentripplanner.transit.model.TripOnDateDataFetcher;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class ResolverTestBase extends AbstractTestBase {

  @Test
  void populateItineraryLegsWithNoRealTime() {
    var refetchService = createRefetchService(new TransitAlertServiceImpl());
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");
    TripOnDateDataFetcher trip2 = TRANSIT_ENV.tripData("trip4");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);

    StopLocation fromStop = RegularStop.of(
      trip1.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex().toWgsCoordinate())
      .withId(busLeg.to().stop.getId())
      .build();

    StopLocation toStop = RegularStop.of(
      trip2.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex().toWgsCoordinate())
      .withId(trainLeg.from().stop.getId())
      .build();

    var from = Place.forStop(fromStop);
    var to = Place.forStop(toStop);

    var walkLeg = StreetLeg.of()
      .withFrom(from)
      .withMode(TraverseMode.WALK)
      .withTo(to)
      .withStartTime(busLeg.startTime().plusMinutes(1))
      .withEndTime(busLeg.endTime().plusMinutes(10))
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .withDistanceMeters(500)
      .build();

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg, trainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();
    var model = new TransitRepository();
    model.index();
    var transitService = new DefaultTransitService(model);
    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitService,
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    Leg walkingLeg = legs.stream().filter(Leg::isWalkingLeg).findFirst().orElse(null);
    assertEquals(3, legs.size());
    assertEquals(
      "2020-03-03T10:01+01:00[Europe/Paris]",
      Objects.requireNonNull(walkingLeg).startTime().toString()
    );
    assertEquals(
      "2020-03-03T11:10+01:00[Europe/Paris]",
      Objects.requireNonNull(walkingLeg).endTime().toString()
    );
  }

  @Test
  void populateItineraryLegsWithRealTime() {
    var refetchService = createRefetchService(new TransitAlertServiceImpl());
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");
    TripOnDateDataFetcher trip2 = TRANSIT_ENV.tripData("trip4");

    var siri = SiriTestHelper.of(TRANSIT_ENV);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef("trip1")
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("10:00", "10:10")
          .call(STOP_B)
          .arriveAimedExpected("11:00", "11:12")
          .departAimedExpected("11:00", "11:12")
          .call(STOP_D)
          .arriveAimedExpected("12:00", "12:15")
      )
      .buildEstimatedTimetableDeliveries();
    UpdateResult updateResult = siri.applyEstimatedTimetable(updates);
    assertEquals(1, updateResult.successful());

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);

    StopLocation fromStop = RegularStop.of(
      trip1.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex().toWgsCoordinate())
      .withId(busLeg.to().stop.getId())
      .build();

    StopLocation toStop = RegularStop.of(
      trip2.trip().getServiceId(),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("Stop"))
      .withCoordinate(VC.vertex().toWgsCoordinate())
      //Sets wrong id that doesnt match previous leg, in order for stops to not match and force a refetch
      .withId(STOP_D.getId())
      .build();

    var from = Place.forStop(fromStop);
    var to = Place.forStop(toStop);

    var walkLeg = StreetLeg.of()
      .withFrom(from)
      .withMode(TraverseMode.WALK)
      .withTo(to)
      .withStartTime(busLeg.startTime().plusMinutes(1))
      .withEndTime(busLeg.endTime().plusMinutes(10))
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .withDistanceMeters(500)
      .build();

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg, trainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();
    var model = new TransitRepository();
    model.index();
    var transitService = new DefaultTransitService(model);
    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitService,
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    Leg refetchedWalkingLeg = legs.stream().filter(Leg::isWalkingLeg).findFirst().orElse(null);
    assertEquals(3, legs.size());
    //Assert that refetch has updated leg data with realtime data
    assertEquals(
      "2020-03-03T11:12+01:00[Europe/Paris]",
      Objects.requireNonNull(refetchedWalkingLeg).startTime().toString()
    );
    assertEquals(
      "2020-03-03T11:12:10+01:00[Europe/Paris]",
      Objects.requireNonNull(refetchedWalkingLeg).endTime().toString()
    );
  }

  @Test
  void testPopulateLegsWithRealtime() {
    TripOnDateDataFetcher trip5 = TRANSIT_ENV.tripData("trip5");
    TripOnDateDataFetcher trip6 = TRANSIT_ENV.tripData("trip6");

    ScheduledTransitLeg busOneLeg = buildScheduledTransitLeg(trip5, 0, 1);
    ScheduledTransitLeg busTwoLeg = buildScheduledTransitLeg(trip6, 0, 1);

    var itinerary = Itinerary.ofScheduledTransit(List.of(busOneLeg, busTwoLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var siri = SiriTestHelper.of(TRANSIT_ENV);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef("trip5")
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("11:05", "11:07:03")
          .call(STOP_B)
          .arriveAimedExpected("11:20", "11:22:03")
      )
      .buildEstimatedTimetableDeliveries();
    UpdateResult updateResult = siri.applyEstimatedTimetable(updates);
    assertEquals(1, updateResult.successful());

    // Put an alert on stopA
    var transitAlertService = new TransitAlertServiceImpl();
    var alert = TransitAlert.of(STOP_A.getId())
      .addEntity(new EntitySelector.Stop(STOP_A.getId()))
      .withCalendar(AlertCalendar.ofAlwaysActive())
      .build();
    transitAlertService.setAlerts(List.of(alert));

    var itinerariesWithRealtime = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      createRefetchService(transitAlertService),
      TRANSIT_ENV.transitService(),
      transitAlertService,
      routeRequest()
    );

    assertFalse(itinerariesWithRealtime.isEmpty());

    var legs = itinerariesWithRealtime.getFirst().legs();
    var leg1ArrivalDelay = legs.getFirst().asScheduledTransitLeg().endTime();

    assertEquals("2020-03-03T11:22:03+01:00[Europe/Paris]", leg1ArrivalDelay.toString());
    assertEquals(1, legs.get(0).listTransitAlerts().size());
    assertEquals(0, legs.get(1).listTransitAlerts().size());
    assertEquals(1, itinerariesWithRealtime.size());
  }

  @Test
  void testPopulateLegsWithRealtimeNonTransit() {
    // Test walk leg and transit leg that doesn't have a corresponding realtime leg
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);

    Place from = Place.normal(VB.vertex(), VB.vertex().getName());
    Place to = Place.normal(VC.vertex(), VC.vertex().getName());

    StreetLeg walkLeg = StreetLeg.of()
      .withFrom(from)
      .withMode(TraverseMode.WALK)
      .withTo(to)
      .withDistanceMeters(300)
      .withStartTime(busLeg.endTime().plusMinutes(1))
      .withEndTime(busLeg.endTime().plusMinutes(10))
      .withGeneralizedCost(Cost.ZERO.toSeconds())
      .withDistanceMeters(500)
      .build();

    var itinerary = Itinerary.ofScheduledTransit(List.of(busLeg, walkLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var model = new TransitRepository();
    model.index();
    var transitService = new DefaultTransitService(model);

    var itineraries = List.of(itinerary);
    itineraries = RealtimeResolver.populateLegsWithRealtime(
      itineraries,
      createRefetchService(new TransitAlertServiceImpl()),
      transitService,
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    assertEquals(1, itineraries.size());

    var legs = itinerary.legs();
    assertEquals(2, legs.size());
    assertTrue(legs.get(1).isWalkingLeg());
    assertTrue(legs.get(0).isTransitLeg());
  }

  @Test
  void testPopulateLegsKeepStaySeated() {
    var cts = createConstrainedTransferService(staySeated("trip1", 1, "trip2", 0));
    var refetchService = createRefetchService(new TransitAlertServiceImpl());
    TripOnDateDataFetcher trip1 = TRANSIT_ENV.tripData("trip1");
    TripOnDateDataFetcher trip2 = TRANSIT_ENV.tripData("trip2");

    ScheduledTransitLeg busLeg = buildScheduledTransitLeg(trip1, 0, 1);
    ConstrainedTransfer transfer = cts.findTransfer(
      TRANSIT_ENV.tripData("trip1").trip(),
      1,
      STOP_B,
      TRANSIT_ENV.tripData("trip2").trip(),
      0,
      STOP_C
    );
    ScheduledTransitLeg updatedBusLeg = busLeg.copyOf().withTransferToNextLeg(transfer).build();

    ScheduledTransitLeg trainLeg = buildScheduledTransitLeg(trip2, 0, 1);
    ScheduledTransitLeg updatedTrainLeg = trainLeg
      .copyOf()
      .withTransferFromPreviousLeg(transfer)
      .build();
    var itinerary = Itinerary.ofScheduledTransit(List.of(updatedBusLeg, updatedTrainLeg))
      .withGeneralizedCost(Cost.ZERO)
      .build();

    var model = new TransitRepository();
    model.index();

    var transitService = new DefaultTransitService(model);
    List<Itinerary> itineraries = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      refetchService,
      transitService,
      new TransitAlertServiceImpl(),
      routeRequest()
    );

    List<Leg> legs = itineraries.getFirst().legs();
    assertEquals(2, legs.size());
    assertTrue(legs.getFirst().transferToNextLeg().getTransferConstraint().isStaySeated());
    assertNull(legs.getFirst().transferFromPrevLeg());
    assertTrue(legs.get(1).transferFromPrevLeg().getTransferConstraint().isStaySeated());
    assertNull(legs.get(1).transferToNextLeg());
    assertEquals(
      "A ~ BUS trip1 10:00 11:00 ~ B ~ BUS trip2 12:00 13:00 ~ C []",
      itineraries.getFirst().toStr()
    );
  }
}
