package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper.getFeedScopedIdAndNetwork;
import static org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper.getNameAndFormFactors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
    var items = new ArrayList<T2<String, Object>>();
    items.addAll(getFeedScopedIdAndNetwork(station));
    items.addAll(getNameAndFormFactors(station, i18NStringMapper));
    items.addAll(
      List.of(
        new T2<>("vehiclesAvailable", station.getVehiclesAvailable()),
        new T2<>("spacesAvailable", station.getSpacesAvailable()),
        new T2<>("operative", station.isAllowPickup() && station.isAllowDropoff())
      )
    );
    return items;
  }
}
