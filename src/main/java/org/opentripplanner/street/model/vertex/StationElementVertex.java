package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StationElement;

public abstract class StationElementVertex extends Vertex {

  private final VertexLabel label;

  protected StationElementVertex(FeedScopedId id, double x, double y, I18NString name) {
    super(x, y, name);
    this.label = VertexLabel.string(id.toString());
  }

  @Override
  public final VertexLabel getLabel() {
    return label;
  }

  /** Get the corresponding StationElement */
  @Nonnull
  public abstract StationElement getStationElement();
}
