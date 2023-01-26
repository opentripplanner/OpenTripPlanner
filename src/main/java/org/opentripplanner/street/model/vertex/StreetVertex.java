package org.opentripplanner.street.model.vertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * Abstract base class for vertices in the street layer of the graph. This includes both vertices
 * representing intersections or points (IntersectionVertices) and Elevator*Vertices.
 */
public abstract class StreetVertex extends Vertex {

  /** All locations for flex transit, which this vertex is part of */
  public Set<AreaStop> areaStops;

  public StreetVertex(Graph g, String label, Coordinate coord, I18NString streetName) {
    this(g, label, coord.x, coord.y, streetName);
  }

  public StreetVertex(Graph g, String label, double x, double y, I18NString streetName) {
    super(g, label, x, y, streetName);
  }

  /**
   * Creates intersection name out of all outgoing names
   * <p>
   * This can be: - name of the street if it is only 1 - unnamedStreed (localized in requested
   * language) if it doesn't have a name - corner of 0 and 1 (localized corner of zero and first
   * street in the corner)
   *
   * @return already localized street names and non-localized corner of x and unnamedStreet
   */
  public I18NString getIntersectionName() {
    // generate names for corners when no name was given
    Set<I18NString> uniqueNameSet = new HashSet<>();
    for (Edge e : getOutgoing()) {
      if (e instanceof StreetEdge) {
        uniqueNameSet.add(e.getName());
      }
    }
    List<I18NString> uniqueNames = new ArrayList<>(uniqueNameSet);

    if (uniqueNames.size() > 1) {
      return new LocalizedString("corner", uniqueNames.get(0), uniqueNames.get(1));
    } else if (uniqueNames.size() == 1) {
      return uniqueNames.get(0);
    } else {
      return new LocalizedString("unnamedStreet");
    }
  }

  public boolean isConnectedToWalkingEdge() {
    return this.getOutgoing()
      .stream()
      .anyMatch(edge ->
        edge instanceof StreetEdge && ((StreetEdge) edge).getPermission().allows(TraverseMode.WALK)
      );
  }

  public boolean isConnectedToDriveableEdge() {
    return this.getOutgoing()
      .stream()
      .anyMatch(edge ->
        edge instanceof StreetEdge && ((StreetEdge) edge).getPermission().allows(TraverseMode.CAR)
      );
  }

  public boolean isEligibleForCarPickupDropoff() {
    return isConnectedToDriveableEdge() && isConnectedToWalkingEdge();
  }
}
