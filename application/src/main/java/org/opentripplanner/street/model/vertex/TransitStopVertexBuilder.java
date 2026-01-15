package org.opentripplanner.street.model.vertex;

import java.util.Set;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;

public class TransitStopVertexBuilder {

  private Point coordinate;
  private Set<TransitMode> modes = Set.of();
  private FeedScopedId id;
  private Accessibility wheelchairAccessibility = Accessibility.NO_INFORMATION;

  /**
   * Protected access to avoid instantiation, use
   * {@link org.opentripplanner.street.model.vertex.TransitStopVertex#of()} method instead.
   */
  TransitStopVertexBuilder() {}

  public TransitStopVertexBuilder withId(FeedScopedId id) {
    this.id = id;
    return this;
  }

  public TransitStopVertexBuilder withPoint(Point coordinates) {
    this.coordinate = coordinates;
    return this;
  }

  public TransitStopVertexBuilder withWheelchairAccessiblity(Accessibility accessiblity) {
    this.wheelchairAccessibility = accessiblity;
    return this;
  }

  public TransitStopVertexBuilder withModes(Set<TransitMode> modes) {
    this.modes = modes;
    return this;
  }

  public TransitStopVertex build() {
    return new TransitStopVertex(id, new WgsCoordinate(coordinate), wheelchairAccessibility, modes);
  }
}
