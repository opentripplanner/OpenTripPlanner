package org.opentripplanner.ext.vectortiles.layers.stops;

import static java.util.Map.entry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class StopsLayerBuilder<T> extends LayerBuilder<T> {

  static Map<MapperType, BiFunction<TransitService, Locale, PropertyMapper<RegularStop>>> mappers = Map.of(
    MapperType.Digitransit,
    DigitransitStopPropertyMapper::create
  );
  private final TransitService transitService;

  public StopsLayerBuilder(
    TransitService transitService,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      (PropertyMapper<T>) Map
        .ofEntries(
          entry(MapperType.Digitransit, new DigitransitStopPropertyMapper(transitService, locale)),
          entry(
            MapperType.DigitransitRealtime,
            new DigitransitRealtimeStopPropertyMapper(transitService, locale)
          )
        )
        .get(MapperType.valueOf(layerParameters.mapper())),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.transitService = transitService;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return transitService
      .findRegularStops(query)
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
    DigitransitRealtime,
  }
}
