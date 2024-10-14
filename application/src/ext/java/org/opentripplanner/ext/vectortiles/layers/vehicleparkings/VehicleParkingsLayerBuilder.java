package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import static java.util.Map.entry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class VehicleParkingsLayerBuilder extends LayerBuilder<VehicleParking> {

  static Map<VehicleParkingsLayerBuilder.MapperType, Function<Locale, PropertyMapper<VehicleParking>>> mappers = Map.ofEntries(
    entry(
      VehicleParkingsLayerBuilder.MapperType.Stadtnavi,
      StadtnaviVehicleParkingPropertyMapper::create
    ),
    entry(MapperType.Digitransit, DigitransitVehicleParkingPropertyMapper::create)
  );
  private final Graph graph;

  public VehicleParkingsLayerBuilder(
    Graph graph,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return graph
      .getVehicleParkingService()
      .getVehicleParkings()
      .map(vehicleParking -> {
        Coordinate coordinate = vehicleParking.getCoordinate().asJtsCoordinate();
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(vehicleParking);
        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
    Stadtnavi,
  }
}
