package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleRentalLayerTest {

  public static final String NAME = "a rental";

  @Test
  public void floatingVehicle() {
    var mapper = new DigitransitRentalVehiclePropertyMapper();
    var vehicle = new VehicleRentalVehicle();
    vehicle.id = new FeedScopedId("A", "B");
    vehicle.latitude = 1;
    vehicle.longitude = 2;
    vehicle.name = new NonLocalizedString(NAME);
    vehicle.vehicleType = vehicleType(BICYCLE);

    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicle).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("A:B", map.get("id"));
    assertEquals("BICYCLE", map.get("formFactor"));
    assertEquals("A", map.get("network"));
    assertEquals(true, map.get("pickupAllowed"));
    assertNull(map.get("name"));
  }

  @Test
  public void station() {
    var mapper = new DigitransitVehicleRentalStationPropertyMapper(new Locale("en-US"));
    var station = new VehicleRentalStation();
    station.id = new FeedScopedId("A", "B");
    station.latitude = 1;
    station.longitude = 2;
    station.name = new NonLocalizedString(NAME);
    station.vehicleTypesAvailable = Map.of(vehicleType(BICYCLE), 5, vehicleType(SCOOTER), 10);

    Map<String, Object> map = new HashMap<>();
    mapper.map(station).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("A:B", map.get("id"));
    assertEquals("BICYCLE,SCOOTER", map.get("formFactors"));
    assertEquals(NAME, map.get("name"));
    assertEquals("A", map.get("network"));
  }

  @Test
  public void stationWithTranslations() {
    var mapper = new DigitransitVehicleRentalStationPropertyMapper(new Locale("de"));
    var station = new VehicleRentalStation();
    station.id = new FeedScopedId("A", "B");
    station.latitude = 1;
    station.longitude = 2;
    var germanName = "nameDE";
    station.name = TranslatedString.getI18NString(
      new HashMap<>() {
        {
          put(null, NAME);
          put("de", germanName);
        }
      },
      false,
      false
    );
    station.vehicleTypesAvailable = Map.of(vehicleType(BICYCLE), 5, vehicleType(SCOOTER), 10);

    Map<String, Object> map = new HashMap<>();
    mapper.map(station).forEach(o -> map.put(o.key(), o.value()));

    assertEquals(germanName, map.get("name"));
  }

  @Test
  public void realtimeStation() {
    var mapper = new DigitransitRealtimeVehicleRentalStationPropertyMapper(new Locale("en-US"));
    var station = new VehicleRentalStation();
    station.id = new FeedScopedId("A", "B");
    station.latitude = 1;
    station.longitude = 2;
    station.name = new NonLocalizedString(NAME);
    station.vehicleTypesAvailable = Map.of(vehicleType(BICYCLE), 5, vehicleType(SCOOTER), 10);
    station.isRenting = false;
    station.isReturning = false;
    station.vehiclesAvailable = 8;
    station.spacesAvailable = 3;

    Map<String, Object> map = new HashMap<>();
    mapper.map(station).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("A:B", map.get("id"));
    assertEquals("BICYCLE,SCOOTER", map.get("formFactors"));
    assertEquals(NAME, map.get("name"));
    assertEquals("A", map.get("network"));
    assertEquals(false, map.get("operative"));
    assertEquals(8, map.get("vehiclesAvailable"));
    assertEquals(3, map.get("spacesAvailable"));
  }

  private static RentalVehicleType vehicleType(RentalFormFactor formFactor) {
    return new RentalVehicleType(
      new FeedScopedId("1", formFactor.name()),
      "bicycle",
      formFactor,
      RentalVehicleType.PropulsionType.HUMAN,
      1000d
    );
  }
}
