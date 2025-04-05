package org.opentripplanner.ext.realtimeresolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.utils.time.TimeUtils.time;

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
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;

class RealtimeResolverTest {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private final Route route1 = TimetableRepositoryForTest.route("route1").build();
  private final Route route2 = TimetableRepositoryForTest.route("route2").build();

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
    var alert = TransitAlert.of(stop3.getId())
      .addEntity(new EntitySelector.StopAndRoute(stop3.getId(), route2.getId()))
      .addTimePeriod(new TimePeriod(0, 0))
      .build();
    transitService.getTransitAlertService().setAlerts(List.of(alert));

    var itinerariesWithRealtime = RealtimeResolver.populateLegsWithRealtime(
      List.of(itinerary),
      transitService
    );

    assertFalse(itinerariesWithRealtime.isEmpty());

    var legs = itinerariesWithRealtime.getFirst().legs();
    var leg1ArrivalDelay = legs
      .get(0)
      .asScheduledTransitLeg()
      .getTripPattern()
      .getScheduledTimetable()
      .getTripTimes()
      .getFirst()
      .getArrivalDelay(1);
    assertEquals(123, leg1ArrivalDelay);
    assertEquals(0, legs.get(0).getTransitAlerts().size());
    assertEquals(1, legs.get(1).getTransitAlerts().size());
    assertEquals(1, itinerariesWithRealtime.size());
  }

  @Test
  void testPopulateLegsWithRealtimeNonTransit() {
    // Test walk leg and transit leg that doesn't have a corresponding realtime leg
    var itinerary = newItinerary(Place.forStop(stop1), time("11:00"))
      .walk(300, Place.forStop(stop2))
      .bus(route1, 1, time("11:20"), time("11:40"), Place.forStop(stop3))
      .build();

    var model = new TimetableRepository();
    model.index();
    var transitService = new DefaultTransitService(model);

    var itineraries = List.of(itinerary);
    itineraries = RealtimeResolver.populateLegsWithRealtime(itineraries, transitService);

    assertEquals(1, itineraries.size());

    var legs = itinerary.legs();
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
    itineraries = RealtimeResolver.populateLegsWithRealtime(itineraries, transitService);

    assertEquals(1, itineraries.size());

    var constrained = itineraries.get(0).legs().get(1).getTransferFromPrevLeg();
    assertNotNull(constrained);
    assertTrue(constrained.getTransferConstraint().isStaySeated());
  }

  private static TripPattern delay(TripPattern pattern1, int seconds) {
    var originalTimeTable = pattern1.getScheduledTimetable();

    var delayedTripTimes = delay(originalTimeTable.getTripTimes().getFirst(), seconds);
    var delayedTimetable = Timetable.of()
      .withTripPattern(pattern1)
      .addTripTimes(delayedTripTimes)
      .build();

    return pattern1.copy().withScheduledTimeTable(delayedTimetable).build();
  }

  private static TripTimes delay(TripTimes tt, int seconds) {
    var delayed = tt.copyScheduledTimes();
    IntStream.range(0, delayed.getNumStops()).forEach(i -> {
      delayed.updateArrivalDelay(i, seconds);
      delayed.updateDepartureDelay(i, seconds);
    });
    return delayed;
  }

  private static List<TripPattern> itineraryPatterns(Itinerary itinerary) {
    return itinerary
      .legs()
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
    var timetableRepository = new TimetableRepository();
    CalendarServiceData calendarServiceData = new CalendarServiceData();

    patterns.forEach(pattern -> {
      timetableRepository.addTripPattern(pattern.getId(), pattern);

      var serviceCode = pattern.getScheduledTimetable().getTripTimes().getFirst().getServiceCode();
      timetableRepository.getServiceCodes().put(pattern.getId(), serviceCode);

      calendarServiceData.putServiceDatesForServiceId(pattern.getId(), List.of(serviceDate));
    });

    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );
    timetableRepository.index();

    return new DefaultTransitService(timetableRepository) {
      final TransitAlertService alertService = new TransitAlertServiceImpl(timetableRepository);

      @Override
      public TransitAlertService getTransitAlertService() {
        return alertService;
      }
    };
  }
}
