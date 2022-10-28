package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.I18NStringMapper;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

public class DigitransitVehicleRentalStationPropertyMapper
  extends PropertyMapper<VehicleRentalStation> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitVehicleRentalStationPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalStation station) {
    var items = new ArrayList<T2<String, Object>>();
    items.addAll(getFeedScopedIdAndNetwork(station));
    items.addAll(getNameAndFormFactors(station, i18NStringMapper));
    return items;
  }

  protected static List<T2<String, Object>> getFeedScopedIdAndNetwork(
    VehicleRentalPlace vehicleRentalPlace
  ) {
    return List.of(
      new T2<>("id", vehicleRentalPlace.getId().toString()),
      new T2<>("network", vehicleRentalPlace.getNetwork())
    );
  }

  protected static List<T2<String, Object>> getNameAndFormFactors(
    VehicleRentalStation vehicleRentalStation,
    I18NStringMapper i18NStringMapper
  ) {
    return List.of(
      new T2<>("name", i18NStringMapper.mapToApi(vehicleRentalStation.getName())),
      // a station can potentially have multiple form factors that's why this is plural
      new T2<>(
        "formFactors",
        vehicleRentalStation
          .formFactors()
          .stream()
          .map(Enum::name)
          .sorted()
          .collect(Collectors.joining(","))
      )
    );
  }
}
