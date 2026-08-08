package org.opentripplanner.service.vehiclerental.street.geofencing;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;

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
 * Rental vertices are created by the updater at runtime, so a network whose zones were applied at
 * graph build time has no updater-side zone index to pre-resolve against. Reading through the
 * repository instead - as {@code VertexLinker} already does for boundary markers - lets those
 * vertices be seeded from zones the updater never computed.
 */
class PreResolveVertexZonesTest {

  private static final String NETWORK = "tier";

  private static final GeofencingZone NO_DROP_OFF = TestGeofencingZoneBuilder.of(id("frogner-park"))
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .withDropOffBanned(true)
    .build();

  @Test
  void seedsVerticesFromZonesRegisteredByAnotherDataSource() {
    var repository = new DefaultVehicleRentalRepository();
    // Registered the way the vehicle rental graph builder registers build-time zones.
    repository.setGeofencingZoneIndex(
      "permanent:" + NETWORK,
      new GeofencingZoneIndex(Set.of(NO_DROP_OFF)),
      Set.of(NO_DROP_OFF)
    );

    var vertex = scooterInsideFrognerPark();

    GeofencingZoneApplier.preResolveVertexZones(List.of(vertex), repository, true);

    assertThat(vertex.getInitialGeofencingZones()).containsExactly(NO_DROP_OFF);
  }

  @Test
  void seedsNothingWhenNoZonesAreRegistered() {
    var vertex = scooterInsideFrognerPark();

    GeofencingZoneApplier.preResolveVertexZones(
      List.of(vertex),
      new DefaultVehicleRentalRepository(),
      true
    );

    assertThat(vertex.getInitialGeofencingZones()).isEmpty();
  }

  private static VehicleRentalPlaceVertex scooterInsideFrognerPark() {
    var coordinate = Polygons.OSLO_FROGNER_PARK.getInteriorPoint();
    var vehicle = VehicleRentalVehicle.of()
      .withId(new FeedScopedId(NETWORK, "scooter-1"))
      .withName(new NonLocalizedString("scooter-1"))
      .withLatitude(coordinate.getY())
      .withLongitude(coordinate.getX())
      .withVehicleType(
        RentalVehicleType.of()
          .withId(new FeedScopedId(NETWORK, "scooter-type"))
          .withFormFactor(RentalFormFactor.SCOOTER)
          .withPropulsionType(RentalVehicleType.PropulsionType.ELECTRIC)
          .withMaxRangeMeters(50000d)
          .build()
      )
      .build();
    return new VehicleRentalPlaceVertex(vehicle);
  }
}
