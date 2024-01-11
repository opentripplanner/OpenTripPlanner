package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;

public class DigitransitVehicleParkingGroupPropertyMapper
  extends PropertyMapper<VehicleParkingAndGroup> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitVehicleParkingGroupPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  public static DigitransitVehicleParkingGroupPropertyMapper create(Locale locale) {
    return new DigitransitVehicleParkingGroupPropertyMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(VehicleParkingAndGroup parkingAndGroup) {
    var group = parkingAndGroup.vehicleParkingGroup();
    String parking = JSONArray.toJSONString(
      parkingAndGroup
        .vehicleParking()
        .stream()
        .map(vehicleParkingPlace -> {
          JSONObject parkingObject = new JSONObject();
          parkingObject.put("carPlaces", vehicleParkingPlace.hasCarPlaces());
          parkingObject.put("bicyclePlaces", vehicleParkingPlace.hasBicyclePlaces());
          parkingObject.put("id", vehicleParkingPlace.getId().toString());
          parkingObject.put("name", i18NStringMapper.mapToApi(vehicleParkingPlace.getName()));
          return parkingObject;
        })
        .toList()
    );
    return List.of(
      new KeyValue("id", group.id().toString()),
      new KeyValue("name", i18NStringMapper.mapToApi(group.name())),
      new KeyValue("vehicleParking", parking)
    );
  }
}
