package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.site.Station;

/**
 * A vertex representing a station centroid. This can be used as a source/destination for routing.
 */
public class StationCentroidVertex extends Vertex {

  private final Station station;

  public StationCentroidVertex(Station station) {
    super(station.getLon(), station.getLat());
    this.station = station;
  }

  public Station getStation() {
    return this.station;
  }

  @Override
  public I18NString getName() {
    return station.getName();
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.feedScopedId(station.getId());
  }
}
