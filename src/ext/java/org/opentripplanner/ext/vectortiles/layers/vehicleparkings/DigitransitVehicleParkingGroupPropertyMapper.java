package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.TranslatedString;

public class DigitransitVehicleParkingGroupPropertyMapper
  extends PropertyMapper<VehicleParkingAndGroup> {

  public static DigitransitVehicleParkingGroupPropertyMapper create() {
    return new DigitransitVehicleParkingGroupPropertyMapper();
  }

  @Override
  protected Collection<T2<String, Object>> map(VehicleParkingAndGroup parkingAndGroup) {
    var items = new ArrayList<T2<String, Object>>();
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
          // TODO translate name
          parkingObject.put("name", vehicleParkingPlace.getName().toString());
          return parkingObject;
        })
        .toList()
    );
    // TODO translate name
    items.addAll(
      List.of(
        new T2<>("id", group.id().toString()),
        new T2<>("name", group.name().toString()),
        new T2<>("vehicleParking", parking)
      )
    );
    return items;
  }
}
