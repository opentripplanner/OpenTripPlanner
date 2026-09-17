package org.opentripplanner.ext.taxizone.routing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

class TaxiAccessEgressRouterTest implements PlanTestConstants {

  private static final double FROM_LAT = 59.9000;
  private static final double FROM_LON = 10.7000;
  private static final double TO_LAT = 59.9010;
  private static final double TO_LON = 10.7010;

  private static final WgsCoordinate FROM_COORDINATE = new WgsCoordinate(FROM_LAT, FROM_LON);
  private static final WgsCoordinate TO_COORDINATE = new WgsCoordinate(TO_LAT, TO_LON);

  private static final RouteRequest REQUEST = RouteRequest.of()
    .withFrom(GenericLocation.fromCoordinate(FROM_COORDINATE))
    .withTo(GenericLocation.fromCoordinate(TO_COORDINATE))
    .buildRequest();

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final Route ZONE_ROUTE = TransitRepositoryForTest.route("taxi").build();

  private static final Place PLACE_A = Place.forStop(
    TEST_MODEL.stop("A").withCoordinate(5.0, 8.0).build()
  );
  private static final Place PLACE_B = Place.forStop(
    TEST_MODEL.stop("B").withCoordinate(6.0, 8.5).build()
  );

  private static final TaxiZoneIndex EMPTY_INDEX = new TaxiZoneIndex(List.of());

  // Covers FROM_LAT/FROM_LON..TO_LAT/TO_LON but not PLACE_A/PLACE_B, so it can be used to verify
  // that decoration/filtering uses the given logical coordinates rather than a leg's or a stop's
  // own local coordinates.
  private static final TaxiZone COVERING_ZONE = new TaxiZone(
    Polygons.square(new Coordinate(10.69, 59.89), new Coordinate(10.71, 59.91)),
    ZONE_ROUTE,
    null,
    null
  );
  private static final TaxiZoneIndex COVERING_INDEX = new TaxiZoneIndex(List.of(COVERING_ZONE));

  // A second zone overlapping COVERING_ZONE, used to verify a stop covered by more than one
  // zone is still only returned once.
  private static final TaxiZone OVERLAPPING_ZONE = new TaxiZone(
    Polygons.square(new Coordinate(10.68, 59.88), new Coordinate(10.72, 59.92)),
    ZONE_ROUTE,
    null,
    null
  );
  private static final TaxiZoneIndex OVERLAPPING_INDEX = new TaxiZoneIndex(
    List.of(COVERING_ZONE, OVERLAPPING_ZONE)
  );

  private static final RegularStop COVERED_STOP = TEST_MODEL.stop(
    "covered-stop",
    TO_LAT,
    TO_LON
  ).build();
  private static final RegularStop UNCOVERED_STOP = TEST_MODEL.stop(
    "uncovered-stop",
    65.0,
    20.0
  ).build();

  @Test
  void filterNearbyStopsKeepsOnlyAccessStopsSharingAZoneWithTheOrigin() {
    var transitService = mockTransitService();
    var subject = new TaxiAccessEgressRouter(COVERING_INDEX);
    var covered = nearbyStop(COVERED_STOP);
    var uncovered = nearbyStop(UNCOVERED_STOP);

    var result = subject.filterNearbyStops(
      transitService,
      List.of(covered, uncovered),
      AccessEgressType.ACCESS,
      REQUEST
    );

    assertThat(result).containsExactly(covered);
  }

  @Test
  void filterNearbyStopsKeepsOnlyEgressStopsSharingAZoneWithTheDestination() {
    var transitService = mockTransitService();
    var subject = new TaxiAccessEgressRouter(COVERING_INDEX);
    var covered = nearbyStop(COVERED_STOP);
    var uncovered = nearbyStop(UNCOVERED_STOP);

    var result = subject.filterNearbyStops(
      transitService,
      List.of(covered, uncovered),
      AccessEgressType.EGRESS,
      REQUEST
    );

    assertThat(result).containsExactly(covered);
  }

  @Test
  void filterNearbyStopsDropsAllAccessStopsWhenNoZoneCoversAny() {
    var transitService = mockTransitService();
    var subject = new TaxiAccessEgressRouter(EMPTY_INDEX);

    var result = subject.filterNearbyStops(
      transitService,
      List.of(nearbyStop(COVERED_STOP)),
      AccessEgressType.ACCESS,
      REQUEST
    );

    assertThat(result).isEmpty();
  }

  @Test
  void filterNearbyStopsKeepsStopCoveredByMultipleOverlappingZonesOnlyOnce() {
    var transitService = mockTransitService();
    var subject = new TaxiAccessEgressRouter(OVERLAPPING_INDEX);
    var covered = nearbyStop(COVERED_STOP);

    var result = subject.filterNearbyStops(
      transitService,
      List.of(covered),
      AccessEgressType.ACCESS,
      REQUEST
    );

    assertThat(result).containsExactly(covered);
  }

  @Test
  void decorateAccessEgressLegsReplacesCarLegUsingLogicalCoordinates() {
    // The drive leg's own local coordinates (PLACE_A/PLACE_B) fall outside the zone, but the
    // logical pickup/dropoff coordinates passed in do not, so the leg should still be decorated.
    var walkLeg = TestItineraryBuilder.newItinerary(PLACE_A, T11_00)
      .walk(60, PLACE_B)
      .build()
      .legs()
      .getFirst();
    var driveLeg = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build()
      .legs()
      .getFirst();
    var subject = new TaxiAccessEgressRouter(COVERING_INDEX);
    var pickup = FROM_COORDINATE;
    var dropoff = TO_COORDINATE;

    var result = subject.decorateAccessEgressLegs(List.of(walkLeg, driveLeg), pickup, dropoff);

    assertThat(result.get(0)).isSameInstanceAs(walkLeg);
    assertThat(result.get(1)).isInstanceOf(TaxiZoneLeg.class);
    assertThat(((TaxiZoneLeg) result.get(1)).route()).isEqualTo(ZONE_ROUTE);
  }

  @Test
  void decorateAccessEgressLegsLeavesCarLegUntouchedWhenNoZoneCoversLogicalCoordinates() {
    var driveLeg = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build()
      .legs()
      .getFirst();
    var subject = new TaxiAccessEgressRouter(EMPTY_INDEX);

    var result = subject.decorateAccessEgressLegs(
      List.of(driveLeg),
      FROM_COORDINATE,
      TO_COORDINATE
    );

    assertThat(result).containsExactly(driveLeg);
  }

  private static TransitService mockTransitService() {
    var transitService = mock(TransitService.class);
    when(transitService.getStopLocation(COVERED_STOP.getId())).thenReturn(COVERED_STOP);
    when(transitService.getStopLocation(UNCOVERED_STOP.getId())).thenReturn(UNCOVERED_STOP);
    return transitService;
  }

  private static NearbyStop nearbyStop(RegularStop stop) {
    return new NearbyStop(stop.getId(), 0, null, null);
  }
}
