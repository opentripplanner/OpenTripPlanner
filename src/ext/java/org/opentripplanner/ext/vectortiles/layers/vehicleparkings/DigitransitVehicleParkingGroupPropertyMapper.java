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
          parkingObject.putAll(mapI18NString("name", vehicleParkingPlace.getName()));
          return parkingObject;
        })
        .toList()
    );
    items.addAll(
      List.of(new T2<>("id", group.id().toString()), new T2<>("vehicleParking", parking))
    );
    items.addAll(listI18NString("name", group.name()));
    return items;
  }

  private static List<T2<String, Object>> listI18NString(String key, I18NString i18n) {
    if (i18n == null) {
      return List.of();
    }

    var items = new ArrayList<T2<String, Object>>();
    items.add(new T2<>(key, i18n.toString()));

    if (i18n instanceof TranslatedString) {
      ((TranslatedString) i18n).getTranslations()
        .forEach(e -> {
          if (e.getKey() != null) {
            items.add(new T2<>(subKey(key, e.getKey()), e.getValue()));
          }
        });
    }

    return items;
  }

  private static Map<String, String> mapI18NString(String key, I18NString i18n) {
    if (i18n == null) {
      return Map.of();
    }

    var items = new HashMap<String, String>();
    items.put(key, i18n.toString());

    if (i18n instanceof TranslatedString) {
      ((TranslatedString) i18n).getTranslations()
        .forEach(e -> {
          if (e.getKey() != null) {
            items.put(subKey(key, e.getKey()), e.getValue());
          }
        });
    }

    return items;
  }

  private static String subKey(String key, String subkey) {
    return String.format("%s.%s", key, subkey);
  }
}
