package org.opentripplanner.inspector.vector.stop;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A vector tile layer for {@link GroupStop}s inside the vector tile bounds. The builder does not
 * query for the GroupStops to draw, but instead uses the geometries of the GroupStops that are
 * passed to the constructor.
 */
public class GroupStopLayerBuilder extends LayerBuilder<StopLocation> {

  private final List<Geometry> geometries;

  public GroupStopLayerBuilder(
    LayerParameters layerParameters,
    Locale locale,
    Collection<GroupStop> groupStops
  ) {
    super(
      new StopLocationPropertyMapper(locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    // Because there are very few GroupStops with relevant geometries, we can precompute the
    // geometries and store them in a list at the time of construction.
    this.geometries = groupStops
      .stream()
      .filter(groupStop -> groupStop.getEncompassingAreaGeometry().isPresent())
      .map(stop -> {
        Geometry geometry = stop.getEncompassingAreaGeometry().get().copy();
        geometry.setUserData(stop);
        return geometry;
      })
      .toList();
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return geometries;
  }
}
