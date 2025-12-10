package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.service.vehiclerental.model.RentalVehicleTypeFactory.vehicleType;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.i18n.TranslatedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;

class DigitransitVehicleRentalStationPropertyMapperTest {

  private static final String NAME = "a rental";

  @Test
  void station() {
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
  void stationWithTranslations() {
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
  void realtimeStation() {
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
}
