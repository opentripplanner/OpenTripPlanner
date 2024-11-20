package org.opentripplanner.street.model.vertex;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;

public final class TemporaryStreetLocation extends StreetLocation implements TemporaryVertex {

  private final LinkingDirection linkingDirection;

  private TemporaryStreetLocation(
    String id,
    Coordinate nearestPoint,
    I18NString name,
    LinkingDirection linkingDirection
  ) {
    super(id, nearestPoint, name);
    this.linkingDirection = linkingDirection;
  }

  public static TemporaryStreetLocation originOrDestination(
    String id,
    Coordinate nearestPoint,
    I18NString name,
    boolean destination
  ) {
    return destination ? destination(id, nearestPoint, name) : origin(id, nearestPoint, name);
  }

  /**
   * Create a temporary vertex witch can be used as the starting point - the origin of a search.
   * The origin only has outgoing edges, an attempt to add incoming edges will fail.
   */
  public static TemporaryStreetLocation origin(
    String id,
    Coordinate nearestPoint,
    I18NString name
  ) {
    return new TemporaryStreetLocation(id, nearestPoint, name, LinkingDirection.OUTGOING);
  }

  /**
   * Create a temporary vertex witch can be used as the end point - the destination of a search.
   * The destination only has incoming edges, an attempt to add outgoing edges will fail.
   */
  public static TemporaryStreetLocation destination(
    String id,
    Coordinate nearestPoint,
    I18NString name
  ) {
    return new TemporaryStreetLocation(id, nearestPoint, name, LinkingDirection.INCOMING);
  }

  /**
   * Create a temporary vertex witch can be used as either the origin or the destination
   * in a nearby-search. This is used to route via a coordinate.
   */
  public static TemporaryStreetLocation via(String id, Coordinate nearestPoint, I18NString name) {
    return new TemporaryStreetLocation(id, nearestPoint, name, LinkingDirection.BOTH_WAYS);
  }

  @Override
  public void addOutgoing(Edge edge) {
    assertConnectToTemporaryEdge(edge);
    if (linkingDirection.allowOutgoing()) {
      addRentalRestriction(edge.getToVertex().rentalRestrictions());
      super.addOutgoing(edge);
    } else {
      throw new UnsupportedOperationException("Can't add outgoing edge to end vertex");
    }
  }

  @Override
  public void addIncoming(Edge edge) {
    assertConnectToTemporaryEdge(edge);
    if (linkingDirection.allowIncoming()) {
      super.addIncoming(edge);
      addRentalRestriction(edge.getFromVertex().rentalRestrictions());
    } else {
      throw new UnsupportedOperationException("Can't add incoming edge to start vertex");
    }
  }

  private static void assertConnectToTemporaryEdge(Edge edge) {
    if (!(edge instanceof TemporaryEdge)) {
      throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
    }
  }
}
