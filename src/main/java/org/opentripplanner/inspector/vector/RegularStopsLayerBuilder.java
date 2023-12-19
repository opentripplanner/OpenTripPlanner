package org.opentripplanner.inspector.vector;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

/**
 * A vector tile layer containing all {@link RegularStop}s inside the vector tile bounds.
 */
public class RegularStopsLayerBuilder extends LayerBuilder<StopLocation> {

  private final Function<Envelope, Collection<RegularStop>> findAreaStops;

  public RegularStopsLayerBuilder(
    TransitService transitService,
    LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      new DebugClientAreaStopPropertyMapper(locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.findAreaStops = transitService::findRegularStop;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return findAreaStops
      .apply(query)
      .stream()
      .map(stop -> {
        Geometry geometry = stop.getGeometry().copy();
        geometry.setUserData(stop);
        return geometry;
      })
      .toList();
  }

}
