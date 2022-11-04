package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.I18NStringMapper;
import org.opentripplanner.ext.vectortiles.PropertyMapper;

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
  protected Collection<T2<String, Object>> map(VehicleParkingAndGroup parkingAndGroup) {
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
      new T2<>("id", group.id().toString()),
      new T2<>("name", i18NStringMapper.mapToApi(group.name())),
      new T2<>("vehicleParking", parking)
    );
  }
}
