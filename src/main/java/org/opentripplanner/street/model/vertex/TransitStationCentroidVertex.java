package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.site.Station;

public class TransitStationCentroidVertex extends Vertex {

  private final Station station;

  public TransitStationCentroidVertex(Station station) {
    super(station.getLon(), station.getLat());
    this.station = station;
  }

  public Station getStation() {
    return this.station;
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return station.getName();
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.feedScopedId(station.getId());
  }
}
