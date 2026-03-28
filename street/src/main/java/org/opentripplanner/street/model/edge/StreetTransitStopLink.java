package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This represents the connection between a street vertex and a transit vertex where going from the
 * street to the vehicle is immediate -- such as at a curbside bus stop.
 */
public class StreetTransitStopLink extends StreetTransitEntityLink<TransitStopVertex> {

  private StreetTransitStopLink(StreetVertex fromv, TransitStopVertex tov) {
    super(fromv, tov, tov.getWheelchairAccessibility());
  }

  private StreetTransitStopLink(TransitStopVertex fromv, StreetVertex tov) {
    super(fromv, tov, fromv.getWheelchairAccessibility());
  }

  public static StreetTransitStopLink createStreetTransitStopLink(
    StreetVertex fromv,
    TransitStopVertex tov
  ) {
    return connectToGraph(new StreetTransitStopLink(fromv, tov));
  }

  public static StreetTransitStopLink createStreetTransitStopLink(
    TransitStopVertex fromv,
    StreetVertex tov
  ) {
    return connectToGraph(new StreetTransitStopLink(fromv, tov));
  }

  /**
   * Either from or to needs to be a {@link TransitStopVertex} and the other vertex should be a
   * {@link StreetVertex}.
   */
  public static StreetTransitStopLink createStreetTransitStopLink(Vertex from, Vertex to) {
    if (from instanceof TransitStopVertex stop && to instanceof StreetVertex street) {
      return connectToGraph(new StreetTransitStopLink(stop, street));
    }
    if (to instanceof TransitStopVertex stop && from instanceof StreetVertex street) {
      return connectToGraph(new StreetTransitStopLink(street, stop));
    }
    throw new IllegalArgumentException(
      "Vertices need to be a transit stop vertex and a street vertex. Got: " +
        from.getClass() +
        " and " +
        to.getClass()
    );
  }

  protected int getStreetToStopTime() {
    return getTransitEntityVertex().hasPathways()
      ? 0
      : getTransitEntityVertex().getStreetToStopTime();
  }
}
