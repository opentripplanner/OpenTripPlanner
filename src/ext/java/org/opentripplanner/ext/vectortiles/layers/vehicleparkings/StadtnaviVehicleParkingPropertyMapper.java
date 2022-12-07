package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.json.simple.JSONObject;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;

public class StadtnaviVehicleParkingPropertyMapper extends PropertyMapper<VehicleParking> {

  protected static StadtnaviVehicleParkingPropertyMapper create(Locale locale) {
    return new StadtnaviVehicleParkingPropertyMapper();
  }

  @Override
  protected Collection<KeyValue> map(VehicleParking vehicleParking) {
    var items = new ArrayList<KeyValue>();
    items.addAll(
      List.of(
        new KeyValue("id", vehicleParking.getId().toString()),
        new KeyValue("realTimeData", vehicleParking.getAvailability() != null),
        new KeyValue("detailsUrl", vehicleParking.getDetailsUrl()),
        new KeyValue("imageUrl", vehicleParking.getImageUrl()),
        new KeyValue("tags", String.join(",", vehicleParking.getTags())),
        new KeyValue("state", vehicleParking.getState().name()),
        new KeyValue("bicyclePlaces", vehicleParking.hasBicyclePlaces()),
        new KeyValue("anyCarPlaces", vehicleParking.hasAnyCarPlaces()),
        new KeyValue("carPlaces", vehicleParking.hasCarPlaces()),
        new KeyValue(
          "wheelchairAccessibleCarPlaces",
          vehicleParking.hasWheelchairAccessibleCarPlaces()
        ),
        new KeyValue("realTimeData", vehicleParking.hasRealTimeData())
      )
    );
    items.addAll(mapI18NString("name", vehicleParking.getName()));
    items.addAll(mapI18NString("note", vehicleParking.getNote()));
    // TODO add when openingHours are implemented
    // items.addAll(mapI18NString("openingHours", vehicleParking.getOpeningHours()));
    // items.addAll(mapI18NString("feeHours", vehicleParking.getFeeHours()));
    items.addAll(mapPlaces("capacity", vehicleParking.getCapacity()));
    items.addAll(mapPlaces("availability", vehicleParking.getAvailability()));
    return items;
  }

  private static List<KeyValue> mapI18NString(String key, Object object) {
    if (object instanceof I18NString) {
      return mapI18NString(key, (I18NString) object);
    } else {
      return List.of();
    }
  }

  private static List<KeyValue> mapI18NString(String key, I18NString i18n) {
    if (i18n == null) {
      return List.of();
    }

    var items = new ArrayList<KeyValue>();
    items.add(new KeyValue(key, i18n.toString()));

    if (i18n instanceof TranslatedString) {
      ((TranslatedString) i18n).getTranslations()
        .forEach(e -> {
          if (e.getKey() != null) {
            items.add(new KeyValue(subKey(key, e.getKey()), e.getValue()));
          }
        });
    }

    return items;
  }

  private static List<KeyValue> mapPlaces(String key, VehicleParkingSpaces places) {
    if (places == null) {
      return List.of();
    }

    var json = new JSONObject();
    json.put("bicyclePlaces", places.getBicycleSpaces());
    json.put("carPlaces", places.getCarSpaces());
    json.put("wheelchairAccessibleCarPlaces", places.getWheelchairAccessibleCarSpaces());

    return List.of(
      new KeyValue(key, JSONObject.toJSONString(json)),
      new KeyValue(subKey(key, "bicyclePlaces"), places.getBicycleSpaces()),
      new KeyValue(subKey(key, "carPlaces"), places.getCarSpaces()),
      new KeyValue(
        subKey(key, "wheelchairAccessibleCarPlaces"),
        places.getWheelchairAccessibleCarSpaces()
      )
    );
  }

  private static String subKey(String key, String subkey) {
    return String.format("%s.%s", key, subkey);
  }
}
