package org.opentripplanner.street.model.vertex;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;

public final class TemporaryStreetLocation extends StreetLocation implements TemporaryVertex {

  private final boolean endVertex;

  public TemporaryStreetLocation(
    String id,
    Coordinate nearestPoint,
    I18NString name,
    boolean endVertex
  ) {
    super(id, nearestPoint, name);
    this.endVertex = endVertex;
  }

  @Override
  public void addOutgoing(Edge edge) {
    if (edge instanceof TemporaryEdge) {
      if (endVertex) {
        throw new UnsupportedOperationException("Can't add outgoing edge to end vertex");
      } else {
        super.addOutgoing(edge);
      }
    } else {
      throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
    }
  }

  @Override
  public void addIncoming(Edge edge) {
    if (edge instanceof TemporaryEdge) {
      if (endVertex) {
        super.addIncoming(edge);
      } else {
        throw new UnsupportedOperationException("Can't add incoming edge to start vertex");
      }
    } else {
      throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
    }
  }

  @Override
  public boolean isEndVertex() {
    return endVertex;
  }
}
