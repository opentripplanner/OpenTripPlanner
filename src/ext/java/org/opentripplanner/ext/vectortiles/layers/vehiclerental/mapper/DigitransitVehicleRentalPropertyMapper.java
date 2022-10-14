package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

public class DigitransitVehicleRentalPropertyMapper extends PropertyMapper<VehicleRentalPlace> {

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalPlace place) {
    return List.of(
      new T2<>("id", place.getStationId()),
      // getName() returns an instance of I18NString which the vector tiles code cannot easily convert.
      // https://github.com/wdtinc/mapbox-vector-tile-java/blob/81d2ea92fe255eab5c1005ec86c8a9160fdf44dd/src/main/java/com/wdtinc/mapbox_vector_tile/encoding/MvtValue.java#L83-L91
      // in order to prevent it being silently dropped we convert to string here.
      // not sure if we should take the possibility of translated names into account and add them
      // to the response somehow.
      new T2<>("name", place.getName().toString()),
      // this is plural since once upon a time OSM-added rental stations could have multiple stations
      new T2<>("networks", place.getNetwork())
    );
  }
}
