package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

public class DigitransitVehicleRentalStationPropertyMapper
  extends PropertyMapper<VehicleRentalStation> {

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalStation station) {
    return List.of(
      new T2<>("id", station.getId().toString()),
      // getName() returns an instance of I18NString which the vector tiles code cannot easily convert.
      // https://github.com/wdtinc/mapbox-vector-tile-java/blob/81d2ea92fe255eab5c1005ec86c8a9160fdf44dd/src/main/java/com/wdtinc/mapbox_vector_tile/encoding/MvtValue.java#L83-L91
      // in order to prevent it being silently dropped we convert to string here.
      // not sure if we should take the possibility of translated names into account and add them
      // to the response somehow.
      new T2<>("name", station.getName().toString()),
      new T2<>("network", station.getNetwork()),
      // a station can potentially have multiple form factors that's why this is plural
      new T2<>(
        "formFactors",
        station.formFactors().stream().map(Enum::name).sorted().collect(Collectors.joining(","))
      )
    );
  }
}
