package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper.getFeedScopedIdAndNetwork;
import static org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper.getNameAndFormFactors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;

public class DigitransitRealtimeVehicleRentalStationPropertyMapper
  extends PropertyMapper<VehicleRentalStation> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitRealtimeVehicleRentalStationPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(VehicleRentalStation station) {
    var items = new ArrayList<KeyValue>();
    items.addAll(getFeedScopedIdAndNetwork(station));
    items.addAll(getNameAndFormFactors(station, i18NStringMapper));
    items.addAll(
      List.of(
        new KeyValue("vehiclesAvailable", station.vehiclesAvailable()),
        new KeyValue("spacesAvailable", station.spacesAvailable()),
        new KeyValue("operative", station.isAllowPickup() && station.isAllowDropoff())
      )
    );
    return items;
  }
}
