package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.I18NStringMapper;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

public class DigitransitRealtimeVehicleRentalStationPropertyMapper
  extends PropertyMapper<VehicleRentalStation> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitRealtimeVehicleRentalStationPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalStation station) {
    return List.of(
      new T2<>("id", station.getId().toString()),
      new T2<>("name", i18NStringMapper.mapToApi(station.getName())),
      new T2<>("network", station.getNetwork()),
      new T2<>("vehiclesAvailable", station.getVehiclesAvailable()),
      new T2<>("spacesAvailable", station.getSpacesAvailable()),
      new T2<>("operative", station.isAllowPickup() && station.isAllowDropoff()),
      // a station can potentially have multiple form factors that's why this is plural
      new T2<>(
        "formFactors",
        station.formFactors().stream().map(Enum::name).sorted().collect(Collectors.joining(","))
      )
    );
  }
}
