package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Direction;

class DefaultTransitAlertEntityResolverTest {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();

  private final RegularStop STOP_1 = envBuilder.stopAtStation("S1", "ST");
  private final RegularStop STOP_2 = envBuilder.stopAtStation("S2", "ST");
  private final RegularStop STOP_3 = envBuilder.stop("S3");
  private final Station STATION = STOP_1.getParentStation();

  private final Route BUS_ROUTE = envBuilder.route("R1", b ->
    b.withMode(TransitMode.BUS).withGtfsType(3)
  );
  private final Route TRAM_ROUTE = envBuilder.route("R2", b ->
    b.withMode(TransitMode.TRAM).withGtfsType(0)
  );

  private final TransitTestEnvironment env = envBuilder
    .addTrip(
      TripInput.of("T1").withRoute(BUS_ROUTE).addStop(STOP_1, "12:00").addStop(STOP_3, "12:10")
    )
    .addTrip(
      TripInput.of("T2").withRoute(TRAM_ROUTE).addStop(STOP_2, "13:00").addStop(STOP_3, "13:10")
    )
    .build();

  private final TransitAlertEntityResolver subject = DefaultTransitAlertEntityResolver.of(
    env.transitService()
  );

  private static final FeedScopedId BUS_TRIP = id("T1");
  private static final FeedScopedId TRAM_TRIP = id("T2");

  @Test
  void routesSelectRouteKeysByDefault() {
    var matcher = matcher(b -> b.withRoutes(List.of(routeIds(BUS_ROUTE.getId()))));

    assertTrue(matcher.test(route(BUS_ROUTE)));
    assertFalse(matcher.test(route(TRAM_ROUTE)));
    // not selected, since only the route type is selected by default
    assertFalse(matcher.test(new EntityKey.Trip(BUS_TRIP)));
    assertFalse(matcher.test(stop(STOP_1)));
  }

  @Test
  void routesSelectRelatedEntities() {
    var matcher = matcher(b ->
      b
        .withRoutes(List.of(routeModes(TransitMode.BUS)))
        .withEntityTypes(Arrays.asList(AlertEntityType.values()))
    );
    var agencyId = BUS_ROUTE.getAgency().getId();
    var feedId = BUS_ROUTE.getId().getFeedId();

    assertTrue(matcher.test(route(BUS_ROUTE)));
    assertFalse(matcher.test(route(TRAM_ROUTE)));
    assertTrue(matcher.test(new EntityKey.Trip(BUS_TRIP)));
    assertFalse(matcher.test(new EntityKey.Trip(TRAM_TRIP)));
    assertTrue(matcher.test(new EntityKey.StopAndTrip(STOP_3.getId(), BUS_TRIP)));
    assertFalse(matcher.test(new EntityKey.StopAndTrip(STOP_3.getId(), TRAM_TRIP)));
    assertTrue(matcher.test(new EntityKey.StopAndRoute(STOP_3.getId(), BUS_ROUTE.getId())));
    assertTrue(matcher.test(pattern(BUS_TRIP)));
    assertFalse(matcher.test(pattern(TRAM_TRIP)));
    assertTrue(matcher.test(new EntityKey.Agency(agencyId)));
    assertTrue(matcher.test(new EntityKey.RouteType(feedId, 3)));
    assertFalse(matcher.test(new EntityKey.RouteType(feedId, 0)));
    assertTrue(matcher.test(new EntityKey.RouteTypeAndAgency(agencyId, 3)));
    assertFalse(matcher.test(new EntityKey.RouteTypeAndAgency(agencyId, 0)));
    // stops visited by the route and their parent stations
    assertTrue(matcher.test(stop(STOP_1)));
    assertFalse(matcher.test(stop(STOP_2)));
    assertTrue(matcher.test(new EntityKey.Stop(STATION.getId())));
    assertFalse(matcher.test(new EntityKey.Unknown()));
  }

