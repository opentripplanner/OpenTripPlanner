package org.opentripplanner.ext.vectortiles.layers.bikerental;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;

import java.util.Collection;
import java.util.List;

public class DigitransitBikeRentalPropertyMapper extends PropertyMapper<BikeRentalStation> {
  public static DigitransitBikeRentalPropertyMapper create(Graph graph) {
    return new DigitransitBikeRentalPropertyMapper();
  }

  @Override
  protected Collection<T2<String, Object>> map(BikeRentalStation station) {
    return List.of(
        new T2<>("id", station.id),
        new T2<>("name", station.name),
        new T2<>("networks", String.join(",", station.networks))
    );
  }
}
