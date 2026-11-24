package org.opentripplanner.routing.refetch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.DESTINATION;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.NETWORK;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.ORIGIN;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.STOP_A;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.createRefetchService;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.legRef;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.request;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.scooter;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.station;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.refetch.ItineraryRefetchServiceTest.GraphBuilder;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingBoundaryExtension;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.TraverseMode;

/// Refetch of an itinerary with a rental access and egress, see {@link RentalRefetchTestModel}.
/// The access is searched backwards from the boarding stop and the egress forwards from the
/// alighting stop, so these tests check that both end in a state the router would also accept.
class ItineraryRefetchServiceRentalTest {

  @ParameterizedTest
  @CsvSource({
    "BIKE_RENTAL, Origin ~ Walk 1m50s ~ bicycle R1 ~ BICYCLE 9:48:40 9:59:10 ~ bicycle R2 ~ Walk 50s ~ A ~ BUS trip1 10:00 12:00 ~ D ~ Walk 1m50s ~ bicycle R3 ~ BICYCLE 12:01:50 12:12:20 ~ bicycle R4 ~ Walk 50s ~ Destination []",
    "CAR_RENTAL, Origin ~ Walk 1m50s ~ car R1 ~ CAR 9:54:12 9:59:10 ~ car R2 ~ Walk 50s ~ A ~ BUS trip1 10:00 12:00 ~ D ~ Walk 1m50s ~ car R3 ~ CAR 12:01:50 12:06:48 ~ car R4 ~ Walk 50s ~ Destination []",
    "SCOOTER_RENTAL, Origin ~ Walk 1m50s ~ scooter R1 ~ SCOOTER 9:49:40 10:00 ~ A ~ BUS trip1 10:00 12:00 ~ D ~ Walk 1m50s ~ scooter R3 ~ SCOOTER 12:01:50 12:12:10 ~ Destination []",
  })
  void refetchWithRentalAccessAndEgress(StreetMode mode, String expected) {
    var itinerary = refetch(request(mode));

    assertEquals(expected, itinerary.toStr());
    assertFalse(itinerary.isArrivedAtDestinationWithRentedVehicle());
  }

  /// The router never keeps a rented vehicle at the end of the access: it has to be dropped off
  /// before boarding. A refetch must not use the destination policy for the boarding stop either.
  @Test
  void accessDropsOffTheRentedVehicleEvenIfItMayBeKeptAtTheDestination() {
    var itinerary = refetch(requestAllowingToKeepTheBicycle());

    var access = itinerary.legs().subList(0, 3);
    assertEquals("bicycle R2", access.get(1).to().name.toString());
    assertTrue(access.get(2).isWalkingLeg());
    assertEquals("A", access.get(2).to().name.toString());
  }

  @Test
  void egressMayArriveAtTheDestinationWithTheRentedVehicle() {
    var itinerary = refetch(requestAllowingToKeepTheBicycle());

    var lastLeg = itinerary.legs().getLast();
    assertEquals(TraverseMode.BICYCLE, ((StreetLeg) lastLeg).getMode());
    assertEquals("Destination", lastLeg.to().name.toString());
    assertTrue(itinerary.isArrivedAtDestinationWithRentedVehicle());
  }

