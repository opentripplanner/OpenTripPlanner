package org.opentripplanner.ext.vectortiles.layers.stops;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class StopsLayerBuilder extends LayerBuilder<RegularStop> {

  static Map<MapperType, BiFunction<TransitService, Locale, PropertyMapper<RegularStop>>> mappers = Map.of(
    MapperType.Digitransit,
    DigitransitStopPropertyMapper::create
  );
  private final TransitService transitService;

  public StopsLayerBuilder(
    TransitService transitService,
    VectorTilesResource.LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      layerParameters.name(),
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitService, locale)
    );
    this.transitService = transitService;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return transitService
      .findRegularStop(query)
      .stream()
      .map(stop -> {
        Geometry point = stop.getGeometry();

        point.setUserData(stop);

        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
  }
}
