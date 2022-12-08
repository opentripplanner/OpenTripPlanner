package org.opentripplanner.inspector.vector;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitService;

/**
 * A vector tile layer containing all {@link AreaStop}s inside the vector tile bounds.
 */
public class AreaStopsLayerBuilder extends LayerBuilder<AreaStop> {

  private static final Map<MapperType, MapperFactory> mappers = Map.of(
    MapperType.DebugClient,
    DebugClientAreaStopPropertyMapper::create
  );
  private final Function<Envelope, Collection<AreaStop>> findAreaStops;

  public AreaStopsLayerBuilder(
    TransitService transitService,
    LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())).build(transitService, locale),
      layerParameters.name(),
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

  @FunctionalInterface
  private interface MapperFactory {
    PropertyMapper<AreaStop> build(TransitService transitService, Locale locale);
  }
}
