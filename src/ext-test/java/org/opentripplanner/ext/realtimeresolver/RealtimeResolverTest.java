package org.opentripplanner.ext.realtimeresolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.time.TimeUtils.time;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

class RealtimeResolverTest {

  private final TransitModelForTest testModel = TransitModelForTest.of();

  private final Route route1 = TransitModelForTest.route("route1").build();
  private final Route route2 = TransitModelForTest.route("route2").build();

  private final RegularStop stop1 = testModel.stop("stop1", 1, 1).build();
  private final RegularStop stop2 = testModel.stop("stop2", 2, 1).build();
  private final RegularStop stop3 = testModel.stop("stop3", 3, 1).build();

  @Test
  void testPopulateLegsWithRealtime() {
    var itinerary = newItinerary(Place.forStop(stop1), time("11:00"))
      .bus(route1, 1, time("11:05"), time("11:20"), Place.forStop(stop2))
      .bus(route2, 2, time("11:20"), time("11:40"), Place.forStop(stop3))
      .build();

    // Put a delay on trip 1
    var serviceDate = itinerary.startTime().toLocalDate();
    var patterns = itineraryPatterns(itinerary);
    var delayedPattern = delay(patterns.get(0), 123);
    var transitService = makeTransitService(List.of(delayedPattern, patterns.get(1)), serviceDate);

    // Put an alert on stop3
    var alert = TransitAlert
      .of(stop3.getId())
      .addEntity(new EntitySelector.StopAndRoute(stop3.getId(), route2.getId()))
      .addTimePeriod(new TimePeriod(0, 0))
      .build();
    transitService.getTransitAlertService().setAlerts(List.of(alert));

    var itineraries = List.of(itinerary);
    RealtimeResolver.populateLegsWithRealtime(itineraries, transitService);

    assertEquals(1, itineraries.size());

    var legs = itinerary.getLegs();
    var leg1ArrivalDelay = legs
      .get(0)
      .asScheduledTransitLeg()
      .getTripPattern()
      .getScheduledTimetable()
      .getTripTimes(0)
      .getArrivalDelay(1);
    assertEquals(123, leg1ArrivalDelay);
    assertEquals(0, legs.get(0).getTransitAlerts().size());
    assertEquals(1, legs.get(1).getTransitAlerts().size());
  }

  @Test
  void testPopulateLegsWithRealtimeNonTransit() {
    // Test walk leg and transit leg that doesn't have a corresponding realtime leg
    var itinerary = newItinerary(Place.forStop(stop1), time("11:00"))
      .walk(300, Place.forStop(stop2))
      .bus(route1, 1, time("11:20"), time("11:40"), Place.forStop(stop3))
      .build();

    var model = new TransitModel();
    model.index();
    var transitService = new DefaultTransitService(model);

    var itineraries = List.of(itinerary);
    RealtimeResolver.populateLegsWithRealtime(itineraries, transitService);

    assertEquals(1, itineraries.size());

    var legs = itinerary.getLegs();
    assertEquals(2, legs.size());
    assertTrue(legs.get(0).isWalkingLeg());
    assertTrue(legs.get(1).isTransitLeg());
  }

  @Test
  void testPopulateLegsWithRealtimeKeepStaySeated() {
    var staySeatedItinerary = newItinerary(Place.forStop(stop1), time("11:00"))
      .bus(route1, 1, time("11:05"), time("11:20"), Place.forStop(stop2))
      .staySeatedBus(route2, 2, time("11:20"), time("11:40"), Place.forStop(stop3))
      .build();

    var serviceDate = staySeatedItinerary.startTime().toLocalDate();
    var patterns = itineraryPatterns(staySeatedItinerary);
    var transitService = makeTransitService(patterns, serviceDate);

    var itineraries = List.of(staySeatedItinerary);
    RealtimeResolver.populateLegsWithRealtime(itineraries, transitService);

    assertEquals(1, itineraries.size());

    var constrained = itineraries.get(0).getLegs().get(1).getTransferFromPrevLeg();
    assertNotNull(constrained);
    assertTrue(constrained.getTransferConstraint().isStaySeated());
  }

  private static TripPattern delay(TripPattern pattern1, int seconds) {
    var originalTimeTable = pattern1.getScheduledTimetable();

    var delayedTimetable = new Timetable(pattern1);
    var delayedTripTimes = delay(originalTimeTable.getTripTimes(0), seconds);
    delayedTimetable.addTripTimes(delayedTripTimes);

    return pattern1.copy().withScheduledTimeTable(delayedTimetable).build();
  }

  private static TripTimes delay(TripTimes tt, int seconds) {
    var delayed = tt.copyScheduledTimes();
    IntStream
      .range(0, delayed.getNumStops())
      .forEach(i -> {
        delayed.updateArrivalDelay(i, seconds);
        delayed.updateDepartureDelay(i, seconds);
      });
    return delayed;
  }

  private static List<TripPattern> itineraryPatterns(Itinerary itinerary) {
    return itinerary
      .getLegs()
      .stream()
      .filter(Leg::isScheduledTransitLeg)
      .map(Leg::asScheduledTransitLeg)
      .map(ScheduledTransitLeg::getTripPattern)
      .collect(Collectors.toList());
  }

  private static TransitService makeTransitService(
    List<TripPattern> patterns,
    LocalDate serviceDate
  ) {
    var transitModel = new TransitModel();
    CalendarServiceData calendarServiceData = new CalendarServiceData();

    patterns.forEach(pattern -> {
      transitModel.addTripPattern(pattern.getId(), pattern);

      var serviceCode = pattern.getScheduledTimetable().getTripTimes(0).getServiceCode();
      transitModel.getServiceCodes().put(pattern.getId(), serviceCode);

      calendarServiceData.putServiceDatesForServiceId(pattern.getId(), List.of(serviceDate));
    });

    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);
    transitModel.index();

    var alertService = new TransitAlertServiceImpl(transitModel);
    return new DefaultTransitService(transitModel) {
      @Override
      public TransitAlertService getTransitAlertService() {
        return alertService;
      }
    };
  }
}
