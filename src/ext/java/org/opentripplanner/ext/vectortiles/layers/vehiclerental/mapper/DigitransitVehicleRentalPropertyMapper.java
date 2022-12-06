package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.api.mapping.I18NStringMapper;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

public class DigitransitVehicleRentalPropertyMapper extends PropertyMapper<VehicleRentalPlace> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitVehicleRentalPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(VehicleRentalPlace place) {
    return List.of(
      new KeyValue("id", place.getStationId()),
      // to the response somehow.
      new KeyValue("name", i18NStringMapper.mapToApi(place.getName())),
      // this is plural since once upon a time OSM-added rental stations could have multiple stations
      new KeyValue("networks", place.getNetwork())
    );
  }
}
