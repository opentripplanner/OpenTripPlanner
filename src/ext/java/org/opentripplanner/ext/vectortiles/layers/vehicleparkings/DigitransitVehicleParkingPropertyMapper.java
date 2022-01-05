package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.simple.JSONObject;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.TranslatedString;

public class DigitransitVehicleParkingPropertyMapper extends PropertyMapper<VehicleParking> {
    public static DigitransitVehicleParkingPropertyMapper create(Graph graph) {
        return new DigitransitVehicleParkingPropertyMapper();
    }

    @Override
    protected Collection<T2<String, Object>> map(VehicleParking vehicleParking) {
        var items = new ArrayList<T2<String, Object>>();
        items.addAll(
                List.of(
                        new T2<>("id", vehicleParking.getId().toString()),
                        new T2<>("realTimeData", vehicleParking.getAvailability() != null),
                        new T2<>("detailsUrl", vehicleParking.getDetailsUrl()),
                        new T2<>("imageUrl", vehicleParking.getImageUrl()),
                        new T2<>("tags", String.join(",", vehicleParking.getTags())),
                        new T2<>("state", vehicleParking.getState().name()),
                        new T2<>("bicyclePlaces", vehicleParking.hasBicyclePlaces()),
                        new T2<>("anyCarPlaces", vehicleParking.hasAnyCarPlaces()),
                        new T2<>("carPlaces", vehicleParking.hasCarPlaces()),
                        new T2<>("wheelchairAccessibleCarPlaces", vehicleParking.hasWheelchairAccessibleCarPlaces()),
                        new T2<>("realTimeData", vehicleParking.hasRealTimeData())
                )
        );
        items.addAll(mapI18NString("name", vehicleParking.getName()));
        items.addAll(mapI18NString("note", vehicleParking.getNote()));
        // TODO add when openingHours are implemented
        // items.addAll(mapI18NString("openingHours", vehicleParking.getOpeningHours()));
        // items.addAll(mapI18NString("feeHours", vehicleParking.getFeeHours()));
        items.addAll(mapPlaces("capacity", vehicleParking.getCapacity()));
        items.addAll(mapPlaces("availability", vehicleParking.getAvailability()));
        items.addAll(mapPlaces("availability", vehicleParking.getAvailability()));
        return items;
    }

    private static List<T2<String, Object>> mapI18NString(String key, Object object) {
        if (object instanceof I18NString) {
            return mapI18NString(key, (I18NString) object);
        } else {
            return List.of();
        }
    }

    private static List<T2<String, Object>> mapI18NString(String key, I18NString i18n) {
        if (i18n == null) {
            return List.of();
        }

        var items = new ArrayList<T2<String, Object>>();
        items.add(new T2<>(key, i18n.toString()));

        if (i18n instanceof TranslatedString) {
            ((TranslatedString) i18n).getTranslations().forEach(
                    e -> {
                        if (e.getKey() != null) {
                            items.add(new T2<>(subKey(key, e.getKey()), e.getValue()));
                        }
                    }
            );
        }

        return items;
    }

    private static List<T2<String, Object>> mapPlaces(String key, VehicleParkingSpaces places) {
        if (places == null) {
            return List.of();
        }

        var json = new JSONObject();
        json.put("bicyclePlaces", places.getBicycleSpaces());
        json.put("carPlaces", places.getCarSpaces());
        json.put("wheelchairAccessibleCarPlaces", places.getWheelchairAccessibleCarSpaces());

        return List.of(
                new T2<>(key, JSONObject.toJSONString(json)),
                new T2<>(subKey(key, "bicyclePlaces"), places.getBicycleSpaces()),
                new T2<>(subKey(key, "carPlaces"), places.getCarSpaces()),
                new T2<>(subKey(key, "wheelchairAccessibleCarPlaces"), places.getWheelchairAccessibleCarSpaces())
        );
    }

    private static String subKey(String key, String subkey) {
        return String.format("%s.%s", key, subkey);
    }
}
