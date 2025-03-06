package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.service.TimetableRepository;

public class FaresIntegrationTest {

  @Test
  public void testBasic() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.CALTRAIN_GTFS);
    var graph = model.graph();
    var timetableRepository = model.timetableRepository();

    var feedId = timetableRepository.getFeedIds().iterator().next();

    var serverContext = TestServerContext.createServerContext(graph, timetableRepository);

    var start = LocalDateTime.of(2009, Month.AUGUST, 7, 12, 0, 0)
      .atZone(ZoneIds.LOS_ANGELES)
      .toInstant();
    var from = GenericLocation.fromStopId("Origin", feedId, "Millbrae Caltrain");
    var to = GenericLocation.fromStopId("Destination", feedId, "Mountain View Caltrain");

    ItineraryFares fare = getFare(from, to, start, serverContext);
    var product = fare.getLegProducts().values().iterator().next().product();
    assertEquals(Money.usDollars(4.25f), product.price());
    assertEquals("OW_2", product.id().getId().toString());
  }

  @Test
  public void testPortland() {
    TestOtpModel model = ConstantsForTests.getInstance().getCachedPortlandGraph();
    Graph graph = model.graph();
    TimetableRepository timetableRepository = model.timetableRepository();
    var portlandId = timetableRepository.getFeedIds().iterator().next();

    var serverContext = TestServerContext.createServerContext(graph, timetableRepository);

    // from zone 3 to zone 2
    var from = GenericLocation.fromStopId(
      "Portland Int'l Airport MAX Station,Eastbound stop in Portland",
      portlandId,
      "10579"
    );
    var to = GenericLocation.fromStopId(
      "NE 82nd Ave MAX Station,Westbound stop in Portland",
      portlandId,
      "8371"
    );

    Instant startTime = LocalDateTime.of(2009, 11, 1, 12, 0, 0)
      .atZone(ZoneId.of("America/Los_Angeles"))
      .toInstant();

    ItineraryFares fare = getFare(from, to, startTime, serverContext);
    var fpu = List.copyOf(fare.getLegProducts().values());
    assertEquals(1, fpu.size());

    var fp = fpu.getFirst();
    assertEquals(Money.usDollars(2f), fp.product().price());
    assertEquals("prt:19", fp.product().id().toString());

    // long trip

    startTime = LocalDateTime.of(2009, 11, 2, 14, 0, 0)
      .atZone(ZoneId.of("America/Los_Angeles"))
      .toInstant();

    from = GenericLocation.fromStopId("Origin", portlandId, "8389");
    to = GenericLocation.fromStopId("Destination", portlandId, "1252");

    fare = getFare(from, to, startTime, serverContext);
    // this assertion was already commented out when I reactivated the test for OTP2 on 2021-11-11
    // not sure what the correct fare should be
    // assertEquals(new Money(new WrappedCurrency("USD"), 460), fare.getFare(FareType.regular));

    // complex trip
    // request.maxTransfers = 5;
    // startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 14, 0, 0);
    // request.dateTime = startTime;
    // request.from = GenericLocation.fromStopId("", portlandId, "10428");
    // request.setRoutingContext(graph, portlandId + ":10428", portlandId + ":4231");

    //
    // this is commented out because portland's fares are, I think, broken in the gtfs. see
    // thread on gtfs-changes.
    // assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 430));
  }

  private static ItineraryFares getFare(
    GenericLocation from,
    GenericLocation to,
    Instant time,
    OtpServerRequestContext serverContext
  ) {
    Itinerary itinerary = getItineraries(from, to, time, serverContext).get(0);
    return itinerary.getFares();
  }

  private static List<Itinerary> getItineraries(
    GenericLocation from,
    GenericLocation to,
    Instant time,
    OtpServerRequestContext serverContext
  ) {
    RouteRequest request = new RouteRequest();
    request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    request.setDateTime(time);
    request.setFrom(from);
    request.setTo(to);
    request.withPreferences(p ->
      p.withItineraryFilter(it -> it.withDebug(ItineraryFilterDebugProfile.LIST_ALL))
    );

    var result = serverContext.routingService().route(request);

    return result
      .getTripPlan()
      .itineraries.stream()
      .sorted(Comparator.comparingInt(Itinerary::getGeneralizedCost))
      .toList();
  }
}
