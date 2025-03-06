package org.opentripplanner.ext.vectortiles.layers.areastops;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitService;

public class AreaStopsLayerBuilder extends LayerBuilder<AreaStop> {

  static Map<MapperType, BiFunction<TransitService, Locale, PropertyMapper<AreaStop>>> mappers =
    Map.of(MapperType.OTPRR, AreaStopPropertyMapper::create);
  private final TransitService transitService;

  public AreaStopsLayerBuilder(
    TransitService transitService,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitService, locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.transitService = transitService;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return transitService
      .findAreaStops(query)
      .stream()
      .filter(g -> g.getGeometry() != null)
      .map(stop -> {
        Geometry point = stop.getGeometry().copy();
        point.setUserData(stop);
        return point;
      })
      .toList();
  }

  enum MapperType {
    OTPRR,
  }
}
