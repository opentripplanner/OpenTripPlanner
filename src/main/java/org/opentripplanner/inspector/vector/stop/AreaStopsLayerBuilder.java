package org.opentripplanner.inspector.vector.stop;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

/**
 * A vector tile layer containing all {@link AreaStop}s inside the vector tile bounds.
 */
public class AreaStopsLayerBuilder extends LayerBuilder<StopLocation> {

  private final Function<Envelope, Collection<AreaStop>> findAreaStops;

  public AreaStopsLayerBuilder(
    TransitService transitService,
    LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      new StopLocationPropertyMapper(locale),
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
}
