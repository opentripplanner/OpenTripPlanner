package org.opentripplanner.inspector.vector;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitService;

public class AreaStopsLayerBuilder extends LayerBuilder<AreaStop> {

  static Map<MapperType, BiFunction<TransitService, Locale, PropertyMapper<AreaStop>>> mappers = Map.of(
    MapperType.DebugClient,
    DebugClientAreaStopPropertyMapper::create
  );
  private final Function<Envelope, Collection<AreaStop>> findAreaStops;

  public AreaStopsLayerBuilder(
    TransitService transitService,
    VectorTilesResource.LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      layerParameters.name(),
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitService, locale),
      layerParameters.expansionFactor()
    );
    this.findAreaStops = transitService::findAreaStops;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return findAreaStops
      .apply(query)
      .stream()
      .map(areaStop -> {
        Geometry geometry = areaStop.getGeometry().copy();

        geometry.setUserData(areaStop);

        return geometry;
      })
      .toList();
  }

  enum MapperType {
    DebugClient,
  }
}
