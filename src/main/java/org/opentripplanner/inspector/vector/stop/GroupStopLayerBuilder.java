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
    this.geometries =
      groupStops
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
