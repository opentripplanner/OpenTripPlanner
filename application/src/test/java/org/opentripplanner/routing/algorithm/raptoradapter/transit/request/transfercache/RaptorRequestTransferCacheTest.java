package org.opentripplanner.routing.algorithm.raptoradapter.transit.request.transfercache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.street.model.StreetMode;

public class RaptorRequestTransferCacheTest {

  @Test
  public void testRaptorRequestTransferCacheKeyWithWheelchair() {
    List<List<Transfer>> list = List.of();

    RouteRequest base = builder()
      .withJourney(b -> b.withAllModes(StreetMode.WALK))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyBase = new RaptorRequestTransferCacheKey(list, base);

    RouteRequest routeRequestWithWheelchairPreferences = base
      .copyOf()
      .withPreferences(p -> p.withWheelchair(b -> b.withStairsReluctance(999)))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithWheelchairPreferences =
      new RaptorRequestTransferCacheKey(list, routeRequestWithWheelchairPreferences);

    RouteRequest routeRequestWithWheelchairPreferencesAndWheelchairEnabled =
      routeRequestWithWheelchairPreferences
        .copyOf()
        .withJourney(j -> j.withWheelchair(true))
        .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithWheelchairPreferencesAndWheelchairEnabled =
      new RaptorRequestTransferCacheKey(
        list,
        routeRequestWithWheelchairPreferencesAndWheelchairEnabled
      );

    assertEquals(cacheKeyWithWheelchairPreferences, cacheKeyBase);
    assertNotEquals(cacheKeyWithWheelchairPreferencesAndWheelchairEnabled, cacheKeyBase);
  }

  @Test
  public void testRaptorRequestTransferCacheKeyWithWalkMode() {
    List<List<Transfer>> list = List.of();

    // This is intentionally CAR in the beginning.
    RouteRequest base = builder()
      .withJourney(b -> b.withAllModes(StreetMode.CAR))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyBase = new RaptorRequestTransferCacheKey(list, base);

    RouteRequest routeRequestWithWalkPreferences = base
      .copyOf()
      .withPreferences(p -> p.withWalk(b -> b.withBoardCost(999)))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithWalkPreferences = new RaptorRequestTransferCacheKey(
      list,
      routeRequestWithWalkPreferences
    );

    RouteRequest routeRequestWithWalkPreferencesAndWalkMode = routeRequestWithWalkPreferences
      .copyOf()
      .withJourney(j -> j.withAllModes(StreetMode.WALK))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithWalkPreferencesAndWalkMode =
      new RaptorRequestTransferCacheKey(list, routeRequestWithWalkPreferencesAndWalkMode);

    assertEquals(cacheKeyWithWalkPreferences, cacheKeyBase);
    assertNotEquals(cacheKeyWithWalkPreferencesAndWalkMode, cacheKeyBase);
  }

  @Test
  public void testRaptorRequestTransferCacheKeyWithBikeMode() {
    List<List<Transfer>> list = List.of();

    // This is intentionally CAR in the beginning.
    RouteRequest base = builder()
      .withJourney(b -> b.withAllModes(StreetMode.CAR))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyBase = new RaptorRequestTransferCacheKey(list, base);

    RouteRequest routeRequestWithBikePreferences = base
      .copyOf()
      .withPreferences(p -> p.withBike(b -> b.withBoardCost(999)))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithBikePreferences = new RaptorRequestTransferCacheKey(
      list,
      routeRequestWithBikePreferences
    );

    RouteRequest routeRequestWithBikePreferencesAndBikeMode = routeRequestWithBikePreferences
      .copyOf()
      .withJourney(j -> j.withAllModes(StreetMode.BIKE))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithBikePreferencesAndBikeMode =
      new RaptorRequestTransferCacheKey(list, routeRequestWithBikePreferencesAndBikeMode);

    assertEquals(cacheKeyWithBikePreferences, cacheKeyBase);
    assertNotEquals(cacheKeyWithBikePreferencesAndBikeMode, cacheKeyBase);
  }

  @Test
  public void testRaptorRequestTransferCacheKeyWithCarMode() {
    List<List<Transfer>> list = List.of();

    // This is intentionally WALK in the beginning.
    RouteRequest base = builder()
      .withJourney(b -> b.withAllModes(StreetMode.WALK))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyBase = new RaptorRequestTransferCacheKey(list, base);

    RouteRequest routeRequestWithCarPreferences = base
      .copyOf()
      .withPreferences(p -> p.withCar(b -> b.withBoardCost(999)))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithCarPreferences = new RaptorRequestTransferCacheKey(
      list,
      routeRequestWithCarPreferences
    );

    RouteRequest routeRequestWithCarPreferencesAndCarMode = routeRequestWithCarPreferences
      .copyOf()
      .withJourney(j -> j.withAllModes(StreetMode.CAR))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithCarPreferencesAndCarMode =
      new RaptorRequestTransferCacheKey(list, routeRequestWithCarPreferencesAndCarMode);

    assertEquals(cacheKeyWithCarPreferences, cacheKeyBase);
    assertNotEquals(cacheKeyWithCarPreferencesAndCarMode, cacheKeyBase);
  }

  @Test
  public void testRaptorRequestTransferCacheKeyWithTurnReluctance() {
    List<List<Transfer>> list = List.of();

    RouteRequest base = builder()
      .withJourney(b -> b.withAllModes(StreetMode.WALK))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyBase = new RaptorRequestTransferCacheKey(list, base);

    RouteRequest routeRequestWithTurnReluctance = base
      .copyOf()
      .withPreferences(p -> p.withStreet(b -> b.withTurnReluctance(999)))
      .buildRequest();
    RaptorRequestTransferCacheKey cacheKeyWithTurnReluctance = new RaptorRequestTransferCacheKey(
      list,
      routeRequestWithTurnReluctance
    );

    assertNotEquals(cacheKeyWithTurnReluctance, cacheKeyBase);
  }

  private static RouteRequestBuilder builder() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1));
  }
}
