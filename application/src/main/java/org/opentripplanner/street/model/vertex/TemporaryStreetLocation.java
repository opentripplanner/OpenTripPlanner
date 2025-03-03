package org.opentripplanner.street.model.vertex;

import java.util.concurrent.atomic.AtomicLong;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;

/**
 * A temporary vertex which can be used as the origin(outgoing edges),  destination(incoming
 * edges) or via point(in-/out-going edges). There is no constraint on adding incoming/outgoing
 * edges. For a temporary request scoped vertex with both incoming and outgoing edges, there
 * needs to be something that excludes it from other parallel searches. One way to do this is to
 * add a small cost to the temporary edges so that the alternative permanent edges have a small
 * advantage.
 */
public final class TemporaryStreetLocation extends StreetLocation implements TemporaryVertex {

  private static final AtomicLong idCounter = new AtomicLong(0);

  public TemporaryStreetLocation(Coordinate nearestPoint, I18NString name) {
    super("TempVertex-" + idCounter.incrementAndGet(), nearestPoint, name);
  }

  @Override
  public void addOutgoing(Edge edge) {
    assertConnectToTemporaryEdge(edge);
    addRentalRestriction(edge.getToVertex().rentalRestrictions());
    super.addOutgoing(edge);
  }

  @Override
  public void addIncoming(Edge edge) {
    assertConnectToTemporaryEdge(edge);
    super.addIncoming(edge);
    addRentalRestriction(edge.getFromVertex().rentalRestrictions());
  }

  private static void assertConnectToTemporaryEdge(Edge edge) {
    if (!(edge instanceof TemporaryEdge)) {
      throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
    }
  }
}