  @Test
  void stopsSelectStopKeysByDefault() {
    var matcher = matcher(b -> b.withStopsOrStations(List.of(stopIds(STOP_1.getId()))));

    assertTrue(matcher.test(stop(STOP_1)));
    assertFalse(matcher.test(stop(STOP_2)));
    assertFalse(matcher.test(new EntityKey.Stop(STATION.getId())));
    assertFalse(matcher.test(new EntityKey.StopAndRoute(STOP_1.getId(), BUS_ROUTE.getId())));
  }

  @Test
  void stopsCanIncludeParentStations() {
    var matcher = matcher(b ->
      b.withStopsOrStations(
        List.of(
          TransitAlertStopOrStationSelectRequest.of()
            .withIds(List.of(STOP_1.getId()))
            .withIncludeParentStationAlerts(true)
            .build()
        )
      )
    );

    assertTrue(matcher.test(stop(STOP_1)));
    assertTrue(matcher.test(new EntityKey.Stop(STATION.getId())));
    assertFalse(matcher.test(stop(STOP_2)));
  }

  @Test
  void stationsCanIncludeChildStops() {
    var station = matcher(b -> b.withStopsOrStations(List.of(stopIds(STATION.getId()))));
    assertTrue(station.test(new EntityKey.Stop(STATION.getId())));
    assertFalse(station.test(stop(STOP_1)));

    var withChildren = matcher(b ->
      b.withStopsOrStations(
        List.of(
          TransitAlertStopOrStationSelectRequest.of()
            .withIds(List.of(STATION.getId()))
            .withIncludeChildStopAlerts(true)
            .build()
        )
      )
    );
    assertTrue(withChildren.test(new EntityKey.Stop(STATION.getId())));
    assertTrue(withChildren.test(stop(STOP_1)));
    assertTrue(withChildren.test(stop(STOP_2)));
    assertFalse(withChildren.test(stop(STOP_3)));
  }

  @Test
  void flagsWhichDontApplyAreIgnored() {
    var matcher = matcher(b ->
      b.withStopsOrStations(
        List.of(
          TransitAlertStopOrStationSelectRequest.of()
            .withIds(List.of(STOP_1.getId()))
            .withIncludeChildStopAlerts(true)
            .build(),
          TransitAlertStopOrStationSelectRequest.of()
            .withIds(List.of(STATION.getId()))
            .withIncludeParentStationAlerts(true)
            .build()
        )
      )
    );

    assertTrue(matcher.test(stop(STOP_1)));
    assertTrue(matcher.test(new EntityKey.Stop(STATION.getId())));
    assertFalse(matcher.test(stop(STOP_2)));
  }

  @Test
  void stopsAndStationsCanBeSelectedByMode() {
    var matcher = matcher(b -> b.withStopsOrStations(List.of(stopModes(TransitMode.TRAM))));

    assertTrue(matcher.test(stop(STOP_2)));
    assertTrue(matcher.test(stop(STOP_3)));
    assertFalse(matcher.test(stop(STOP_1)));
    // the station has a child stop with the mode
    assertTrue(matcher.test(new EntityKey.Stop(STATION.getId())));
  }

  @Test
  void stopsSelectRelatedEntities() {
    var matcher = matcher(b ->
      b
        .withStopsOrStations(List.of(stopIds(STOP_1.getId())))
        .withEntityTypes(Arrays.asList(AlertEntityType.values()))
    );

    assertTrue(matcher.test(route(BUS_ROUTE)));
    assertFalse(matcher.test(route(TRAM_ROUTE)));
    assertTrue(matcher.test(new EntityKey.Trip(BUS_TRIP)));
    assertFalse(matcher.test(new EntityKey.Trip(TRAM_TRIP)));
    assertTrue(matcher.test(pattern(BUS_TRIP)));
    assertFalse(matcher.test(pattern(TRAM_TRIP)));
    assertTrue(matcher.test(new EntityKey.Agency(BUS_ROUTE.getAgency().getId())));
    assertTrue(matcher.test(new EntityKey.StopAndTrip(STOP_1.getId(), TRAM_TRIP)));
    assertFalse(matcher.test(new EntityKey.StopAndTrip(STOP_3.getId(), BUS_TRIP)));
    assertFalse(matcher.test(new EntityKey.RouteType(BUS_ROUTE.getId().getFeedId(), 3)));
  }

