package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.inspector.vector.KeyValue;

public class DigitransitVehicleParkingGroupPropertyMapper
  extends PropertyMapper<VehicleParkingAndGroup> {

  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
  private final I18NStringMapper i18NStringMapper;

  public DigitransitVehicleParkingGroupPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  public static DigitransitVehicleParkingGroupPropertyMapper create(Locale locale) {
    return new DigitransitVehicleParkingGroupPropertyMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(VehicleParkingAndGroup parkingAndGroup) {
    try {
      var group = parkingAndGroup.vehicleParkingGroup();
      var lots = parkingAndGroup
        .vehicleParking()
        .stream()
        .map(vehicleParkingPlace -> {
          var parkingObject = OBJECT_MAPPER.createObjectNode();
          parkingObject.put("carPlaces", vehicleParkingPlace.hasCarPlaces());
          parkingObject.put("bicyclePlaces", vehicleParkingPlace.hasBicyclePlaces());
          parkingObject.put("id", vehicleParkingPlace.getId().toString());
          parkingObject.put("name", i18NStringMapper.mapToApi(vehicleParkingPlace.getName()));
          return parkingObject;
        })
        .toList();
      var string = OBJECT_MAPPER.writeValueAsString(lots);
      return List.of(
        new KeyValue("id", group.id().toString()),
        new KeyValue("name", i18NStringMapper.mapToApi(group.name())),
        new KeyValue("vehicleParking", string)
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
