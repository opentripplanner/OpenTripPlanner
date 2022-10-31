package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.I18NStringMapper;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

public class DigitransitVehicleRentalPropertyMapper extends PropertyMapper<VehicleRentalPlace> {

  private final I18NStringMapper i18NStringMapper;

  public DigitransitVehicleRentalPropertyMapper(Locale locale) {
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalPlace place) {
    return List.of(
      new T2<>("id", place.getStationId()),
      // to the response somehow.
      new T2<>("name", i18NStringMapper.mapToApi(place.getName())),
      // this is plural since once upon a time OSM-added rental stations could have multiple stations
      new T2<>("networks", place.getNetwork())
    );
  }
}
