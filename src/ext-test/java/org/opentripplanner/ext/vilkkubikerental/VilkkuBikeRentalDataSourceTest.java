package org.opentripplanner.ext.vilkkubikerental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

public class VilkkuBikeRentalDataSourceTest {

  private VilkkuBikeRentalDataSource source;

  @BeforeEach
  public void initDatasoure() {
    this.source =
      new VilkkuBikeRentalDataSource(
        new VilkkuBikeRentalDataSourceParameters(
          "file:src/ext-test/resources/vilkkubikerental/vilkku.xml",
          null,
          false,
          Map.of()
        )
      );
  }

  @Test
  public void parseBikeRentalStations() {
    assertTrue(source.update());
    List<VehicleRentalPlace> stations = source.getUpdates();
    assertEquals(40, stations.size());

    VehicleRentalPlace station = stations.get(0);

    assertEquals("BRAHENPUISTO", station.getName().toString());
    //assertEquals("B05", station.getStationId());
    assertEquals(62.887472, station.getLatitude());
    assertEquals(27.687409, station.getLongitude());

    assertEquals(8, station.getVehiclesAvailable());
    assertTrue(station.getSpacesAvailable() > 0);
    assertTrue(station.isAllowDropoff());
    assertTrue(station.isAllowPickup());
    assertTrue(station.allowDropoffNow());

    // ensure that null is not returned
    assertEquals(station.getCapacity(), 0);
  }
}
