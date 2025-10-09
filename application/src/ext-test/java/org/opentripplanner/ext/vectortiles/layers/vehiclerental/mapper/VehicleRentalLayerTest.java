package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
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
    var vehicle = VehicleRentalVehicle.of()
      .withId(new FeedScopedId("A", "B"))
      .withLatitude(1)
      .withLongitude(2)
      .withName(new NonLocalizedString(NAME))
      .withVehicleType(vehicleType(BICYCLE))
      .build();

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
    var station = VehicleRentalStation.of()
      .withId(new FeedScopedId("A", "B"))
      .withLatitude(1)
      .withLongitude(2)
      .withName(new NonLocalizedString(NAME))
      .withVehicleTypesAvailable(Map.of(vehicleType(BICYCLE), 5, vehicleType(SCOOTER), 10))
      .build();

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
    var germanName = "nameDE";
    var station = VehicleRentalStation.of()
      .withId(new FeedScopedId("A", "B"))
      .withLatitude(1)
      .withLongitude(2)
      .withName(
        TranslatedString.getI18NString(
          new HashMap<>() {
            {
              put(null, NAME);
              put("de", germanName);
            }
          },
          false,
          false
        )
      )
      .withVehicleTypesAvailable(Map.of(vehicleType(BICYCLE), 5, vehicleType(SCOOTER), 10))
      .build();

    Map<String, Object> map = new HashMap<>();
    mapper.map(station).forEach(o -> map.put(o.key(), o.value()));

    assertEquals(germanName, map.get("name"));
  }

  @Test
  public void realtimeStation() {
    var mapper = new DigitransitRealtimeVehicleRentalStationPropertyMapper(new Locale("en-US"));
    var station = VehicleRentalStation.of()
      .withId(new FeedScopedId("A", "B"))
      .withLatitude(1)
      .withLongitude(2)
      .withName(new NonLocalizedString(NAME))
      .withVehicleTypesAvailable(Map.of(vehicleType(BICYCLE), 5, vehicleType(SCOOTER), 10))
      .withIsRenting(false)
      .withIsReturning(false)
      .withVehiclesAvailable(8)
      .withSpacesAvailable(3)
      .build();

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
    return RentalVehicleType.of()
      .withId(new FeedScopedId("1", formFactor.name()))
      .withName(I18NString.of("bicycle"))
      .withFormFactor(formFactor)
      .withPropulsionType(RentalVehicleType.PropulsionType.HUMAN)
      .withMaxRangeMeters(1000d)
      .build();
  }
}