  @Test
  void routesThroughAStationAreSelected() {
    var matcher = matcher(b ->
      b
        .withStopsOrStations(List.of(stopIds(STATION.getId())))
        .withEntityTypes(List.of(AlertEntityType.ROUTE))
    );

    assertTrue(matcher.test(route(BUS_ROUTE)));
    assertTrue(matcher.test(route(TRAM_ROUTE)));
  }

  @Test
  void routesAndStopsSelectStopOnRouteByDefault() {
    var matcher = matcher(b ->
      b
        .withRoutes(List.of(routeIds(BUS_ROUTE.getId())))
        .withStopsOrStations(List.of(stopIds(STOP_3.getId())))
    );

    assertTrue(matcher.test(new EntityKey.StopAndRoute(STOP_3.getId(), BUS_ROUTE.getId())));
    assertFalse(matcher.test(new EntityKey.StopAndRoute(STOP_3.getId(), TRAM_ROUTE.getId())));
    assertFalse(matcher.test(new EntityKey.StopAndRoute(STOP_1.getId(), BUS_ROUTE.getId())));
    assertFalse(matcher.test(stop(STOP_3)));
    assertFalse(matcher.test(route(BUS_ROUTE)));
  }

  @Test
  void routesAndStopsMustBothBeRelated() {
    var matcher = matcher(b ->
      b
        .withRoutes(List.of(routeIds(BUS_ROUTE.getId())))
        .withStopsOrStations(List.of(stopIds(STOP_3.getId())))
        .withEntityTypes(List.of(AlertEntityType.STOP, AlertEntityType.ROUTE))
    );

    assertTrue(matcher.test(stop(STOP_3)));
    assertTrue(matcher.test(route(BUS_ROUTE)));
    assertFalse(matcher.test(stop(STOP_2)));
    assertFalse(matcher.test(route(TRAM_ROUTE)));
  }

  @Test
  void unknownIdsAreMatchedOnlyAsUnknownEntities() {
    var unknown = id("unknown");
    var routes = matcher(b -> b.withRoutes(List.of(routeIds(unknown))));
    var stops = matcher(b -> b.withStopsOrStations(List.of(stopIds(unknown))));

    // the entities don't resolve to anything, so they are not routes or stops
    assertFalse(routes.test(new EntityKey.Route(unknown)));
    assertFalse(stops.test(new EntityKey.Stop(unknown)));

    var unknownRoutes = matcher(b ->
      b.withRoutes(List.of(routeIds(unknown))).withEntityTypes(List.of(AlertEntityType.UNKNOWN))
    );
    var unknownStops = matcher(b ->
      b
        .withStopsOrStations(List.of(stopIds(unknown)))
        .withEntityTypes(List.of(AlertEntityType.UNKNOWN))
    );

    assertTrue(unknownRoutes.test(new EntityKey.Route(unknown)));
    assertFalse(unknownRoutes.test(new EntityKey.Route(id("other"))));
    assertTrue(unknownStops.test(new EntityKey.Stop(unknown)));
    assertFalse(unknownStops.test(new EntityKey.Stop(id("other"))));
  }

  @Test
  void resolvedEntitiesAreOfTheirOwnType() {
    var agencyId = BUS_ROUTE.getAgency().getId();
    var feedId = BUS_ROUTE.getId().getFeedId();

    assertType(AlertEntityType.AGENCY, new EntityKey.Agency(agencyId));
    assertType(AlertEntityType.PATTERN, pattern(BUS_TRIP));
    assertType(AlertEntityType.ROUTE, route(BUS_ROUTE));
    assertType(AlertEntityType.ROUTE_TYPE, new EntityKey.RouteType(feedId, 3));
    assertType(AlertEntityType.ROUTE_TYPE, new EntityKey.RouteTypeAndAgency(agencyId, 3));
    assertType(AlertEntityType.STOP, stop(STOP_1));
    assertType(AlertEntityType.STOP, new EntityKey.Stop(STATION.getId()));
    assertType(
      AlertEntityType.STOP_ON_ROUTE,
      new EntityKey.StopAndRoute(STOP_1.getId(), BUS_ROUTE.getId())
    );
    assertType(AlertEntityType.STOP_ON_TRIP, new EntityKey.StopAndTrip(STOP_1.getId(), BUS_TRIP));
    assertType(AlertEntityType.TRIP, new EntityKey.Trip(BUS_TRIP));
  }

