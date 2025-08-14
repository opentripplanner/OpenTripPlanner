package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;

public class DigitransitVehicleRentalStationPropertyMapper
  extends PropertyMapper<VehicleRentalStation> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitVehicleRentalStationPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(VehicleRentalStation station) {
    var items = new ArrayList<KeyValue>();
    items.addAll(getFeedScopedIdAndNetwork(station));
    items.addAll(getNameAndFormFactors(station, i18NStringMapper));
    return items;
  }

  protected static List<KeyValue> getFeedScopedIdAndNetwork(VehicleRentalPlace vehicleRentalPlace) {
    return List.of(
      new KeyValue("id", vehicleRentalPlace.id().toString()),
      new KeyValue("network", vehicleRentalPlace.network())
    );
  }

  protected static List<KeyValue> getNameAndFormFactors(
    VehicleRentalStation vehicleRentalStation,
    I18NStringMapper i18NStringMapper
  ) {
    return List.of(
      new KeyValue("name", i18NStringMapper.mapToApi(vehicleRentalStation.name())),
      // a station can potentially have multiple form factors that's why this is plural
      new KeyValue(
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
