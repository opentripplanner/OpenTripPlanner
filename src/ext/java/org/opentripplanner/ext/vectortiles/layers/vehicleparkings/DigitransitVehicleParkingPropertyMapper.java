package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
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
  protected Collection<KeyValue> map(VehicleParking vehicleParking) {
    return basicMapping(vehicleParking);
  }

  @Nonnull
  protected ArrayList<KeyValue> basicMapping(VehicleParking vehicleParking) {
    return new ArrayList<>(
      List.of(
        new KeyValue("id", vehicleParking.getId().toString()),
        new KeyValue("bicyclePlaces", vehicleParking.hasBicyclePlaces()),
        new KeyValue("anyCarPlaces", vehicleParking.hasAnyCarPlaces()),
        new KeyValue("carPlaces", vehicleParking.hasCarPlaces()),
        new KeyValue(
          "wheelchairAccessibleCarPlaces",
          vehicleParking.hasWheelchairAccessibleCarPlaces()
        ),
        new KeyValue("name", i18NStringMapper.mapToApi(vehicleParking.getName()))
      )
    );
  }
}