  @Test
  void unresolvedEntitiesAreUnknown() {
    var unknown = id("unknown");

    assertType(AlertEntityType.UNKNOWN, new EntityKey.Unknown());
    assertType(AlertEntityType.UNKNOWN, new EntityKey.Agency(unknown));
    assertType(
      AlertEntityType.UNKNOWN,
      new EntityKey.DirectionAndRoute(unknown, Direction.INBOUND)
    );
    assertType(AlertEntityType.UNKNOWN, new EntityKey.Route(unknown));
    assertType(AlertEntityType.UNKNOWN, new EntityKey.RouteTypeAndAgency(unknown, 3));
    assertType(AlertEntityType.UNKNOWN, new EntityKey.Stop(unknown));
    assertType(AlertEntityType.UNKNOWN, new EntityKey.Trip(unknown));
    // both parts must resolve
    assertType(AlertEntityType.UNKNOWN, new EntityKey.StopAndRoute(STOP_1.getId(), unknown));
    assertType(AlertEntityType.UNKNOWN, new EntityKey.StopAndRoute(unknown, BUS_ROUTE.getId()));
    assertType(AlertEntityType.UNKNOWN, new EntityKey.StopAndTrip(STOP_1.getId(), unknown));
    assertType(AlertEntityType.UNKNOWN, new EntityKey.StopAndTrip(unknown, BUS_TRIP));
  }

  @Test
  void onlyEntityTypes() {
    var matcher = matcher(b -> b.withEntityTypes(List.of(AlertEntityType.AGENCY)));

    assertTrue(matcher.test(new EntityKey.Agency(BUS_ROUTE.getAgency().getId())));
    assertFalse(matcher.test(new EntityKey.Agency(id("any"))));
    assertFalse(matcher.test(route(BUS_ROUTE)));
  }

  @Test
  void emptyEntitySelectorIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      TransitAlertEntitySelectRequest.of().build()
    );
  }

  private Predicate<EntityKey> matcher(
    UnaryOperator<TransitAlertEntitySelectRequestBuilder> customizer
  ) {
    return subject.matcher(customizer.apply(TransitAlertEntitySelectRequest.of()).build());
  }

  private void assertType(AlertEntityType expected, EntityKey key) {
    for (var type : AlertEntityType.values()) {
      var matcher = matcher(b -> b.withEntityTypes(List.of(type)));
      assertEquals(type == expected, matcher.test(key), () -> type + " for " + key);
    }
  }

  private EntityKey pattern(FeedScopedId tripId) {
    var trip = env.transitService().getTrip(tripId);
    var pattern = env.transitService().findPattern(trip);
    return new EntityKey.DirectionAndRoute(trip.getRoute().getId(), pattern.getDirection());
  }

  private static EntityKey route(Route route) {
    return new EntityKey.Route(route.getId());
  }

  private static EntityKey stop(RegularStop stop) {
    return new EntityKey.Stop(stop.getId());
  }

  private static TransitAlertRouteSelectRequest routeIds(FeedScopedId... ids) {
    return TransitAlertRouteSelectRequest.of().withIds(List.of(ids)).build();
  }

  private static TransitAlertRouteSelectRequest routeModes(TransitMode... modes) {
    return TransitAlertRouteSelectRequest.of().withModes(List.of(modes)).build();
  }

  private static TransitAlertStopOrStationSelectRequest stopIds(FeedScopedId... ids) {
    return TransitAlertStopOrStationSelectRequest.of().withIds(List.of(ids)).build();
  }

  private static TransitAlertStopOrStationSelectRequest stopModes(TransitMode... modes) {
    return TransitAlertStopOrStationSelectRequest.of().withModes(List.of(modes)).build();
  }
}
