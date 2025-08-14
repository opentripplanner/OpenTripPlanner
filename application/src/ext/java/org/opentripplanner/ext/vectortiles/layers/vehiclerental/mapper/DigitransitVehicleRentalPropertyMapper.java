package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;

public class DigitransitVehicleRentalPropertyMapper extends PropertyMapper<VehicleRentalPlace> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitVehicleRentalPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<KeyValue> map(VehicleRentalPlace place) {
    return List.of(
      new KeyValue("id", place.stationId()),
      // to the response somehow.
      new KeyValue("name", i18NStringMapper.mapToApi(place.name())),
      // this is plural since once upon a time OSM-added rental stations could have multiple stations
      new KeyValue("networks", place.network())
    );
  }
}
