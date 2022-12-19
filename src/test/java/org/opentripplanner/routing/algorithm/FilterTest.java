package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RouteRequestTransitDataProviderFilter;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FilterTest {

  final String AGENCY_1 = "RUT:Agency:1";
  final String AGENCY_2 = "RUT:Agency:2";
  final String AGENCY_3 = "RUT:Agency:3";

  final String ROUTE_1 = "RUT:Route:1";
  final String ROUTE_2 = "RUT:Route:2";
  final String ROUTE_3 = "RUT:Route:3";
  final String ROUTE_4 = "RUT:Route:4";

  @Test
  @DisplayName(
    """
    Filter test 1
    
    filters: [
      {
        select: [ {A} ]
      }
    ]
    
    -> A
    """
  )
  public void testOne() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();
    var agency2 = TransitModelForTest.agency("B").copy().withId(id(AGENCY_2)).build();
    var agency3 = TransitModelForTest.agency("C").copy().withId(id(AGENCY_3)).build();

    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_2).withAgency(agency2).build(),
      TransitModelForTest.route(ROUTE_3).withAgency(agency3).build()
    );

    var filterRequest = TransitFilterRequest
      .of()
      .addSelect(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_1)).build())
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      List.of(filterRequest),
      routes
    );

    assertEquals(2, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_2)));
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 2
    
    filters: [
      {
        not: [ {A} ]
      }
    ]
        
    -> S - A
    """
  )
  public void testTwo() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();
    var agency2 = TransitModelForTest.agency("B").copy().withId(id(AGENCY_2)).build();
    var agency3 = TransitModelForTest.agency("C").copy().withId(id(AGENCY_3)).build();

    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_2).withAgency(agency2).build(),
      TransitModelForTest.route(ROUTE_3).withAgency(agency3).build()
    );

    var filterRequest = TransitFilterRequest
      .of()
      .addNot(
        SelectRequest.of().withAgencies(List.of(FeedScopedId.parseId("F:" + AGENCY_1))).build()
      )
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      List.of(filterRequest),
      routes
    );

    assertEquals(1, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_1)));
  }

  @Test
  @DisplayName(
    """
    Filter test 3
    
    filters: [
      {
        select: [ {A}, {B} ]
      }
    ]
        
    -> A ∪ B
    """
  )
  public void testThree() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();
    var agency2 = TransitModelForTest.agency("B").copy().withId(id(AGENCY_2)).build();
    var agency3 = TransitModelForTest.agency("C").copy().withId(id(AGENCY_3)).build();

    var routes = List.of(
      TransitModelForTest
        .route(ROUTE_1)
        .withAgency(agency1)
        .withMode(TransitMode.BUS)
        .withNetexSubmode("schoolBus")
        .build(),
      TransitModelForTest
        .route(ROUTE_2)
        .withAgency(agency2)
        .withMode(TransitMode.RAIL)
        .withNetexSubmode("railShuttle")
        .build(),
      TransitModelForTest.route(ROUTE_3).withAgency(agency3).withMode(TransitMode.TRAM).build()
    );

    var filterRequest = TransitFilterRequest
      .of()
      .addSelect(
        SelectRequest
          .of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS, SubMode.of("schoolBus"))))
          .build()
      )
      .addSelect(
        SelectRequest
          .of()
          .withTransportModes(
            List.of(new MainAndSubMode(TransitMode.RAIL, SubMode.of("railShuttle")))
          )
          .build()
      )
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      List.of(filterRequest),
      routes
    );

    assertEquals(1, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 4
    
    filters: [
      {
        select: [ {A} ]
      },
      {
        select: [ {B} ]
      }
    ]
    
    -> A ∪ B
    """
  )
  public void testFour() {
    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).build(),
      TransitModelForTest.route(ROUTE_2).build(),
      TransitModelForTest.route(ROUTE_3).build()
    );

    var filter1 = TransitFilterRequest
      .of()
      .addSelect(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_1)).build())
      .build();

    var filter2 = TransitFilterRequest
      .of()
      .addSelect(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_2)).build())
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      List.of(filter1, filter2),
      routes
    );

    assertEquals(1, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 5
      
    filters: [
      {
        select: [ {A} ]
      },
      {
        not: [ {B} ]
      }
    ]
        
    -> A ∪ (S - B)
    """
  )
  public void testFive() {
    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).build(),
      TransitModelForTest.route(ROUTE_2).build()
    );

    var filter1 = TransitFilterRequest
      .of()
      .addSelect(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_1)).build())
      .build();

    var filter2 = TransitFilterRequest
      .of()
      .addNot(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_1)).build())
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      List.of(filter1, filter2),
      routes
    );

    assertTrue(bannedRoutes.isEmpty());
  }

  @Test
  @DisplayName(
    """
    Filter test 6
    
    filters: [
      {
        select: [ {A} ]
        not: [ {B} ]
      }
    ]
        
    -> A - B
    """
  )
  public void testSix() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();

    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_2).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_3).withAgency(agency1).build()
    );

    var filterRequest = TransitFilterRequest
      .of()
      .addSelect(
        SelectRequest.of().withAgencies(List.of(FeedScopedId.parseId("F:" + AGENCY_1))).build()
      )
      .addNot(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_3)).build())
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      List.of(filterRequest),
      routes
    );

    assertEquals(1, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 7
        
    filters: [
      {
        select: [ {A} ]
      },
      {
        select: [ {B} ]
        not: [ {C} ]
      }
    ]
        
    -> A ∪ (B - C)
    """
  )
  public void testSeven() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();
    var agency2 = TransitModelForTest.agency("B").copy().withId(id(AGENCY_2)).build();

    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_2).withAgency(agency2).build(),
      TransitModelForTest.route(ROUTE_3).withAgency(agency2).build()
    );

    var filter1 = TransitFilterRequest
      .of()
      .addSelect(
        SelectRequest.of().withAgencies(List.of(FeedScopedId.parseId("F:" + AGENCY_1))).build()
      )
      .build();

    var filter2 = TransitFilterRequest
      .of()
      .addSelect(
        SelectRequest.of().withAgencies(List.of(FeedScopedId.parseId("F:" + AGENCY_2))).build()
      )
      .addNot(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_3)).build())
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      List.of(filter1, filter2),
      routes
    );

    assertEquals(1, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 8
    
    filters: [
      {
        select: [ {A,B} ]
      }
    ]
        
    -> A ∩ B
    """
  )
  public void testEight() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();
    var agency2 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_2)).build();

    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).withMode(TransitMode.BUS).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_2).withMode(TransitMode.RAIL).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_3).withMode(TransitMode.BUS).withAgency(agency2).build()
    );

    var filter = TransitFilterRequest
      .of()
      .addSelect(
        SelectRequest
          .of()
          .withAgencies(List.of(FeedScopedId.parseId("F:" + AGENCY_1)))
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .build()
      )
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(List.of(filter), routes);

    assertEquals(2, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_2)));
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 9
        
    filters: [
      {
        select: [ {A,B} ]
        not: [ {C} ]
      }
    ]
        
    -> (A ∩ B) - C
    """
  )
  public void testNine() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();
    var agency2 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_2)).build();

    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).withAgency(agency1).withMode(TransitMode.BUS).build(),
      TransitModelForTest.route(ROUTE_2).withAgency(agency1).withMode(TransitMode.RAIL).build(),
      TransitModelForTest.route(ROUTE_3).withAgency(agency1).withMode(TransitMode.BUS).build(),
      TransitModelForTest.route(ROUTE_4).withAgency(agency2).withMode(TransitMode.BUS).build()
    );

    var filter = TransitFilterRequest
      .of()
      .addSelect(
        SelectRequest
          .of()
          .withAgencies(List.of(FeedScopedId.parseId("F:" + AGENCY_1)))
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .build()
      )
      .addNot(SelectRequest.of().withRoutes(RouteMatcher.parse("F__" + ROUTE_3)).build())
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(List.of(filter), routes);

    assertEquals(3, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_2)));
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
    assertTrue(bannedRoutes.contains(id(ROUTE_4)));
  }

  @Test
  @DisplayName(
    """
    Filter test 10
    
    filters: [
      {
        select: [ {A} ]
        not: [ {B, C} ]
      }
    ]
        
    -> A - (B ∩ C)
    """
  )
  public void testTen() {
    var agency1 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_1)).build();
    var agency2 = TransitModelForTest.agency("A").copy().withId(id(AGENCY_2)).build();

    var routes = List.of(
      TransitModelForTest.route(ROUTE_1).withMode(TransitMode.BUS).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_2).withMode(TransitMode.RAIL).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_3).withMode(TransitMode.BUS).withAgency(agency1).build(),
      TransitModelForTest.route(ROUTE_4).withAgency(agency2).build()
    );

    var filter = TransitFilterRequest
      .of()
      .addSelect(
        SelectRequest.of().withAgencies(List.of(FeedScopedId.parseId("F:" + AGENCY_1))).build()
      )
      .addNot(
        SelectRequest
          .of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .withRoutes(RouteMatcher.parse("F__" + ROUTE_3))
          .build()
      )
      .build();

    var bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(List.of(filter), routes);

    assertEquals(2, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id(ROUTE_4)));
    assertTrue(bannedRoutes.contains(id(ROUTE_3)));
  }
}