  /// The router drops a free-floating vehicle before it enters a no-drop-off zone. The access is
  /// searched backwards from the boarding stop, so the search has to know the zones of the stop.
  @Test
  void accessDoesNotDropAScooterInANoDropOffZoneAroundTheBoardingStop() {
    var g = GraphBuilder.of();
    var origin = g.vertex();
    var r1 = g.vertex();
    var r2 = g.vertex();
    var va = g.linkStop(STOP_A);
    origin.street(r1).meters(100);
    r1.street(r2).meters(3000);
    r2.street(va).meters(100);
    g.rentalPlace(r1, scooter("scooter R1", r1), SCOOTER);
    var zone = noDropOffZone();
    r2.vertex().addGeofencingBoundary(new GeofencingBoundaryExtension(zone, true));
    va.vertex().addGeofencingBoundary(new GeofencingBoundaryExtension(zone, false));
    var graph = g.build();
    var stopVertex = graph.getStopVertex(STOP_A.getId());
    var zoneService = new ZonesAt(stopVertex.getCoordinate(), zone);

    var itinerary = createRefetchService(graph, zoneService).refetchItinerary(
      GenericLocation.fromCoordinate(origin.coord()),
      null,
      List.of(legRef()),
      request(StreetMode.SCOOTER_RENTAL)
    );

    var access = itinerary.legs().subList(0, 3);
    assertEquals(TraverseMode.SCOOTER, ((StreetLeg) access.get(1)).getMode());
    assertTrue(access.get(2).isWalkingLeg());
    assertEquals("A", access.get(2).to().name.toString());
  }

  /// The reference stores no station or vehicle, so a refetch rents from the vehicles there are
  /// now. With live availability, an empty station is not used.
  @ParameterizedTest
  @CsvSource({
    "true, Origin ~ Walk 26m40s ~ A ~ BUS trip1 10:00 12:00 ~ D []",
    "false, Origin ~ Walk 1m50s ~ bicycle R1 ~ BICYCLE 9:48:40 9:59:10 ~ bicycle R2 ~ Walk 50s ~ A ~ BUS trip1 10:00 12:00 ~ D []",
  })
  void refetchRentsFromTheVehiclesThereAreNow(boolean useAvailability, String expected) {
    var g = GraphBuilder.of();
    var origin = g.vertex();
    var r1 = g.vertex();
    var r2 = g.vertex();
    var va = g.linkStop(STOP_A);
    origin.street(r1).meters(100);
    r1.street(r2).meters(3000);
    r2.street(va).meters(100);
    g.rentalPlace(r1, station(NETWORK, "bicycle R1", r1, BICYCLE, 0), BICYCLE);
    g.rentalPlace(r2, station(NETWORK, "bicycle R2", r2, BICYCLE, 0), BICYCLE);
    var request = request(StreetMode.BIKE_RENTAL)
      .copyOf()
      .withPreferences(p ->
        p.withBike(b -> b.withRental(r -> r.withUseAvailabilityInformation(useAvailability)))
      )
      .buildRequest();

    var itinerary = createRefetchService(g.build(), GeofencingZoneService.EMPTY).refetchItinerary(
      GenericLocation.fromCoordinate(origin.coord()),
      null,
      List.of(legRef()),
      request
    );

    assertEquals(expected, itinerary.toStr());
  }

  private static Itinerary refetch(RouteRequest request) {
    return createRefetchService().refetchItinerary(ORIGIN, DESTINATION, List.of(legRef()), request);
  }

  private static RouteRequest requestAllowingToKeepTheBicycle() {
    return request(StreetMode.BIKE_RENTAL)
      .copyOf()
      .withPreferences(p ->
        p.withBike(b -> b.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(true)))
      )
      .buildRequest();
  }

  private static GeofencingZone noDropOffZone() {
    return new GeofencingZone(
      new FeedScopedId(NETWORK, "no-drop-off"),
      null,
      Polygons.OSLO,
      true,
      false,
      null,
      false,
      null,
      null,
      0
    );
  }

  /// The given zone at the given coordinate, and no zones anywhere else.
  private record ZonesAt(
    Coordinate coordinate,
    GeofencingZone zone
  ) implements GeofencingZoneService {
    @Override
    public Set<GeofencingZone> findZonesContaining(Coordinate coord) {
      return coordinate.equals2D(coord) ? Set.of(zone) : Set.of();
    }

    @Override
    public Set<GeofencingZone> findZonesContaining(Coordinate coord, String network) {
      return zone.id().getFeedId().equals(network) ? findZonesContaining(coord) : Set.of();
    }

    @Override
    public boolean hasIndexedZones() {
      return true;
    }

    @Override
    public Set<GeofencingZone> listZones() {
      return Set.of(zone);
    }
  }
}
