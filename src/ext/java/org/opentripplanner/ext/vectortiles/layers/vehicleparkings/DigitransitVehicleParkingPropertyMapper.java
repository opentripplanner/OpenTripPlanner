package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.I18NStringMapper;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class DigitransitVehicleParkingPropertyMapper extends PropertyMapper<VehicleParking> {

  private final I18NStringMapper i18NStringMapper;

  private DigitransitVehicleParkingPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  protected static DigitransitVehicleParkingPropertyMapper create(Locale locale) {
    return new DigitransitVehicleParkingPropertyMapper(locale);
  }

  @Override
  protected Collection<T2<String, Object>> map(VehicleParking vehicleParking) {
    var items = new ArrayList<T2<String, Object>>();
    items.addAll(
      List.of(
        new T2<>("id", vehicleParking.getId().toString()),
        new T2<>("bicyclePlaces", vehicleParking.hasBicyclePlaces()),
        new T2<>("anyCarPlaces", vehicleParking.hasAnyCarPlaces()),
        new T2<>("carPlaces", vehicleParking.hasCarPlaces()),
        new T2<>(
          "wheelchairAccessibleCarPlaces",
          vehicleParking.hasWheelchairAccessibleCarPlaces()
        ),
        new T2<>("name", i18NStringMapper.mapToApi(vehicleParking.getName()))
      )
    );
    return items;
  }
}
