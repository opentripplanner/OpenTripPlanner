package org.opentripplanner.ext.vectortiles.layers.stops;

import static java.util.Map.entry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.ext.vectortiles.layers.LayerFilters;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class StopsLayerBuilder extends LayerBuilder<RegularStop> {

  private final TransitService transitService;
  private final Predicate<RegularStop> filter;

  public StopsLayerBuilder(
    TransitService transitService,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      Map.ofEntries(
        entry(MapperType.Digitransit, new DigitransitStopPropertyMapper(transitService, locale)),
        entry(
          MapperType.DigitransitRealtime,
          new DigitransitRealtimeStopPropertyMapper(transitService, locale)
        )
      ).get(MapperType.valueOf(layerParameters.mapper())),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.transitService = transitService;
    this.filter = LayerFilters.forType(layerParameters.filterType(), transitService);
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return transitService
      .findRegularStopsByBoundingBox(query)
      .stream()
      .filter(filter)
      .map(stop -> {
        Geometry point = stop.getGeometry();

        point.setUserData(stop);

        return point;
      })
      .toList();
  }

  enum MapperType {
    Digitransit,
    DigitransitRealtime,
  }
}
