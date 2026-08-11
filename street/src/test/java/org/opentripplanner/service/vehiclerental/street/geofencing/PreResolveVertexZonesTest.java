package org.opentripplanner.service.vehiclerental.street.geofencing;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.geometry.Polygons;
import org.opentripplanner.street.model.RentalFormFactor;

/**
 * A rental vertex is seeded from the repository, so it gets its network's zones whichever phase
 * produced them, and only its own network's.
 */
class PreResolveVertexZonesTest {

  private static final String TIER = "tier";
  private static final String VOI = "voi";

  private static final GeofencingZone TIER_NO_DROP_OFF = zone(TIER, "frogner-park");
  private static final GeofencingZone VOI_NO_DROP_OFF = zone(VOI, "frogner-park");

  @Test
  void seedsVerticesFromZonesRegisteredByAnotherPhase() {
    var repository = new DefaultVehicleRentalRepository();
    // Registered the way the geofencing graph builder registers zones during graph build.
    repository.setGeofencingZoneIndex(
      TIER,
      new GeofencingZoneIndex(Set.of(TIER_NO_DROP_OFF)),
      Set.of(TIER_NO_DROP_OFF)
    );

    var vertex = scooterInsideFrognerPark(TIER);

    GeofencingZoneApplier.preResolveVertexZones(List.of(vertex), repository, true);

    assertThat(vertex.getInitialGeofencingZones()).containsExactly(TIER_NO_DROP_OFF);
  }

  /**
   * Other networks' zones are not inert: {@code DeferredForkHandler} and
   * {@code NetworkCommitmentHandler} read the zone set without filtering by the state's network.
   */
  @Test
  void seedsOnlyTheVertexOwnNetworkWhenZonesOverlap() {
    var repository = new DefaultVehicleRentalRepository();
    repository.setGeofencingZoneIndex(TIER, new GeofencingZoneIndex(Set.of(TIER_NO_DROP_OFF)));
    repository.setGeofencingZoneIndex(VOI, new GeofencingZoneIndex(Set.of(VOI_NO_DROP_OFF)));

    var tierVertex = scooterInsideFrognerPark(TIER);
    var voiVertex = scooterInsideFrognerPark(VOI);

    GeofencingZoneApplier.preResolveVertexZones(List.of(tierVertex, voiVertex), repository, true);

    assertThat(tierVertex.getInitialGeofencingZones()).containsExactly(TIER_NO_DROP_OFF);
    assertThat(voiVertex.getInitialGeofencingZones()).containsExactly(VOI_NO_DROP_OFF);
  }

  @Test
  void seedsNothingWhenNoZonesAreRegistered() {
    var vertex = scooterInsideFrognerPark(TIER);

    GeofencingZoneApplier.preResolveVertexZones(
      List.of(vertex),
      new DefaultVehicleRentalRepository(),
      true
    );

    assertThat(vertex.getInitialGeofencingZones()).isEmpty();
  }

  private static GeofencingZone zone(String network, String id) {
    return TestGeofencingZoneBuilder.of(new FeedScopedId(network, id))
      .withGeometry(Polygons.OSLO_FROGNER_PARK)
      .withDropOffBanned(true)
      .build();
  }

  private static VehicleRentalPlaceVertex scooterInsideFrognerPark(String network) {
    var coordinate = Polygons.OSLO_FROGNER_PARK.getInteriorPoint();
    var vehicle = VehicleRentalVehicle.of()
      .withId(new FeedScopedId(network, "scooter-1"))
      .withName(new NonLocalizedString("scooter-1"))
      .withLatitude(coordinate.getY())
      .withLongitude(coordinate.getX())
      .withVehicleType(
        RentalVehicleType.of()
          .withId(new FeedScopedId(network, "scooter-type"))
          .withFormFactor(RentalFormFactor.SCOOTER)
          .withPropulsionType(RentalVehicleType.PropulsionType.ELECTRIC)
          .withMaxRangeMeters(50000d)
          .build()
      )
      .build();
    return new VehicleRentalPlaceVertex(vehicle);
  }
}
