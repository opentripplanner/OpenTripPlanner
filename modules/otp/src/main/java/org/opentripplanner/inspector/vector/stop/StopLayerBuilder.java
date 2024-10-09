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
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A vector tile layer for {@link StopLocation}s inside the vector tile bounds. These can be further
 * filtered to get only a subset of stop implementations like {@link RegularStop}
 * or {@link AreaStop}.
 */
public class StopLayerBuilder<T extends StopLocation> extends LayerBuilder<StopLocation> {

  private final Function<Envelope, Collection<T>> findStops;

  public StopLayerBuilder(
    LayerParameters layerParameters,
    Locale locale,
    Function<Envelope, Collection<T>> findStops
  ) {
    super(
      new StopLocationPropertyMapper(locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.findStops = findStops;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return findStops
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
