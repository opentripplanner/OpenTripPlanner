package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;

public class FaresIntegrationTest {

  private final Currency USD = Currency.getInstance("USD");

  @Test
  public void testBasic() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.CALTRAIN_GTFS);
    var graph = model.graph();
    var transitModel = model.transitModel();

    var feedId = transitModel.getFeedIds().iterator().next();

    var serverContext = TestServerContext.createServerContext(graph, transitModel);

    var start = LocalDateTime
      .of(2009, Month.AUGUST, 7, 12, 0, 0)
      .atZone(ZoneIds.LOS_ANGELES)
      .toInstant();
    var from = GenericLocation.fromStopId("Origin", feedId, "Millbrae Caltrain");
    var to = GenericLocation.fromStopId("Destination", feedId, "Mountain View Caltrain");

    ItineraryFares fare = getFare(from, to, start, serverContext);
    assertEquals(fare.getFare(FareType.regular), new Money(USD, 425));
  }

  @Test
  public void testPortland() {
    TestOtpModel model = ConstantsForTests.getInstance().getCachedPortlandGraph();
    Graph graph = model.graph();
    TransitModel transitModel = model.transitModel();
    var portlandId = transitModel.getFeedIds().iterator().next();

    var serverContext = TestServerContext.createServerContext(graph, transitModel);

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

    Instant startTime = LocalDateTime
      .of(2009, 11, 1, 12, 0, 0)
      .atZone(ZoneId.of("America/Los_Angeles"))
      .toInstant();

    ItineraryFares fare = getFare(from, to, startTime, serverContext);

    assertEquals(new Money(USD, 200), fare.getFare(FareType.regular));

    // long trip

    startTime =
      LocalDateTime.of(2009, 11, 2, 14, 0, 0).atZone(ZoneId.of("America/Los_Angeles")).toInstant();

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

  @Test
  public void testFareComponent() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FARE_COMPONENT_GTFS);
    Graph graph = model.graph();
    TransitModel transitModel = model.transitModel();
    String feedId = transitModel.getFeedIds().iterator().next();

    var serverContext = TestServerContext.createServerContext(graph, transitModel);

    Money tenUSD = new Money(USD, 1000);

    var dateTime = LocalDateTime
      .of(2009, 8, 7, 0, 0, 0)
      .atZone(ZoneId.of("America/Los_Angeles"))
      .toInstant();

    // A -> B, base case

    var from = GenericLocation.fromStopId("Origin", feedId, "A");
    var to = GenericLocation.fromStopId("Destination", feedId, "B");

    var fare = getFare(from, to, dateTime, serverContext);

    var fareComponents = fare.getDetails(FareType.regular);
    assertEquals(fareComponents.size(), 1);
    assertEquals(fareComponents.get(0).price(), tenUSD);
    assertEquals(fareComponents.get(0).fareId(), new FeedScopedId(feedId, "AB"));
    assertEquals(fareComponents.get(0).routes().get(0), new FeedScopedId(feedId, "1"));

    // D -> E, null case

    from = GenericLocation.fromStopId("Origin", feedId, "D");
    to = GenericLocation.fromStopId("Destination", feedId, "E");
    fare = getFare(from, to, dateTime, serverContext);
    assertEquals(ItineraryFares.empty(), fare);

    // A -> C, 2 components in a path

    from = GenericLocation.fromStopId("Origin", feedId, "A");
    to = GenericLocation.fromStopId("Destination", feedId, "C");
    fare = getFare(from, to, dateTime, serverContext);

    fareComponents = fare.getDetails(FareType.regular);
    assertEquals(fareComponents.size(), 2);
    assertEquals(fareComponents.get(0).price(), tenUSD);
    assertEquals(fareComponents.get(0).fareId(), new FeedScopedId(feedId, "AB"));
    assertEquals(fareComponents.get(0).routes().get(0), new FeedScopedId(feedId, "1"));
    assertEquals(fareComponents.get(1).price(), tenUSD);
    assertEquals(fareComponents.get(1).fareId(), new FeedScopedId(feedId, "BC"));
    assertEquals(fareComponents.get(1).routes().get(0), new FeedScopedId(feedId, "2"));

    // B -> D, 2 fully connected components
    from = GenericLocation.fromStopId("Origin", feedId, "B");
    to = GenericLocation.fromStopId("Destination", feedId, "D");
    fare = getFare(from, to, dateTime, serverContext);

    fareComponents = fare.getDetails(FareType.regular);
    assertEquals(fareComponents.size(), 1);
    assertEquals(fareComponents.get(0).price(), tenUSD);
    assertEquals(fareComponents.get(0).fareId(), new FeedScopedId(feedId, "BD"));
    assertEquals(fareComponents.get(0).routes().get(0), new FeedScopedId(feedId, "2"));
    assertEquals(fareComponents.get(0).routes().get(1), new FeedScopedId(feedId, "3"));

    // E -> G, missing in between fare
    from = GenericLocation.fromStopId("Origin", feedId, "E");
    to = GenericLocation.fromStopId("Destination", feedId, "G");
    fare = getFare(from, to, dateTime, serverContext);

    fareComponents = fare.getDetails(FareType.regular);
    assertEquals(fareComponents.size(), 1);
    assertEquals(fareComponents.get(0).price(), tenUSD);
    assertEquals(fareComponents.get(0).fareId(), new FeedScopedId(feedId, "EG"));
    assertEquals(fareComponents.get(0).routes().get(0), new FeedScopedId(feedId, "5"));
    assertEquals(fareComponents.get(0).routes().get(1), new FeedScopedId(feedId, "6"));

    // C -> E, missing fare after
    from = GenericLocation.fromStopId("Origin", feedId, "C");
    to = GenericLocation.fromStopId("Destination", feedId, "E");
    fare = getFare(from, to, dateTime, serverContext);

    fareComponents = fare.getDetails(FareType.regular);
    assertEquals(fareComponents.size(), 1);
    assertEquals(fareComponents.get(0).price(), tenUSD);
    assertEquals(fareComponents.get(0).fareId(), new FeedScopedId(feedId, "CD"));
    assertEquals(fareComponents.get(0).routes().get(0), new FeedScopedId(feedId, "3"));

    // D -> G, missing fare before
    from = GenericLocation.fromStopId("Origin", feedId, "D");
    to = GenericLocation.fromStopId("Destination", feedId, "G");
    fare = getFare(from, to, dateTime, serverContext);

    fareComponents = fare.getDetails(FareType.regular);
    assertEquals(fareComponents.size(), 1);
    assertEquals(fareComponents.get(0).price(), tenUSD);
    assertEquals(fareComponents.get(0).fareId(), new FeedScopedId(feedId, "EG"));
    assertEquals(fareComponents.get(0).routes().get(0), new FeedScopedId(feedId, "5"));
    assertEquals(fareComponents.get(0).routes().get(1), new FeedScopedId(feedId, "6"));

    // A -> D, use individual component parts
    from = GenericLocation.fromStopId("Origin", feedId, "A");
    to = GenericLocation.fromStopId("Destination", feedId, "D");
    fare = getFare(from, to, dateTime, serverContext);

    fareComponents = fare.getDetails(FareType.regular);
    assertEquals(fareComponents.size(), 2);
    assertEquals(fareComponents.get(0).price(), tenUSD);
    assertEquals(fareComponents.get(0).fareId(), new FeedScopedId(feedId, "AB"));
    assertEquals(fareComponents.get(0).routes().get(0), new FeedScopedId(feedId, "1"));
    assertEquals(fareComponents.get(1).price(), tenUSD);
    assertEquals(fareComponents.get(1).fareId(), new FeedScopedId(feedId, "BD"));
    assertEquals(fareComponents.get(1).routes().get(0), new FeedScopedId(feedId, "2"));
    assertEquals(fareComponents.get(1).routes().get(1), new FeedScopedId(feedId, "3"));
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
    request.setDateTime(time);
    request.setFrom(from);
    request.setTo(to);
    request.withPreferences(p -> p.withItineraryFilter(it -> it.withDebug(true)));

    var result = serverContext.routingService().route(request);

    return result
      .getTripPlan()
      .itineraries.stream()
      .sorted(Comparator.comparingInt(Itinerary::getGeneralizedCost))
      .toList();
  }
}
