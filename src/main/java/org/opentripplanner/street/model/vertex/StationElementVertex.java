package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StationElement;

public abstract class StationElementVertex extends Vertex {

  private final FeedScopedId id;

  protected StationElementVertex(FeedScopedId id, double x, double y, I18NString name) {
    super(x, y, name);
    this.id = id;
  }

  @Override
  public final VertexLabel getLabel() {
    return VertexLabel.feedScopedId(id);
  }

  /** Get the corresponding StationElement */
  @Nonnull
  public abstract StationElement getStationElement();
}
