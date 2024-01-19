package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.json.simple.JSONObject;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.model.calendar.openinghours.OsmOpeningHoursSupport;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;

public class StadtnaviVehicleParkingPropertyMapper extends PropertyMapper<VehicleParking> {

  private final DigitransitVehicleParkingPropertyMapper digitransitMapper;
  private final I18NStringMapper i18NStringMapper;

  public StadtnaviVehicleParkingPropertyMapper(Locale locale) {
    i18NStringMapper = new I18NStringMapper(locale);
    digitransitMapper = DigitransitVehicleParkingPropertyMapper.create(locale);
  }

  protected static StadtnaviVehicleParkingPropertyMapper create(Locale locale) {
    return new StadtnaviVehicleParkingPropertyMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(VehicleParking vehicleParking) {
    var items = digitransitMapper.basicMapping(vehicleParking);
    items.addAll(
      List.of(
        new KeyValue("realTimeData", vehicleParking.getAvailability() != null),
        new KeyValue("detailsUrl", vehicleParking.getDetailsUrl()),
        new KeyValue("imageUrl", vehicleParking.getImageUrl()),
        new KeyValue("tags", String.join(",", vehicleParking.getTags())),
        new KeyValue("state", vehicleParking.getState().name()),
        new KeyValue("realTimeData", vehicleParking.hasRealTimeData()),
        new KeyValue("note", i18NStringMapper.mapToApi(vehicleParking.getNote()))
      )
    );
    if (vehicleParking.getOpeningHours() != null) {
      items.add(
        new KeyValue(
          "openingHours",
          OsmOpeningHoursSupport.osmFormat(vehicleParking.getOpeningHours())
        )
      );
    }
    items.addAll(mapPlaces("capacity", vehicleParking.getCapacity()));
    items.addAll(mapPlaces("availability", vehicleParking.getAvailability()));
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
