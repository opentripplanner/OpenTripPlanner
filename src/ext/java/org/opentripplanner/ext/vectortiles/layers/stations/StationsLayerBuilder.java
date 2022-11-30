package org.opentripplanner.ext.vectortiles.layers.stations;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.TransitService;

public class StationsLayerBuilder extends LayerBuilder<Station> {

  static Map<MapperType, BiFunction<TransitService, Locale, PropertyMapper<Station>>> mappers = Map.of(
    MapperType.Digitransit,
    DigitransitStationPropertyMapper::create
  );
  private final TransitService transitModel;

  public StationsLayerBuilder(
    TransitService transitService,
    VectorTilesResource.LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      layerParameters.name(),
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitService, locale)
    );
    this.transitModel = transitService;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return transitModel
      .getStations()
      .stream()
      .map(station -> {
        Coordinate coordinate = station.getCoordinate().asJtsCoordinate();
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(station);
        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
  }
}
