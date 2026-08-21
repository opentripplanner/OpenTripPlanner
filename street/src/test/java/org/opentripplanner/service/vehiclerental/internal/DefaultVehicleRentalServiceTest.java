package org.opentripplanner.service.vehiclerental.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingZoneIndex;
import org.opentripplanner.street.geometry.Polygons;

class DefaultVehicleRentalServiceTest {

  @Test
  void getVehicleRentalStationForEnvelopeShouldExcludeVehicleRentalVehicle() {
    DefaultVehicleRentalRepository repository = new DefaultVehicleRentalRepository();
    DefaultVehicleRentalService defaultVehicleRentalService = new DefaultVehicleRentalService(
      repository
    );

    VehicleRentalStation vehicleRentalStation = new TestVehicleRentalStationBuilder()
      .withCoordinates(1, 1)
      .build();
    repository.addVehicleRentalStation(vehicleRentalStation);

    VehicleRentalVehicle vehicleRentalVehicle = new TestFreeFloatingRentalVehicleBuilder()
      .withLatitude(2)
      .withLongitude(2)
      .build();
    repository.addVehicleRentalStation(vehicleRentalVehicle);

    List<VehicleRentalStation> vehicleRentalStationForEnvelope =
      defaultVehicleRentalService.getVehicleRentalStationForEnvelope(0, 0, 10, 10);
    assertEquals(1, vehicleRentalStationForEnvelope.size());
    assertEquals(vehicleRentalStation, vehicleRentalStationForEnvelope.get(0));
  }

  @Test
  void listNetworksIsEmptyWithoutRentalData() {
    var service = new DefaultVehicleRentalService(new DefaultVehicleRentalRepository());

    assertThat(service.listNetworks()).isEmpty();
  }

  @Test
  void listNetworksUnionsRentalPlacesAndGeofencingZonesListingEachNetworkOnce() {
    var repository = new DefaultVehicleRentalRepository();
    addVehicle(repository, "voi");
    addVehicle(repository, "bolt");
    addZone(repository, "bolt");
    addZone(repository, "tier");

    var networks = new DefaultVehicleRentalService(repository).listNetworks();

    assertThat(networks).containsExactly("bolt", "tier", "voi").inOrder();
  }

  private void addVehicle(DefaultVehicleRentalRepository repository, String network) {
    repository.addVehicleRentalStation(
      new TestFreeFloatingRentalVehicleBuilder().withNetwork(network).build()
    );
  }

  private void addZone(DefaultVehicleRentalRepository repository, String network) {
    var zone = TestGeofencingZoneBuilder.of(network, "zone")
      .withGeometry(Polygons.OSLO)
      .noDropOff()
      .build();
    repository.setGeofencingZoneIndex(network, new GeofencingZoneIndex(List.of(zone)));
  }
}
