package org.opentripplanner.street.model.vertex;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;

/**
 * A temporary vertex witch can be used as the origin(outgoing edges),  destination(incomming
 * edges) or via point(in-/out-going edges). There is no constraint on adding incomming/outgoing
 * edges. For a temporary request scoped vertex with both incomming and outcomming edges, there
 * need to be a something that exclude it from other parallell searches. One way to do this is to
 * add a small cost to the temporary edges so the alternative permanent edges have a small
 * advatage.
 */
public final class TemporaryStreetLocation extends StreetLocation implements TemporaryVertex {

  public TemporaryStreetLocation(String id, Coordinate nearestPoint, I18NString name) {
    super(id, nearestPoint, name);
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
